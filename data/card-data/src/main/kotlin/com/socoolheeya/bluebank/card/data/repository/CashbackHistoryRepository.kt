package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.CashbackHistory
import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CashbackHistoryRepository : JpaRepository<CashbackHistory, Long> {
    fun findByCardId(cardId: Long): List<CashbackHistory>
    fun findByCustomerId(customerId: Long): List<CashbackHistory>
    fun findByTransactionId(transactionId: Long): List<CashbackHistory>
    fun findByStatus(status: CashbackStatus): List<CashbackHistory>
    fun findByStatusAndPaymentDate(status: CashbackStatus, paymentDate: LocalDate): List<CashbackHistory>
    fun countByCardIdAndEarnedDate(cardId: Long, earnedDate: LocalDate): Int
}