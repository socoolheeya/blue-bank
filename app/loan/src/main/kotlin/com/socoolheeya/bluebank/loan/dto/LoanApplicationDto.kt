package com.socoolheeya.bluebank.loan.dto

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.result.LoanApplicationResult
import java.math.BigDecimal
import java.time.LocalDateTime

object LoanApplicationDto {

    data class Request(
        val customerId: Long,
        val accountId: Long,
        val productType: LoanEnums.ProductType,
        val amount: BigDecimal,
        val term: Int,
        val repaymentMethod: LoanEnums.RepaymentMethod,
        val annualIncome: BigDecimal,
        val employmentType: String,
        val employmentPeriodMonths: Int,
        val companyName: String? = null,
        val existingLoanCount: Int = 0,
        val totalExistingDebt: BigDecimal = BigDecimal.ZERO,
        val hasDelayHistory: Boolean = false
    )

    data class Response(
        val id: Long?,
        val customerId: Long,
        val requestedAmount: BigDecimal,
        val requestedTerm: Int,
        val productType: String,
        val status: String,
        val approvedAmount: BigDecimal?,
        val approvedRate: BigDecimal?,
        val rejectionReason: String?,
        val loanId: Long?,
        val appliedAt: LocalDateTime
    ) {
        companion object {
            fun from(result: LoanApplicationResult): Response {
                return Response(
                    id = result.id,
                    customerId = result.customerId,
                    requestedAmount = result.requestedAmount,
                    requestedTerm = result.requestedTerm,
                    productType = result.productType.description,
                    status = result.status.description,
                    approvedAmount = result.approvedAmount,
                    approvedRate = result.approvedRate,
                    rejectionReason = result.rejectionReason,
                    loanId = result.loanId,
                    appliedAt = result.appliedAt
                )
            }
        }
    }

    data class ApproveRequest(
        val approvedAmount: BigDecimal,
        val approvedRate: BigDecimal
    )

    data class RejectRequest(
        val reason: String
    )
}