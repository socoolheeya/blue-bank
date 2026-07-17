package com.socoolheeya.bluebank.batch.config

import com.socoolheeya.bluebank.batch.eod.EodExecution
import com.socoolheeya.bluebank.batch.eod.EodExecutionRepository
import com.socoolheeya.bluebank.batch.eod.EodStatus
import com.socoolheeya.bluebank.batch.eod.LedgerCloseService
import com.socoolheeya.bluebank.batch.eod.ScheduledTransferRepository
import com.socoolheeya.bluebank.batch.eod.TransferStatus
import com.socoolheeya.bluebank.batch.interest.InterestAndFeeService
import com.socoolheeya.bluebank.batch.settlement.SettlementService
import com.socoolheeya.bluebank.batch.support.BusinessDate
import com.socoolheeya.bluebank.batch.transfer.ScheduledTransferService
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.card.data.domain.CardEnums
import com.socoolheeya.bluebank.card.data.repository.CardTransactionRepository
import com.socoolheeya.bluebank.deposit.data.repository.DepositRepository
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.listener.JobExecutionListener
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime

data class InterestTarget(val type: String, val id: Long)

@Configuration
class DailyEodJobConfiguration(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val properties: EodProperties,
    private val eodExecutionRepository: EodExecutionRepository,
    private val balanceRepository: BalanceRepository,
    private val transferRepository: ScheduledTransferRepository,
    private val cardTransactionRepository: CardTransactionRepository,
    private val depositRepository: DepositRepository,
    private val loanRepository: LoanRepository,
    private val ledgerCloseService: LedgerCloseService,
    private val transferService: ScheduledTransferService,
    private val interestService: InterestAndFeeService,
    private val settlementService: SettlementService
) {
    @Bean
    fun dailyEodJob(
        openEodExecutionStep: Step,
        closeLedgerStep: Step,
        executeScheduledTransferStep: Step,
        calculateInterestAndFeeStep: Step,
        settleCardTransactionStep: Step,
        settleExternalTransactionStep: Step,
        closeEodExecutionStep: Step
    ): Job = JobBuilder("dailyEodJob", jobRepository)
        .validator { parameters -> BusinessDate.parse(parameters.getString("businessDate")) }
        .listener(eodJobExecutionListener())
        .start(openEodExecutionStep)
        .next(closeLedgerStep)
        .next(executeScheduledTransferStep)
        .next(calculateInterestAndFeeStep)
        .next(settleCardTransactionStep)
        .next(settleExternalTransactionStep)
        .next(closeEodExecutionStep)
        .build()

    @Bean
    fun eodJobExecutionListener(): JobExecutionListener = object : JobExecutionListener {
        override fun afterJob(jobExecution: org.springframework.batch.core.job.JobExecution) {
            val date = BusinessDate.parse(jobExecution.jobParameters.getString("businessDate"))
            val execution = eodExecutionRepository.findByBusinessDate(date) ?: return
            execution.successCount = jobExecution.stepExecutions.sumOf { it.writeCount }
            execution.failureCount = jobExecution.stepExecutions.sumOf { it.skipCount }
            if (jobExecution.status != BatchStatus.COMPLETED) {
                execution.status = EodStatus.FAILED
                execution.completedAt = LocalDateTime.now()
                execution.errorSummary = jobExecution.allFailureExceptions
                    .joinToString(" | ") { it.message ?: it.javaClass.simpleName }
                    .take(2000)
            }
            eodExecutionRepository.save(execution)
        }
    }

    @Bean
    fun openEodExecutionStep(): Step = taskletStep("openEodExecutionStep") { date ->
        val execution = eodExecutionRepository.findByBusinessDate(date)
        if (execution == null) {
            eodExecutionRepository.save(EodExecution(businessDate = date))
        } else if (execution.status != EodStatus.COMPLETED) {
            execution.status = EodStatus.STARTED
            execution.startedAt = LocalDateTime.now()
            execution.errorSummary = null
            eodExecutionRepository.save(execution)
        }
    }

    @Bean
    fun closeLedgerStep(): Step = chunkStep<Long>(
        "closeLedgerStep",
        { _ -> balanceRepository.findAll().map { it.accountId }.iterator() },
        { accountId: Long, date: LocalDate -> ledgerCloseService.close(accountId, date) }
    )

    @Bean
    fun executeScheduledTransferStep(): Step = chunkStep<Long>(
        "executeScheduledTransferStep",
        { date -> transferRepository.findByStatusAndNextExecutionDateLessThanEqual(TransferStatus.PENDING, date).mapNotNull { it.id }.iterator() },
        { transferId: Long, date: LocalDate -> transferService.execute(transferId, date) }
    )

    @Bean
    fun calculateInterestAndFeeStep(): Step = chunkStep<InterestTarget>(
        "calculateInterestAndFeeStep",
        { _ ->
            buildList {
                addAll(balanceRepository.findAll().map { InterestTarget("ACCOUNT", it.accountId) })
                addAll(depositRepository.findAll().mapNotNull { it.id?.let { id -> InterestTarget("DEPOSIT", id) } })
                addAll(loanRepository.findAll().mapNotNull { it.id?.let { id -> InterestTarget("LOAN", id) } })
            }.iterator()
        },
        { target: InterestTarget, date: LocalDate ->
            when (target.type) {
                "ACCOUNT" -> interestService.processAccount(target.id, date)
                "DEPOSIT" -> interestService.processDeposit(target.id, date)
                "LOAN" -> interestService.processLoan(target.id, date)
            }
        }
    )

    @Bean
    fun settleCardTransactionStep(): Step = chunkStep<Long>(
        "settleCardTransactionStep",
        { date -> cardTransactionRepository.findByStatusAndTransactionDateBefore(
            CardEnums.TransactionStatus.APPROVED, date.plusDays(1).atStartOfDay()
        ).mapNotNull { it.id }.iterator() },
        { transactionId: Long, date: LocalDate -> settlementService.settleCardTransaction(transactionId, date) }
    )

    @Bean
    fun settleExternalTransactionStep(): Step = taskletStep("settleExternalTransactionStep") { date ->
        settlementService.aggregateExternalSettlements(date)
    }

    @Bean
    fun closeEodExecutionStep(): Step = taskletStep("closeEodExecutionStep") { date ->
        val execution = eodExecutionRepository.findByBusinessDate(date)
            ?: error("EOD execution missing for $date")
        execution.status = EodStatus.COMPLETED
        execution.completedAt = LocalDateTime.now()
        eodExecutionRepository.save(execution)
    }

    private fun taskletStep(name: String, action: (LocalDate) -> Unit): Step =
        StepBuilder(name, jobRepository).tasklet({ _, context ->
            action(BusinessDate.parse(context.stepContext.jobParameters["businessDate"]?.toString()))
            RepeatStatus.FINISHED
        }, transactionManager).build()

    private fun <T : Any> chunkStep(
        name: String,
        items: (LocalDate) -> Iterator<T>,
        action: (T, LocalDate) -> Unit
    ): Step {
        var iterator: Iterator<T>? = null
        var businessDate: LocalDate? = null
        var stepExecutionId: Long? = null
        val reader = ItemReader<T> {
            val context = org.springframework.batch.core.scope.context.StepSynchronizationManager.getContext()
                ?: error("No active Spring Batch step context")
            val currentStepExecutionId = context.stepExecution.id
            if (stepExecutionId != currentStepExecutionId) {
                stepExecutionId = currentStepExecutionId
                businessDate = BusinessDate.parse(context.stepExecution.jobParameters.getString("businessDate"))
                iterator = items(businessDate!!)
            }
            val date = businessDate ?: currentBusinessDate().also { businessDate = it }
            val current = iterator ?: items(date).also { iterator = it }
            if (current.hasNext()) current.next() else null
        }
        val writer = ItemWriter<T> { chunk ->
            val date = businessDate ?: currentBusinessDate()
            chunk.items.forEach { action(it, date) }
        }
        return StepBuilder(name, jobRepository)
            .chunk<T, T>(properties.chunkSize)
            .reader(reader)
            .writer(writer)
            .transactionManager(transactionManager)
            .build()
    }

    private fun currentBusinessDate(): LocalDate {
        val execution = org.springframework.batch.core.scope.context.StepSynchronizationManager.getContext()
            ?: error("No active Spring Batch step context")
        return BusinessDate.parse(execution.stepExecution.jobParameters.getString("businessDate"))
    }
}
