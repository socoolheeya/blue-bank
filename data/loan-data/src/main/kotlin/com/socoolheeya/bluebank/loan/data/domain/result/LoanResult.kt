package com.socoolheeya.bluebank.loan.data.domain.result

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class LoanResult(
    val id: Long?,
    val loanNumber: String,
    val customerId: Long,
    val accountId: Long,
    val loanType: LoanEnums.LoanType,
    val productType: LoanEnums.ProductType,
    val principalAmount: BigDecimal,
    val outstandingBalance: BigDecimal,
    val interestRate: BigDecimal,
    val rateType: LoanEnums.RateType,
    val loanTerm: Int,
    val startDate: LocalDate,
    val maturityDate: LocalDate,
    val repaymentMethod: LoanEnums.RepaymentMethod,
    val monthlyPayment: BigDecimal?,
    val status: LoanEnums.LoanStatus,
    val collateralId: Long?,
    val loanToValueRatio: BigDecimal?,
    val creditScore: Int?,
    val preferentialRate: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(loan: Loan): LoanResult {
            return LoanResult(
                id = loan.id,
                loanNumber = loan.loanNumber,
                customerId = loan.customerId,
                accountId = loan.accountId,
                loanType = loan.loanType,
                productType = loan.productType,
                principalAmount = loan.principalAmount,
                outstandingBalance = loan.outstandingBalance,
                interestRate = loan.interestRate,
                rateType = loan.rateType,
                loanTerm = loan.loanTerm,
                startDate = loan.startDate,
                maturityDate = loan.maturityDate,
                repaymentMethod = loan.repaymentMethod,
                monthlyPayment = loan.monthlyPayment,
                status = loan.status,
                collateralId = loan.collateralId,
                loanToValueRatio = loan.loanToValueRatio,
                creditScore = loan.creditScore,
                preferentialRate = loan.preferentialRate,
                createdAt = loan.createdAt,
                updatedAt = loan.updatedAt
            )
        }
    }
}