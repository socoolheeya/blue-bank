package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.InterestPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface InterestPaymentRepository : JpaRepository<InterestPayment, Long> {
    fun findByAccountIdOrderByPaidAtDesc(accountId: Long): List<InterestPayment>
    fun findByAccountIdAndCalculationPeriodStartBetween(accountId: Long, start: LocalDate, end: LocalDate): List<InterestPayment>

    @Query("SELECT SUM(ip.amount) FROM InterestPayment ip WHERE ip.accountId = :accountId")
    fun getTotalInterestByAccountId(accountId: Long): BigDecimal?
}
