package com.socoolheeya.bluebank.account

import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.dto.AccountDto
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDateTime

val accountDomainTests by testSuite("Account domain and mapping") {
    testSuite("Account lifecycle") {
        for ((operation, expected) in listOf(
            "close" to AccountEnums.AccountStatus.CLOSED,
            "freeze" to AccountEnums.AccountStatus.FROZEN,
            "set dormant" to AccountEnums.AccountStatus.DORMANT,
            "activate" to AccountEnums.AccountStatus.ACTIVE
        )) {
            test("$operation changes account status") {
                val account = Account(
                    accountNumber = "1000-0001",
                    accountType = AccountEnums.AccountType.CHECKING,
                    productType = AccountEnums.ProductType.BASIC_CHECKING
                )

                when (operation) {
                    "close" -> account.close()
                    "freeze" -> account.freeze()
                    "set dormant" -> account.setDormant()
                    "activate" -> account.activate()
                }

                check(account.status == expected)
                if (operation == "close") check(account.closedAt != null)
            }
        }
    }

    testSuite("Balance rules") {
        test("deposit and hold recalculate available balance") {
            val balance = Balance(
                accountId = 1L,
                ledgerBalance = BigDecimal("100.00"),
                availableBalance = BigDecimal("100.00"),
                holdBalance = BigDecimal.ZERO,
                updatedAt = LocalDateTime.now()
            )

            balance.deposit(BigDecimal("25.00"))
            balance.addHold(BigDecimal("40.00"))

            check(balance.ledgerBalance == BigDecimal("125.00"))
            check(balance.availableBalance == BigDecimal("85.00"))
        }

        for (amount in listOf(BigDecimal("0.01"), BigDecimal("50.00"), BigDecimal("100.00"))) {
            test("withdraw $amount succeeds when funds are available") {
                val balance = Balance(
                    accountId = 1L,
                    ledgerBalance = BigDecimal("100.00"),
                    availableBalance = BigDecimal("100.00"),
                    holdBalance = BigDecimal.ZERO,
                    updatedAt = LocalDateTime.now()
                )

                balance.withdraw(amount)

                check(balance.ledgerBalance == BigDecimal("100.00").subtract(amount))
            }
        }

        test("withdraw rejects an amount greater than available balance") {
            val balance = Balance(
                accountId = 1L,
                ledgerBalance = BigDecimal("100.00"),
                availableBalance = BigDecimal("20.00"),
                holdBalance = BigDecimal("80.00"),
                updatedAt = LocalDateTime.now()
            )

            val failure = runCatching { balance.withdraw(BigDecimal("20.01")) }.exceptionOrNull()

            check(failure is IllegalArgumentException)
            check(failure.message == "Insufficient balance")
        }
    }

    testSuite("Command and DTO converters") {
        test("create request maps to command and entity without losing relationships") {
            val request = AccountDto.CreateRequest(
                accountNumber = "1000-0002",
                name = "생활비",
                accountType = AccountEnums.AccountType.CHECKING,
                productType = AccountEnums.ProductType.BASIC_CHECKING,
                interestRate = BigDecimal("0.035"),
                parentAccountId = 10L,
                linkedAccountId = 11L,
                customerId = 42L
            )

            val command = request.toCommand()
            val entity = command.toEntity()

            check(command.accountNumber == request.accountNumber)
            check(entity.accountNumber == request.accountNumber)
            check(entity.parentAccountId == 10L)
            check(entity.linkedAccountId == 11L)
            check(entity.openedAt != null)
        }

        test("result maps to response for serialization boundary") {
            val openedAt = LocalDateTime.of(2026, 7, 17, 12, 0)
            val result = AccountResult(
                id = 7L,
                accountNumber = "1000-0003",
                name = "급여",
                accountType = AccountEnums.AccountType.CHECKING,
                productType = AccountEnums.ProductType.BASIC_CHECKING,
                status = AccountEnums.AccountStatus.ACTIVE,
                interestRate = BigDecimal("0.02"),
                openedAt = openedAt,
                closedAt = null,
                parentAccountId = null,
                linkedAccountId = null
            )

            val response = AccountDto.Response.from(result)

            check(response.id == 7L)
            check(response.accountNumber == "1000-0003")
            check(response.openedAt == openedAt)
            check(response.status == AccountEnums.AccountStatus.ACTIVE)
        }
    }
}
