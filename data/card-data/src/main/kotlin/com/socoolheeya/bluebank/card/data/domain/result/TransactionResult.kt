package com.socoolheeya.bluebank.card.data.domain.result

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TransactionResult(
    val id: Long?,
    val cardId: Long,
    val customerId: Long,
    val transactionId: String,
    val merchantName: String,
    val merchantCategory: String,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val transactionDate: LocalDateTime,
    val approvalDate: LocalDateTime?,
    val settlementDate: LocalDate?,
    val status: TransactionStatus,
    val merchantCountry: String,
    val merchantCity: String?,
    val isOverseas: Boolean,
    val installmentMonths: Int,
    val approvalNumber: String?,
    val isApproved: Boolean
) {
    companion object {
        fun from(transaction: CardTransaction): TransactionResult {
            return TransactionResult(
                id = transaction.id,
                cardId = transaction.cardId,
                customerId = transaction.customerId,
                transactionId = transaction.transactionId,
                merchantName = transaction.merchantName,
                merchantCategory = transaction.merchantCategory,
                transactionType = transaction.transactionType,
                amount = transaction.amount,
                currency = transaction.currency,
                transactionDate = transaction.transactionDate,
                approvalDate = transaction.approvalDate,
                settlementDate = transaction.settlementDate,
                status = transaction.status,
                merchantCountry = transaction.merchantCountry,
                merchantCity = transaction.merchantCity,
                isOverseas = transaction.isOverseas,
                installmentMonths = transaction.installmentMonths,
                approvalNumber = transaction.approvalNumber,
                isApproved = transaction.isApproved
            )
        }
    }
}
