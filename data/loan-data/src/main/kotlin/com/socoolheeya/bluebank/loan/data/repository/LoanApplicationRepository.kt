package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.LoanApplication
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LoanApplicationRepository : JpaRepository<LoanApplication, Long> {
    fun findByCustomerId(customerId: Long): List<LoanApplication>
    fun findByCustomerIdAndStatus(customerId: Long, status: LoanEnums.ApplicationStatus): List<LoanApplication>
    fun findByStatus(status: LoanEnums.ApplicationStatus): List<LoanApplication>
    fun findByLoanId(loanId: Long): Optional<LoanApplication>
}