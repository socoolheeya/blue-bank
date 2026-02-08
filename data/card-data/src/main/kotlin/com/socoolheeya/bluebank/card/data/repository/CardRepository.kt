package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.Card
import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CardRepository : JpaRepository<Card, Long> {
    fun findByCardNumber(cardNumber: String): Optional<Card>
    fun findByCustomerId(customerId: Long): List<Card>
    fun findByCustomerIdAndStatus(customerId: Long, status: CardStatus): List<Card>
    fun findByStatus(status: CardStatus): List<Card>
    fun findByAccountId(accountId: Long): List<Card>
    fun existsByCardNumber(cardNumber: String): Boolean
}