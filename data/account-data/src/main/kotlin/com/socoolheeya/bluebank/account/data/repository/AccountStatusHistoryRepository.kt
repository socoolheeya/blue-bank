package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.AccountStatusHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountStatusHistoryRepository : JpaRepository<AccountStatusHistory, Long> {
    fun findByAccountIdOrderByChangedAtDesc(accountId: Long): List<AccountStatusHistory>
    fun findTopByAccountIdOrderByChangedAtDesc(accountId: Long): AccountStatusHistory?
}
