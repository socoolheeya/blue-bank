package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: JpaRepository<Account, Long> {
    fun findByAccountNumber(accountNumber: String): Account?
    fun findByProductType(productType: AccountEnums.ProductType): List<Account>
    fun findByStatus(status: AccountEnums.AccountStatus): List<Account>
    fun findByParentAccountId(parentAccountId: Long): List<Account>
    fun findByLinkedAccountId(linkedAccountId: Long): Account?

    @Query("SELECT a FROM Account a JOIN AccountHolder ah ON a.id = ah.accountId WHERE ah.customerId = :customerId")
    fun findByCustomerId(customerId: Long): List<Account>
}