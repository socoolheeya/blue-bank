package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.LimitUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface LimitUsageRepository : JpaRepository<LimitUsage, Long> {
    fun findByAccountIdAndDate(accountId: Long, date: LocalDate): LimitUsage?
    fun findByAccountIdAndDateBetween(accountId: Long, startDate: LocalDate, endDate: LocalDate): List<LimitUsage>
}
