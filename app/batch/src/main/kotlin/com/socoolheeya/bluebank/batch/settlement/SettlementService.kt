package com.socoolheeya.bluebank.batch.settlement

import com.socoolheeya.bluebank.batch.config.EodProperties
import com.socoolheeya.bluebank.batch.eod.EodAccountingEntry
import com.socoolheeya.bluebank.batch.eod.EodAccountingEntryRepository
import com.socoolheeya.bluebank.batch.eod.ExternalSettlement
import com.socoolheeya.bluebank.batch.eod.ExternalSettlementRepository
import com.socoolheeya.bluebank.batch.eod.SettlementStatus
import com.socoolheeya.bluebank.batch.interest.DailyAmountCalculator
import com.socoolheeya.bluebank.batch.support.IdempotencyKeys
import com.socoolheeya.bluebank.card.data.domain.CardEnums
import com.socoolheeya.bluebank.card.data.repository.CardTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SettlementService(
    private val transactionRepository: CardTransactionRepository,
    private val accountingRepository: EodAccountingEntryRepository,
    private val settlementRepository: ExternalSettlementRepository,
    private val calculator: DailyAmountCalculator,
    private val properties: EodProperties
) {
    @Transactional
    fun settleCardTransaction(transactionId: Long, businessDate: LocalDate): Boolean {
        val transaction = transactionRepository.findById(transactionId).orElse(null) ?: return false
        if (transaction.status != CardEnums.TransactionStatus.APPROVED || transaction.transactionDate.toLocalDate() > businessDate) return false
        val institution = if (transaction.isOverseas) "INTERNATIONAL_CARD" else "DOMESTIC_CARD"
        val rate = if (transaction.isOverseas) properties.overseasCardSettlementRate else properties.domesticCardSettlementRate
        val key = IdempotencyKeys.accounting(businessDate, "CARD_TRANSACTION", transactionId, "SETTLEMENT")
        if (accountingRepository.existsByIdempotencyKey(key)) return false
        val fee = calculator.fee(transaction.amount, rate)
        val net = transaction.amount - fee
        transaction.settle(businessDate)
        transactionRepository.save(transaction)
        accountingRepository.save(
            EodAccountingEntry(businessDate = businessDate, referenceType = "CARD_TRANSACTION", referenceId = transactionId,
                entryType = "SETTLEMENT", baseAmount = transaction.amount, rate = rate, amount = net, idempotencyKey = key)
        )
        val settlementKey = IdempotencyKeys.settlement(businessDate, institution, transaction.transactionId)
        if (!settlementRepository.existsByIdempotencyKey(settlementKey)) {
            settlementRepository.save(
                ExternalSettlement(businessDate = businessDate, institution = institution, settlementType = "CARD",
                    grossAmount = transaction.amount, feeAmount = fee, netAmount = net,
                    status = SettlementStatus.COMPLETED, idempotencyKey = settlementKey)
            )
        }
        return true
    }

    fun aggregateExternalSettlements(businessDate: LocalDate): Long =
        settlementRepository.findAll().count { it.businessDate == businessDate }.toLong()
}
