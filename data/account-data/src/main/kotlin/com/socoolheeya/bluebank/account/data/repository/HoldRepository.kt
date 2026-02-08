package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.Hold
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HoldRepository : JpaRepository<Hold, Long> {
    fun findByAccountId(accountId: Long): List<Hold>
    fun findByAccountIdAndStatus(accountId: Long, status: AccountEnums.HoldStatus): List<Hold>
    fun findByStatusAndExpiresAtBefore(status: AccountEnums.HoldStatus, expiresAt: LocalDateTime): List<Hold>
}
