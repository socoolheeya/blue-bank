package com.socoolheeya.bluebank.card.data.domain.result

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.CashbackHistory
import java.math.BigDecimal
import java.time.LocalDate

data class CashbackResult(
    val id: Long?,
    val cardId: Long,
    val customerId: Long,
    val transactionId: Long,
    val cashbackAmount: BigDecimal,
    val cashbackRate: BigDecimal,
    val transactionAmount: BigDecimal,
    val cashbackType: CashbackType,
    val earnedDate: LocalDate,
    val paymentDate: LocalDate?,
    val actualPaymentDate: LocalDate?,
    val status: CashbackStatus
) {
    companion object {
        fun from(cashback: CashbackHistory): CashbackResult {
            return CashbackResult(
                id = cashback.id,
                cardId = cashback.cardId,
                customerId = cashback.customerId,
                transactionId = cashback.transactionId,
                cashbackAmount = cashback.cashbackAmount,
                cashbackRate = cashback.cashbackRate,
                transactionAmount = cashback.transactionAmount,
                cashbackType = cashback.cashbackType,
                earnedDate = cashback.earnedDate,
                paymentDate = cashback.paymentDate,
                actualPaymentDate = cashback.actualPaymentDate,
                status = cashback.status
            )
        }
    }
}