package com.socoolheeya.bluebank.deposit.data.domain.result

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.entity.DepositTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

data class DepositTransactionResult(
    val id: Long?,
    val depositId: Long,
    val customerId: Long,
    val transactionType: DepositTransactionType,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val description: String?,
    val isAutoTransfer: Boolean,
    val weekNumber: Int?,
    val dayNumber: Int?,
    val transactionDate: LocalDateTime
) {
    companion object {
        fun from(transaction: DepositTransaction): DepositTransactionResult {
            return DepositTransactionResult(
                id = transaction.id,
                depositId = transaction.depositId,
                customerId = transaction.customerId,
                transactionType = transaction.transactionType,
                amount = transaction.amount,
                balanceAfter = transaction.balanceAfter,
                description = transaction.description,
                isAutoTransfer = transaction.isAutoTransfer,
                weekNumber = transaction.weekNumber,
                dayNumber = transaction.dayNumber,
                transactionDate = transaction.transactionDate
            )
        }
    }
}
