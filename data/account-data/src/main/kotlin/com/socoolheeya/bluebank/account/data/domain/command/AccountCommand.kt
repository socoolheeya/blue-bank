package com.socoolheeya.bluebank.account.data.domain.command

import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import java.math.BigDecimal
import java.time.LocalDateTime

sealed interface AccountCommand {

    data class Create(
        val accountNumber: String,
        val name: String?,
        val accountType: AccountEnums.AccountType,
        val productType: AccountEnums.ProductType,
        val status: AccountEnums.AccountStatus,
        val interestRate: BigDecimal,
        val parentAccountId: Long? = null,
        val linkedAccountId: Long? = null
    ) : AccountCommand {
        fun toEntity(): Account {
            return Account(
                accountNumber = accountNumber,
                name = name,
                accountType = accountType,
                productType = productType,
                status = status,
                interestRate = interestRate,
                openedAt = LocalDateTime.now(),
                parentAccountId = parentAccountId,
                linkedAccountId = linkedAccountId
            )
        }
    }

    data class Modify(
        val accountNumber: String,
        val name: String? = null,
    ) : AccountCommand

    data class Search(
        val accountNumber: String
    ) : AccountCommand
}