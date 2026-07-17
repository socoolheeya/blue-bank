package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.dto.AccountDto
import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

val accountServiceScenarios by testSuite("Account lifecycle") {
    val limits = mapOf(
        AccountEnums.ProductType.BASIC_CHECKING to Triple("10000000", "5000000", null),
        AccountEnums.ProductType.GROUP_MEETING to Triple("5000000", "2000000", null),
        AccountEnums.ProductType.CHILD_ACCOUNT to Triple("1000000", "500000", null),
        AccountEnums.ProductType.RECORD_BOOK to Triple("10000000", "5000000", "30000000"),
        AccountEnums.ProductType.SAFEBOX to Triple("10000000", "5000000", null)
    )
    limits.forEach { (product, expected) ->
        test("create uses the $product default limit") {
            val fake = FakeAccountDataServices(); val service = AccountService(fake.accountDataService)
            val result = service.createAccount(AccountDto.CreateRequest("number-$product", "name", AccountEnums.AccountType.CHECKING, product, customerId = 9))
            check(result.accountNumber == "number-$product")
            check(fake.lastLimit!!.dailyTransferLimit == BigDecimal(expected.first))
            check(fake.lastLimit!!.singleTransferLimit == BigDecimal(expected.second))
            check(fake.lastLimit!!.monthlyDepositLimit == expected.third?.let(::BigDecimal))
        }
    }
    test("lookups return accounts by id and number") {
        val fake = FakeAccountDataServices(); val expected = fake.account(7, "700"); val service = AccountService(fake.accountDataService)
        check(service.getAccountById(7) == expected); check(service.getAccountByAccountNumber("700") == expected)
    }
    test("missing lookups fail with the requested identifier") {
        val service = AccountService(FakeAccountDataServices().accountDataService)
        val byId = runCatching { service.getAccountById(88) }.exceptionOrNull()
        val byNumber = runCatching { service.getAccountByAccountNumber("missing") }.exceptionOrNull()
        check(byId is NoSuchElementException && byId.message!!.contains("88"))
        check(byNumber is NoSuchElementException && byNumber.message!!.contains("missing"))
    }
    test("customer listing and modification reflect fake state") {
        val fake = FakeAccountDataServices(); fake.account(1, "one", customerId = 42); fake.account(2, "two", customerId = 42)
        val service = AccountService(fake.accountDataService)
        check(service.getAccountsByCustomerId(42).map { it.id } == listOf(1L, 2L))
        check(service.modifyAccount(AccountDto.ModifyRequest("two", "renamed")).name == "renamed")
        check(fake.accounts[2]!!.name == "renamed")
    }
    test("missing modification fails") {
        val service = AccountService(FakeAccountDataServices().accountDataService)
        check(runCatching { service.modifyAccount(AccountDto.ModifyRequest("missing", "x")) }.exceptionOrNull() is NoSuchElementException)
    }
    test("close freeze and activate mutate account state") {
        val fake = FakeAccountDataServices(); fake.account(1, "one"); val service = AccountService(fake.accountDataService)
        check(service.freezeAccount("one", 4, "risk").status == AccountEnums.AccountStatus.FROZEN)
        check(service.activateAccount("one", 4).status == AccountEnums.AccountStatus.ACTIVE)
        check(service.closeAccount("one", 4, "done").status == AccountEnums.AccountStatus.CLOSED)
        check(fake.accounts[1]!!.closedAt != null)
    }
}
