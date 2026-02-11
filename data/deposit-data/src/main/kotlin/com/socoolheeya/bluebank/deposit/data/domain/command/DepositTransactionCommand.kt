package com.socoolheeya.bluebank.deposit.data.domain.command

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.entity.DepositTransaction
import java.math.BigDecimal

sealed interface DepositTransactionCommand {

    data class Create(
        val depositId: Long,
        val customerId: Long,
        val transactionType: DepositTransactionType,
        val amount: BigDecimal,
        val balanceAfter: BigDecimal,
        val description: String? = null,
        val isAutoTransfer: Boolean = false,
        val weekNumber: Int? = null,
        val dayNumber: Int? = null
    ) : DepositTransactionCommand {
        fun toEntity(): DepositTransaction {
            return DepositTransaction(
                depositId = depositId,
                customerId = customerId,
                transactionType = transactionType,
                amount = amount,
                balanceAfter = balanceAfter,
                description = description,
                isAutoTransfer = isAutoTransfer,
                weekNumber = weekNumber,
                dayNumber = dayNumber
            )
        }
    }
}
