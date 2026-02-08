package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LoanRepository : JpaRepository<Loan, Long> {
    fun findByLoanNumber(loanNumber: String): Optional<Loan>
    fun findByCustomerId(customerId: Long): List<Loan>
    fun findByCustomerIdAndStatus(customerId: Long, status: LoanEnums.LoanStatus): List<Loan>
    fun findByStatus(status: LoanEnums.LoanStatus): List<Loan>
    fun existsByLoanNumber(loanNumber: String): Boolean
}
