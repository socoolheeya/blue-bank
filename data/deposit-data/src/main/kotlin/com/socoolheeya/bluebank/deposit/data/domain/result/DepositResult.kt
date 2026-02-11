package com.socoolheeya.bluebank.deposit.data.domain.result

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.entity.Deposit
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class DepositResult(
    // 기본 정보
    val id: Long?,
    val depositNumber: String,
    val customerId: Long,
    val accountId: Long,
    val productType: DepositProductType,
    val status: DepositStatus,

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
        fun from(deposit: Deposit): DepositResult {
            return DepositResult(
                id = deposit.id,
                depositNumber = deposit.depositNumber,
                customerId = deposit.customerId,
                accountId = deposit.accountId,
                productType = deposit.productType,
                status = deposit.status,
                currentBalance = deposit.currentBalance,
                accumulatedInterest = deposit.accumulatedInterest,
                appliedRate = deposit.appliedRate,
                startDate = deposit.startDate,
                maturityDate = deposit.maturityDate,
                createdAt = deposit.createdAt,
                updatedAt = deposit.updatedAt
            )
        }
    }
}
