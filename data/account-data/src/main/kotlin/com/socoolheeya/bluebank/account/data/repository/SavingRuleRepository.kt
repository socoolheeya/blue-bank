package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.SavingRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SavingRuleRepository : JpaRepository<SavingRule, Long> {
    fun findByAccountId(accountId: Long): List<SavingRule>
    fun findByAccountIdAndIsActive(accountId: Long, isActive: Boolean): List<SavingRule>
    fun findByIsActiveAndStartDateBeforeAndEndDateAfter(isActive: Boolean, startDate: LocalDateTime, endDate: LocalDateTime): List<SavingRule>
}
