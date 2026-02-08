package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.CardStatement
import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface CardStatementRepository : JpaRepository<CardStatement, Long> {
    fun findByCardId(cardId: Long): List<CardStatement>
    fun findByCardIdAndStatementYearAndStatementMonth(
        cardId: Long,
        year: Int,
        month: Int
    ): Optional<CardStatement>
    fun findByStatus(status: StatementStatus): List<CardStatement>
    fun findByPaymentDueDateBeforeAndStatus(dueDate: LocalDate, status: StatementStatus): List<CardStatement>
}