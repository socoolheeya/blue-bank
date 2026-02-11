package com.socoolheeya.bluebank.deposit.data.repository

import com.socoolheeya.bluebank.deposit.data.domain.entity.InterestPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface InterestPaymentRepository : JpaRepository<InterestPayment, Long> {

    fun findByDepositId(depositId: Long): List<InterestPayment>

    fun findByDepositIdOrderByPaymentDateDesc(depositId: Long): List<InterestPayment>

    fun findByCustomerId(customerId: Long): List<InterestPayment>

    fun findByPaymentDate(paymentDate: LocalDate): List<InterestPayment>

    fun findByDepositIdAndPaymentDateBetween(
        depositId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<InterestPayment>
}
