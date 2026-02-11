package com.socoolheeya.bluebank.deposit.data.domain.result

import com.socoolheeya.bluebank.deposit.data.domain.entity.InterestPayment
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InterestPaymentResult(
    val id: Long?,
    val depositId: Long,
    val customerId: Long,
    val interestAmount: BigDecimal,
    val appliedRate: BigDecimal,
    val calculationPeriodStart: LocalDate,
    val calculationPeriodEnd: LocalDate,
    val principalBalance: BigDecimal,
    val taxAmount: BigDecimal,
    val netInterest: BigDecimal,
    val paymentDate: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(interestPayment: InterestPayment): InterestPaymentResult {
            return InterestPaymentResult(
                id = interestPayment.id,
                depositId = interestPayment.depositId,
                customerId = interestPayment.customerId,
                interestAmount = interestPayment.interestAmount,
                appliedRate = interestPayment.appliedRate,
                calculationPeriodStart = interestPayment.calculationPeriodStart,
                calculationPeriodEnd = interestPayment.calculationPeriodEnd,
                principalBalance = interestPayment.principalBalance,
                taxAmount = interestPayment.taxAmount,
                netInterest = interestPayment.netInterest,
                paymentDate = interestPayment.paymentDate,
                createdAt = interestPayment.createdAt
            )
        }
    }
}
