package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.InterestPayment
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface InterestPaymentRepository : JpaRepository<InterestPayment, Long> {
    fun findByLoanId(loanId: Long): List<InterestPayment>
    fun findByStatus(status: LoanEnums.InterestPaymentStatus): List<InterestPayment>
    fun findByDueDateBefore(date: LocalDate): List<InterestPayment>
}