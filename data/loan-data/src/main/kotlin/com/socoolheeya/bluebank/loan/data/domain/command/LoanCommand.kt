package com.socoolheeya.bluebank.loan.data.domain.command

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import java.math.BigDecimal
import java.time.LocalDate

sealed interface LoanCommand {

    data class Create(
        val loanNumber: String,
        val customerId: Long,
        val accountId: Long,
        val loanType: LoanEnums.LoanType,
        val productType: LoanEnums.ProductType,
        val principalAmount: BigDecimal,
        val interestRate: BigDecimal,
        val rateType: LoanEnums.RateType,
        val loanTerm: Int,
        val repaymentMethod: LoanEnums.RepaymentMethod,
        val monthlyPayment: BigDecimal? = null,
        val collateralId: Long? = null,
        val loanToValueRatio: BigDecimal? = null,
        val creditScore: Int? = null,
        val preferentialRate: BigDecimal = BigDecimal.ZERO,
        val discountReason: String? = null
    ) : LoanCommand {
        fun toEntity(): Loan {
            return Loan(
                loanNumber = loanNumber,
                customerId = customerId,
                accountId = accountId,
                loanType = loanType,
                productType = productType,
                principalAmount = principalAmount,
                outstandingBalance = principalAmount,
                interestRate = interestRate,
                rateType = rateType,
                loanTerm = loanTerm,
                startDate = LocalDate.now(),
                maturityDate = LocalDate.now().plusMonths(loanTerm.toLong()),
                repaymentMethod = repaymentMethod,
                monthlyPayment = monthlyPayment,
                status = LoanEnums.LoanStatus.PENDING,
                collateralId = collateralId,
                loanToValueRatio = loanToValueRatio,
                creditScore = creditScore,
                preferentialRate = preferentialRate,
                discountReason = discountReason
            )
        }
    }

    data class Approve(
        val loanId: Long,
        val approver: String
    ) : LoanCommand

    data class Execute(
        val loanId: Long,
        val accountId: Long
    ) : LoanCommand

    data class Repay(
        val loanId: Long,
        val amount: BigDecimal,
        val repaymentType: LoanEnums.RepaymentType
    ) : LoanCommand

    data class Reject(
        val loanId: Long,
        val reason: String
    ) : LoanCommand
}