package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.AccountSection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountSectionRepository : JpaRepository<AccountSection, Long> {
    fun findByAccountIdOrderByDisplayOrderAsc(accountId: Long): List<AccountSection>
    fun findByAccountId(accountId: Long): List<AccountSection>
}
