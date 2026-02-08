package com.socoolheeya.bluebank.account.data.domain.result

import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountResult(
    val id: Long?,
    val accountNumber: String,
    val name: String?,
    val accountType: AccountEnums.AccountType,
    val productType: AccountEnums.ProductType,
    val status: AccountEnums.AccountStatus,
    val interestRate: BigDecimal,
    val openedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val parentAccountId: Long? = null,
    val linkedAccountId: Long? = null
) {
    companion object {
        fun from(account: Account): AccountResult {
            return AccountResult(
                id = account.id,
                accountNumber = account.accountNumber,
                name = account.name,
                accountType = account.accountType,
                productType = account.productType,
                status = account.status,
                interestRate = account.interestRate,
                openedAt = account.openedAt,
                closedAt = account.closedAt,
                parentAccountId = account.parentAccountId,
                linkedAccountId = account.linkedAccountId
            )
        }
    }
}