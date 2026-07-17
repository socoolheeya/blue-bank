package com.socoolheeya.bluebank.batch.eod

import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.data.repository.AccountRepository
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.batch.BatchApplication
import com.socoolheeya.bluebank.card.data.domain.CardEnums
import com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction
import com.socoolheeya.bluebank.card.data.repository.CardTransactionRepository
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

val dailyEodJobIntegrationTests by testSuite("Daily EOD job integration") {
    test("executes transfer interest ledger close and card settlement idempotently") {
        val context = SpringApplicationBuilder(BatchApplication::class.java)
            .web(WebApplicationType.NONE)
            .properties("blue-bank.batch.eod.schedule-enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop")
            .run()
        try {
            val accountRepository = context.getBean(AccountRepository::class.java)
            val balanceRepository = context.getBean(BalanceRepository::class.java)
            val transferRepository = context.getBean(ScheduledTransferRepository::class.java)
            val transferExecutionRepository = context.getBean(ScheduledTransferExecutionRepository::class.java)
            val cardTransactionRepository = context.getBean(CardTransactionRepository::class.java)
            val accountingRepository = context.getBean(EodAccountingEntryRepository::class.java)
            val eodRepository = context.getBean(EodExecutionRepository::class.java)
            val ledgerCloseRepository = context.getBean(LedgerCloseRepository::class.java)
            val jobOperator = context.getBean(JobOperator::class.java)
            val job = context.getBean("dailyEodJob", Job::class.java)
            val businessDate = LocalDate.of(2026, 7, 16)

            val sourceAccount = accountRepository.save(
                Account(accountNumber = "EOD-SOURCE", accountType = AccountEnums.AccountType.CHECKING,
                    productType = AccountEnums.ProductType.BASIC_CHECKING, interestRate = BigDecimal("0.0365"))
            )
            val destinationAccount = accountRepository.save(
                Account(accountNumber = "EOD-DEST", accountType = AccountEnums.AccountType.CHECKING,
                    productType = AccountEnums.ProductType.BASIC_CHECKING)
            )
            balanceRepository.save(
                Balance(sourceAccount.id!!, BigDecimal("100000"), BigDecimal("100000"), BigDecimal.ZERO, updatedAt = LocalDateTime.now())
            )
            balanceRepository.save(
                Balance(destinationAccount.id!!, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, updatedAt = LocalDateTime.now())
            )
            val transfer = transferRepository.save(
                ScheduledTransfer(sourceAccountId = sourceAccount.id!!, destinationAccountId = destinationAccount.id!!,
                    amount = BigDecimal("1000"), nextExecutionDate = businessDate)
            )
            val cardTransaction = cardTransactionRepository.save(
                CardTransaction(
                    cardId = 10L,
                    customerId = 20L,
                    transactionId = "CARD-EOD-1",
                    merchantName = "merchant",
                    merchantCategory = "5411",
                    transactionType = CardEnums.TransactionType.PURCHASE,
                    amount = BigDecimal("10000"),
                    transactionDate = businessDate.atTime(12, 0),
                    status = CardEnums.TransactionStatus.APPROVED,
                    isApproved = true
                )
            )

            val execution = jobOperator.start(
                job,
                JobParametersBuilder().addString("businessDate", businessDate.toString()).toJobParameters()
            )

            check(execution.status == BatchStatus.COMPLETED)
            val sourceBalance = balanceRepository.findByAccountId(sourceAccount.id!!)!!.ledgerBalance
            check(sourceBalance.compareTo(BigDecimal("99010")) == 0) { "Unexpected source balance: $sourceBalance" }
            check(balanceRepository.findByAccountId(destinationAccount.id!!)!!.ledgerBalance.compareTo(BigDecimal("1000")) == 0)
            check(transferExecutionRepository.existsByIdempotencyKey("$businessDate:TRANSFER:${transfer.id}"))
            check(cardTransactionRepository.findById(cardTransaction.id!!).orElseThrow().status == CardEnums.TransactionStatus.SETTLED)
            check(accountingRepository.findAll().isNotEmpty())
            check(ledgerCloseRepository.findAll().size == 2)
            check(eodRepository.findByBusinessDate(businessDate)?.status == EodStatus.COMPLETED)

            val accountingCount = accountingRepository.count()
            val secondExecution = jobOperator.start(
                job,
                JobParametersBuilder()
                    .addString("businessDate", businessDate.toString())
                    .addLong("rerun", 2L)
                    .toJobParameters()
            )

            check(secondExecution.status == BatchStatus.COMPLETED)
            check(balanceRepository.findByAccountId(sourceAccount.id!!)!!.ledgerBalance.compareTo(sourceBalance) == 0)
            check(transferExecutionRepository.count() == 1L)
            check(accountingRepository.count() == accountingCount)
            check(ledgerCloseRepository.count() == 2L)
        } finally {
            context.close()
        }
    }
}
