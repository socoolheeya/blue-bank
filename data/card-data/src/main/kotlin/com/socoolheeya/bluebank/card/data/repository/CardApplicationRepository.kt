package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.CardApplication
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CardApplicationRepository : JpaRepository<CardApplication, Long> {
    fun findByCustomerId(customerId: Long): List<CardApplication>
    fun findByCustomerIdAndStatus(customerId: Long, status: CardApplicationStatus): List<CardApplication>
    fun findByStatus(status: CardApplicationStatus): List<CardApplication>
    fun findByCardId(cardId: Long): Optional<CardApplication>
}