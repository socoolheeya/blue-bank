package com.socoolheeya.bluebank.loan.data.domain.result

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.LoanApplication
import java.math.BigDecimal
import java.time.LocalDateTime

data class LoanApplicationResult(
    val id: Long?,
    val customerId: Long,
    val requestedAmount: BigDecimal,
    val requestedTerm: Int,
    val productType: LoanEnums.ProductType,
    val repaymentMethod: LoanEnums.RepaymentMethod,
    val annualIncome: BigDecimal,
    val creditScore: Int,
    val status: LoanEnums.ApplicationStatus,
    val approvedAmount: BigDecimal?,
    val approvedRate: BigDecimal?,
    val rejectionReason: String?,
    val loanId: Long?,
    val appliedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?
) {
    companion object {
        fun from(application: LoanApplication): LoanApplicationResult {
            return LoanApplicationResult(
                id = application.id,
                customerId = application.customerId,
                requestedAmount = application.requestedAmount,
                requestedTerm = application.requestedTerm,
                productType = application.productType,
                repaymentMethod = application.repaymentMethod,
                annualIncome = application.annualIncome,
                creditScore = application.creditScore,
                status = application.status,
                approvedAmount = application.approvedAmount,
                approvedRate = application.approvedRate,
                rejectionReason = application.rejectionReason,
                loanId = application.loanId,
                appliedAt = application.appliedAt,
                reviewedAt = application.reviewedAt
            )
        }
    }
}