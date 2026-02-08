package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.AccountLimit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountLimitRepository : JpaRepository<AccountLimit, Long> {
    fun findByAccountId(accountId: Long): AccountLimit?
}
