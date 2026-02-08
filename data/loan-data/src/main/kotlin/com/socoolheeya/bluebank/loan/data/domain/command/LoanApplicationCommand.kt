package com.socoolheeya.bluebank.loan.data.domain.command

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.LoanApplication
import java.math.BigDecimal

sealed interface LoanApplicationCommand {

    data class Submit(
        val customerId: Long,
        val requestedAmount: BigDecimal,
        val requestedTerm: Int,
        val productType: LoanEnums.ProductType,
        val repaymentMethod: LoanEnums.RepaymentMethod,
        val annualIncome: BigDecimal,
        val employmentType: String,
        val employmentPeriodMonths: Int,
        val companyName: String? = null,
        val creditScore: Int,
        val existingLoanCount: Int = 0,
        val totalExistingDebt: BigDecimal = BigDecimal.ZERO,
        val hasDelayHistory: Boolean = false,
        val collateralType: String? = null,
        val collateralValue: BigDecimal? = null,
        val collateralAddress: String? = null
    ) : LoanApplicationCommand {
        fun toEntity(): LoanApplication {
            return LoanApplication(
                customerId = customerId,
                requestedAmount = requestedAmount,
                requestedTerm = requestedTerm,
                productType = productType,
                repaymentMethod = repaymentMethod,
                annualIncome = annualIncome,
                employmentType = employmentType,
                employmentPeriodMonths = employmentPeriodMonths,
                companyName = companyName,
                creditScore = creditScore,
                existingLoanCount = existingLoanCount,
                totalExistingDebt = totalExistingDebt,
                hasDelayHistory = hasDelayHistory,
                collateralType = collateralType,
                collateralValue = collateralValue,
                collateralAddress = collateralAddress
            )
        }
    }
}