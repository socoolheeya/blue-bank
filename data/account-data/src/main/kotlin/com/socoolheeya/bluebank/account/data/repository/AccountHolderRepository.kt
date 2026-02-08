package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.AccountHolder
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountHolderRepository : JpaRepository<AccountHolder, Long> {
    fun findByAccountId(accountId: Long): List<AccountHolder>
    fun findByCustomerId(customerId: Long): List<AccountHolder>
    fun findByAccountIdAndCustomerId(accountId: Long, customerId: Long): AccountHolder?
    fun findByAccountIdAndRole(accountId: Long, role: AccountEnums.HolderRole): List<AccountHolder>
}
