package com.socoolheeya.bluebank.batch.transfer

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.LedgerEntry
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.account.data.repository.LedgerEntryRepository
import com.socoolheeya.bluebank.batch.eod.ScheduledTransferExecution
import com.socoolheeya.bluebank.batch.eod.ScheduledTransferExecutionRepository
import com.socoolheeya.bluebank.batch.eod.ScheduledTransferRepository
import com.socoolheeya.bluebank.batch.eod.TransferFrequency
import com.socoolheeya.bluebank.batch.eod.TransferStatus
import com.socoolheeya.bluebank.batch.support.IdempotencyKeys
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

enum class TransferOutcome { COMPLETED, FAILED, ALREADY_PROCESSED }

@Service
class ScheduledTransferService(
    private val transferRepository: ScheduledTransferRepository,
    private val executionRepository: ScheduledTransferExecutionRepository,
    private val balanceRepository: BalanceRepository,
    private val ledgerEntryRepository: LedgerEntryRepository
) {
    @Transactional
    fun execute(transferId: Long, businessDate: LocalDate): TransferOutcome {
        val key = IdempotencyKeys.transfer(businessDate, transferId)
        if (executionRepository.existsByIdempotencyKey(key)) return TransferOutcome.ALREADY_PROCESSED

        val transfer = transferRepository.findById(transferId).orElseThrow {
            IllegalArgumentException("Scheduled transfer not found: $transferId")
        }
        val source = balanceRepository.findByAccountId(transfer.sourceAccountId)
        val destination = balanceRepository.findByAccountId(transfer.destinationAccountId)
        val failure = when {
            transfer.status != TransferStatus.PENDING -> "Transfer is not pending"
            transfer.nextExecutionDate > businessDate -> "Transfer is not due"
            transfer.amount.signum() <= 0 -> "Transfer amount must be positive"
            source == null -> "Source balance not found"
            destination == null -> "Destination balance not found"
            source.availableBalance < transfer.amount -> "Insufficient balance"
            else -> null
        }

        if (failure != null) {
            transfer.retryCount++
            transfer.failureReason = failure
            executionRepository.save(
                ScheduledTransferExecution(
                    transferId = transferId,
                    businessDate = businessDate,
                    idempotencyKey = key,
                    status = TransferStatus.FAILED,
                    message = failure
                )
            )
            return TransferOutcome.FAILED
        }

        source!!.withdraw(transfer.amount)
        destination!!.deposit(transfer.amount)
        balanceRepository.save(source)
        balanceRepository.save(destination)

        val occurredAt = businessDate.atTime(1, 0)
        val transactionId = "EOD-TRANSFER-$businessDate-$transferId"
        ledgerEntryRepository.saveAll(
            listOf(
                LedgerEntry(
                    accountId = transfer.sourceAccountId,
                    type = AccountEnums.EntryType.TRANSFER,
                    amount = transfer.amount.negate(),
                    balanceAfter = source.ledgerBalance,
                    description = "Scheduled transfer debit",
                    transactionId = "$transactionId-DEBIT",
                    occurredAt = occurredAt
                ),
                LedgerEntry(
                    accountId = transfer.destinationAccountId,
                    type = AccountEnums.EntryType.TRANSFER,
                    amount = transfer.amount,
                    balanceAfter = destination.ledgerBalance,
                    description = "Scheduled transfer credit",
                    transactionId = "$transactionId-CREDIT",
                    occurredAt = occurredAt
                )
            )
        )

        transfer.lastExecutedDate = businessDate
        transfer.failureReason = null
        if (transfer.frequency == TransferFrequency.ONCE) {
            transfer.status = TransferStatus.COMPLETED
        } else {
            transfer.nextExecutionDate = nextDate(businessDate, transfer.frequency)
        }
        transferRepository.save(transfer)
        executionRepository.save(
            ScheduledTransferExecution(
                transferId = transferId,
                businessDate = businessDate,
                idempotencyKey = key,
                status = TransferStatus.COMPLETED,
                executedAt = LocalDateTime.now()
            )
        )
        return TransferOutcome.COMPLETED
    }

    private fun nextDate(date: LocalDate, frequency: TransferFrequency): LocalDate = when (frequency) {
        TransferFrequency.DAILY -> date.plusDays(1)
        TransferFrequency.WEEKLY -> date.plusWeeks(1)
        TransferFrequency.MONTHLY -> date.plusMonths(1)
        TransferFrequency.ONCE -> date
    }
}
