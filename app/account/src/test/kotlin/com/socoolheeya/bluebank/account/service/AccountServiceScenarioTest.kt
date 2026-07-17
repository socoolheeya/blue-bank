package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.dto.AccountDto
import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import com.socoolheeya.bluebank.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

private class AccountScenarioContext {
    val fake = FakeAccountDataServices()
    val service = AccountService(fake.accountDataService)
    var result: AccountResult? = null
    var accounts: List<AccountResult>? = null
    var failure: Throwable? = null
    var secondFailure: Throwable? = null
}

val accountServiceScenarios by testSuite("Account lifecycle") {
    val limits = mapOf(
        AccountEnums.ProductType.BASIC_CHECKING to Triple("10000000", "5000000", null),
        AccountEnums.ProductType.GROUP_MEETING to Triple("5000000", "2000000", null),
        AccountEnums.ProductType.CHILD_ACCOUNT to Triple("1000000", "500000", null),
        AccountEnums.ProductType.RECORD_BOOK to Triple("10000000", "5000000", "30000000"),
        AccountEnums.ProductType.SAFEBOX to Triple("10000000", "5000000", null)
    )
    limits.forEach { (product, expected) ->
        Scenario("create uses the $product default limit", ::AccountScenarioContext) {
            When("the customer creates a $product account") {
                result = service.createAccount(AccountDto.CreateRequest("number-$product", "name", AccountEnums.AccountType.CHECKING, product, customerId = 9))
            }
            Then("the product defaults and account number are preserved") {
                check(result!!.accountNumber == "number-$product")
                check(fake.lastLimit!!.dailyTransferLimit == BigDecimal(expected.first))
                check(fake.lastLimit!!.singleTransferLimit == BigDecimal(expected.second))
                check(fake.lastLimit!!.monthlyDepositLimit == expected.third?.let(::BigDecimal))
            }
        }
    }
    Scenario("lookups return accounts by id and number", ::AccountScenarioContext) {
        Given("an account exists") {
            result = fake.account(7, "700")
        }
        Then("the account is returned by id and number") {
            check(service.getAccountById(7) == result)
            check(service.getAccountByAccountNumber("700") == result)
        }
    }
    Scenario("missing lookups fail with the requested identifier", ::AccountScenarioContext) {
        When("missing accounts are requested by id and number") {
            failure = runCatching { service.getAccountById(88) }.exceptionOrNull()
            secondFailure = runCatching { service.getAccountByAccountNumber("missing") }.exceptionOrNull()
        }
        Then("each failure contains its requested identifier") {
            check(failure is NoSuchElementException && failure!!.message!!.contains("88"))
            check(secondFailure is NoSuchElementException && secondFailure!!.message!!.contains("missing"))
        }
    }
    Scenario("customer listing and modification reflect fake state", ::AccountScenarioContext) {
        Given("a customer has two accounts") {
            fake.account(1, "one", customerId = 42)
            fake.account(2, "two", customerId = 42)
        }
        When("the accounts are listed and one is renamed") {
            accounts = service.getAccountsByCustomerId(42)
            result = service.modifyAccount(AccountDto.ModifyRequest("two", "renamed"))
        }
        Then("the listing and fake state reflect the modification") {
            check(accounts!!.map { it.id } == listOf(1L, 2L))
            check(result!!.name == "renamed")
            check(fake.accounts[2]!!.name == "renamed")
        }
    }
    Scenario("missing modification fails", ::AccountScenarioContext) {
        When("a missing account is modified") {
            failure = runCatching { service.modifyAccount(AccountDto.ModifyRequest("missing", "x")) }.exceptionOrNull()
        }
        Then("the modification fails") {
            check(failure is NoSuchElementException)
        }
    }
    Scenario("close freeze and activate mutate account state", ::AccountScenarioContext) {
        Given("an active account") {
            fake.account(1, "one")
        }
        Then("freeze activate and close mutate account state") {
            check(service.freezeAccount("one", 4, "risk").status == AccountEnums.AccountStatus.FROZEN)
            check(service.activateAccount("one", 4).status == AccountEnums.AccountStatus.ACTIVE)
            check(service.closeAccount("one", 4, "done").status == AccountEnums.AccountStatus.CLOSED)
            check(fake.accounts[1]!!.closedAt != null)
        }
    }
}
