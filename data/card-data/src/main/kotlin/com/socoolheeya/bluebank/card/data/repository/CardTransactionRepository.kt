package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction
import com.socoolheeya.bluebank.card.data.domain.CardEnums.TransactionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface CardTransactionRepository : JpaRepository<CardTransaction, Long> {
    fun findByStatusAndTransactionDateBefore(status: TransactionStatus, transactionDate: LocalDateTime): List<CardTransaction>
    fun findByCardId(cardId: Long): List<CardTransaction>
    fun findByCardIdAndTransactionDateBetween(
        cardId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CardTransaction>
    fun findByCustomerId(customerId: Long): List<CardTransaction>
    fun findByTransactionId(transactionId: String): Optional<CardTransaction>
    fun findByStatus(status: TransactionStatus): List<CardTransaction>
}
