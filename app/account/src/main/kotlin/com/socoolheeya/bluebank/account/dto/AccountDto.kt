package com.socoolheeya.bluebank.account.dto

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.command.AccountCommand
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import java.math.BigDecimal
import java.time.LocalDateTime

sealed interface AccountDto {

    data class CreateRequest(
        val accountNumber: String,
        val name: String? = null,
        val accountType: AccountEnums.AccountType,
        val productType: AccountEnums.ProductType,
        val status: AccountEnums.AccountStatus = AccountEnums.AccountStatus.ACTIVE,
        val interestRate: BigDecimal = BigDecimal.ZERO,
        val parentAccountId: Long? = null,
        val linkedAccountId: Long? = null,
        val customerId: Long,
        val holderRole: AccountEnums.HolderRole = AccountEnums.HolderRole.PRIMARY
    ) : AccountDto {
        fun toCommand(): AccountCommand.Create {
            return AccountCommand.Create(
                accountNumber = accountNumber,
                name = name,
                accountType = accountType,
                productType = productType,
                status = status,
                interestRate = interestRate,
                parentAccountId = parentAccountId,
                linkedAccountId = linkedAccountId
            )
        }
    }

    data class ModifyRequest(
        val accountNumber: String,
        val name: String? = null,
    ) : AccountDto {
        fun toCommand(): AccountCommand.Modify {
            return AccountCommand.Modify(
                accountNumber = accountNumber,
                name = name
            )
        }
    }

    data class SearchRequest(
        val accountNumber: String
    ) : AccountDto {
        fun toCommand(): AccountCommand.Search {
            return AccountCommand.Search(
                accountNumber = accountNumber
            )
        }
    }

    data class Response(
        val id: Long?,
        val accountNumber: String,
        val name: String?,
        val accountType: AccountEnums.AccountType,
        val productType: AccountEnums.ProductType,
        val status: AccountEnums.AccountStatus,
        val interestRate: BigDecimal,
        val openedAt: LocalDateTime?,
        val closedAt: LocalDateTime?,
        val parentAccountId: Long?,
        val linkedAccountId: Long?
    ) : AccountDto {
        companion object {
            fun from(result: AccountResult): Response {
                return Response(
                    id = result.id,
                    accountNumber = result.accountNumber,
                    name = result.name,
                    accountType = result.accountType,
                    productType = result.productType,
                    status = result.status,
                    interestRate = result.interestRate,
                    openedAt = result.openedAt,
                    closedAt = result.closedAt,
                    parentAccountId = result.parentAccountId,
                    linkedAccountId = result.linkedAccountId
                )
            }
        }
    }
}