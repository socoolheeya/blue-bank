package com.socoolheeya.bluebank.loan.dto

import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object LoanDto {

    data class Response(
        val id: Long?,
        val loanNumber: String,
        val customerId: Long,
        val accountId: Long,
        val loanType: String,
        val productType: String,
        val principalAmount: BigDecimal,
        val outstandingBalance: BigDecimal,
        val interestRate: BigDecimal,
        val rateType: String,
        val loanTerm: Int,
        val startDate: LocalDate,
        val maturityDate: LocalDate,
        val repaymentMethod: String,
        val monthlyPayment: BigDecimal?,
        val status: String,
        val creditScore: Int?,
        val createdAt: LocalDateTime
    ) {
        companion object {
            fun from(result: LoanResult): Response {
                return Response(
                    id = result.id,
                    loanNumber = result.loanNumber,
                    customerId = result.customerId,
                    accountId = result.accountId,
                    loanType = result.loanType.description,
                    productType = result.productType.description,
                    principalAmount = result.principalAmount,
                    outstandingBalance = result.outstandingBalance,
                    interestRate = result.interestRate,
                    rateType = result.rateType.description,
                    loanTerm = result.loanTerm,
                    startDate = result.startDate,
                    maturityDate = result.maturityDate,
                    repaymentMethod = result.repaymentMethod.description,
                    monthlyPayment = result.monthlyPayment,
                    status = result.status.description,
                    creditScore = result.creditScore,
                    createdAt = result.createdAt
                )
            }
        }
    }

    data class RepayRequest(
        val amount: BigDecimal
    )
}