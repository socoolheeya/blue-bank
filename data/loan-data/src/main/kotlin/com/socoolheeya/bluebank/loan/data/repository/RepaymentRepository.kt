package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.Repayment
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RepaymentRepository : JpaRepository<Repayment, Long> {
    fun findByLoanId(loanId: Long): List<Repayment>
    fun findByLoanIdAndStatus(loanId: Long, status: LoanEnums.RepaymentStatus): List<Repayment>
    fun findByStatus(status: LoanEnums.RepaymentStatus): List<Repayment>
    fun findByScheduledDateBetween(startDate: LocalDate, endDate: LocalDate): List<Repayment>
}