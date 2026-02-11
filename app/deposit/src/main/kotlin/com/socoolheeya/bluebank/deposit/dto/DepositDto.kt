package com.socoolheeya.bluebank.deposit.dto

import com.socoolheeya.bluebank.deposit.data.domain.result.DepositResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object DepositDto {

    data class CreateRequest(
        val customerId: Long,
        val accountId: Long,
        val productType: String,
        val principalAmount: BigDecimal,
        val baseRate: BigDecimal,
        val contractPeriod: Int,
        val periodUnit: String,
        val startDate: LocalDate,
        val maturityDate: LocalDate,
        val monthlyPayment: BigDecimal? = null,
        val minMonthlyPayment: BigDecimal? = null,
        val maxMonthlyPayment: BigDecimal? = null,
        val autoTransferEnabled: Boolean = false,
        val autoTransferDay: Int? = null,
        val autoTransferAmount: BigDecimal? = null,
        val autoRenewalEnabled: Boolean = false,
        val initialWeeklyAmount: BigDecimal? = null,
        val childId: Long? = null,
        val parentIds: String? = null,
        val maxBalance: BigDecimal? = null,
        val spareChangeEnabled: Boolean = false,
        val aiSavingsEnabled: Boolean = false,
        val interestPaymentDay: Int? = null,
        val isTaxFree: Boolean = false
    )

    data class DepositRequest(
        val amount: BigDecimal,
        val description: String? = null
    )

    data class WithdrawRequest(
        val amount: BigDecimal
    )

    data class Response(
        // 기본 정보
        val id: Long?,
        val depositNumber: String,
        val customerId: Long,
        val accountId: Long,
        val productType: String,
        val status: String,

        // 금액 정보
        val currentBalance: BigDecimal,
        val accumulatedInterest: BigDecimal,
        val appliedRate: BigDecimal,

        // 기간 정보
        val startDate: LocalDate,
        val maturityDate: LocalDate,

        // 감사
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        companion object {
            fun from(result: DepositResult): Response {
                return Response(
                    id = result.id,
                    depositNumber = result.depositNumber,
                    customerId = result.customerId,
                    accountId = result.accountId,
                    productType = result.productType.description,
                    status = result.status.description,
                    currentBalance = result.currentBalance,
                    accumulatedInterest = result.accumulatedInterest,
                    appliedRate = result.appliedRate,
                    startDate = result.startDate,
                    maturityDate = result.maturityDate,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt
                )
            }
        }
    }
}