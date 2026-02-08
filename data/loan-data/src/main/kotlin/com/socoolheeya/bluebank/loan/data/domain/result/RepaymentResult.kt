package com.socoolheeya.bluebank.loan.data.domain.result

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.Repayment
import java.math.BigDecimal
import java.time.LocalDate

data class RepaymentResult(
    val id: Long?,
    val loanId: Long,
    val repaymentType: LoanEnums.RepaymentType,
    val principalAmount: BigDecimal,
    val interestAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val balanceAfter: BigDecimal,
    val scheduledDate: LocalDate,
    val actualDate: LocalDate?,
    val status: LoanEnums.RepaymentStatus,
    val isOverdue: Boolean,
    val overdueDays: Int,
    val penaltyAmount: BigDecimal
) {
    companion object {
        fun from(repayment: Repayment): RepaymentResult {
            return RepaymentResult(
                id = repayment.id,
                loanId = repayment.loanId,
                repaymentType = repayment.repaymentType,
                principalAmount = repayment.principalAmount,
                interestAmount = repayment.interestAmount,
                totalAmount = repayment.totalAmount,
                balanceAfter = repayment.balanceAfter,
                scheduledDate = repayment.scheduledDate,
                actualDate = repayment.actualDate,
                status = repayment.status,
                isOverdue = repayment.isOverdue,
                overdueDays = repayment.overdueDays,
                penaltyAmount = repayment.penaltyAmount
            )
        }
    }
}