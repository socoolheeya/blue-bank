package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.CreditScoreHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CreditScoreHistoryRepository : JpaRepository<CreditScoreHistory, Long> {
    fun findByCustomerId(customerId: Long): List<CreditScoreHistory>
    fun findByCustomerIdOrderByScoredAtDesc(customerId: Long): List<CreditScoreHistory>
    fun findByLoanApplicationId(applicationId: Long): Optional<CreditScoreHistory>
}