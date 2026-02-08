package com.socoolheeya.bluebank.card.data.domain.command

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction
import java.math.BigDecimal
import java.time.LocalDateTime

sealed interface TransactionCommand {

    data class Create(
        val cardId: Long,
        val customerId: Long,
        val transactionId: String,
        val merchantName: String,
        val merchantCategory: String,
        val transactionType: TransactionType,
        val amount: BigDecimal,
        val currency: String = "KRW",
        val merchantCountry: String = "KR",
        val merchantCity: String? = null,
        val isOverseas: Boolean = false,
        val installmentMonths: Int = 0
    ) : TransactionCommand {
        fun toEntity(): CardTransaction {
            return CardTransaction(
                cardId = cardId,
                customerId = customerId,
                transactionId = transactionId,
                merchantName = merchantName,
                merchantCategory = merchantCategory,
                transactionType = transactionType,
                amount = amount,
                currency = currency,
                transactionDate = LocalDateTime.now(),
                status = TransactionStatus.PENDING,
                merchantCountry = merchantCountry,
                merchantCity = merchantCity,
                isOverseas = isOverseas,
                installmentMonths = installmentMonths
            )
        }
    }
}