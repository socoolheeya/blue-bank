package com.socoolheeya.bluebank.account.testing

import com.socoolheeya.bluebank.account.data.domain.*
import com.socoolheeya.bluebank.account.data.domain.command.AccountCommand
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.data.service.AccountDataService
import com.socoolheeya.bluebank.account.data.service.BalanceDataService
import com.socoolheeya.bluebank.account.data.service.InterestDataService
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class FakeAccountDataServices {
    val accounts = linkedMapOf<Long, AccountResult>()
    val customers = mutableMapOf<Long, MutableSet<Long>>()
    val balances = mutableMapOf<Long, Balance>()
    val interestPayments = mutableListOf<InterestPayment>()
    var lastLimit: AccountLimit? = null
    var lastDeposit: List<Any?>? = null
    var lastWithdraw: List<Any?>? = null
    var lastAveragePeriod: Triple<Long, LocalDate, LocalDate>? = null
    var lastHistoryPeriod: Triple<Long, LocalDate, LocalDate>? = null
    var lastExpectedMonths: Int? = null
    var lastMonthlyInterestRequest: Pair<Long, YearMonth>? = null

    val accountDataService: AccountDataService = mock(AccountDataService::class.java)
    val balanceDataService: BalanceDataService = mock(BalanceDataService::class.java)
    val interestDataService: InterestDataService = mock(InterestDataService::class.java)

    private var nextId = 1L

    init {
        doAnswer { invocation ->
            val command = invocation.getArgument<AccountCommand.Create>(0)
            val customerId = invocation.getArgument<Long>(1)
            lastLimit = invocation.getArgument(3)
            val result = AccountResult(nextId++, command.accountNumber, command.name, command.accountType,
                command.productType, command.status, command.interestRate, LocalDateTime.now(), null,
                command.parentAccountId, command.linkedAccountId)
            accounts[result.id!!] = result
            customers.getOrPut(customerId) { linkedSetOf() }.add(result.id!!)
            result
        }.`when`(accountDataService).createAccount(any(), any(), any(), any())

        doAnswer { invocation -> accounts.values.firstOrNull { it.accountNumber == invocation.getArgument<AccountCommand.Search>(0).accountNumber } }
            .`when`(accountDataService).searchAccount(any())
        doAnswer { invocation -> accounts[invocation.getArgument(0)] }.`when`(accountDataService).getAccountById(any())
        doAnswer { invocation -> customers[invocation.getArgument<Long>(0)].orEmpty().mapNotNull(accounts::get) }
            .`when`(accountDataService).getAccountsByCustomerId(any())
        doAnswer { invocation ->
            val command = invocation.getArgument<AccountCommand.Modify>(0)
            val old = accounts.values.firstOrNull { it.accountNumber == command.accountNumber } ?: return@doAnswer null
            old.copy(name = command.name).also { accounts[old.id!!] = it }
        }.`when`(accountDataService).modifyAccount(any())
        doAnswer { lifecycle(it.getArgument(0), AccountEnums.AccountStatus.CLOSED) }
            .`when`(accountDataService).closeAccount(any(), any(), anyOrNull())
        doAnswer { lifecycle(it.getArgument(0), AccountEnums.AccountStatus.FROZEN) }
            .`when`(accountDataService).freezeAccount(any(), any(), any())
        doAnswer { lifecycle(it.getArgument(0), AccountEnums.AccountStatus.ACTIVE) }
            .`when`(accountDataService).activateAccount(any(), any())

        doAnswer { balances[it.getArgument(0)] }.`when`(balanceDataService).getBalance(any())
        doAnswer { invocation ->
            val id = invocation.getArgument<Long>(0); val amount = invocation.getArgument<BigDecimal>(1)
            lastDeposit = (0..5).map { invocation.getArgument<Any?>(it) }
            balances.getValue(id).also { it.deposit(amount) }
        }.`when`(balanceDataService).deposit(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        doAnswer { invocation ->
            val id = invocation.getArgument<Long>(0); val amount = invocation.getArgument<BigDecimal>(1)
            lastWithdraw = (0..4).map { invocation.getArgument<Any?>(it) }
            balances.getValue(id).also { it.withdraw(amount) }
        }.`when`(balanceDataService).withdraw(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())

        doAnswer { invocation ->
            val accountId = invocation.getArgument<Long>(0)
            lastMonthlyInterestRequest = accountId to invocation.getArgument(1)
            interestPayments.firstOrNull { it.accountId == accountId }
        }
            .`when`(interestDataService).calculateAndPayMonthlyInterest(any(), any())
        doAnswer { invocation ->
            lastAveragePeriod = Triple(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
            BigDecimal("125.50")
        }.`when`(interestDataService).calculateAverageDailyBalance(any(), any(), any())
        doAnswer { invocation -> interestPayments.filter { it.accountId == invocation.getArgument<Long>(0) }.fold(BigDecimal.ZERO) { a, p -> a + p.amount } }
            .`when`(interestDataService).getTotalInterestReceived(any())
        doAnswer { invocation -> interestPayments.filter { it.accountId == invocation.getArgument<Long>(0) } }
            .`when`(interestDataService).getInterestPaymentHistory(any())
        doAnswer { invocation ->
            val id = invocation.getArgument<Long>(0); val start = invocation.getArgument<LocalDate>(1); val end = invocation.getArgument<LocalDate>(2)
            lastHistoryPeriod = Triple(id, start, end)
            interestPayments.filter { it.accountId == id && !it.calculationPeriodStart.isBefore(start) && !it.calculationPeriodStart.isAfter(end) }
        }.`when`(interestDataService).getInterestPaymentHistoryByPeriod(any(), any(), any())
        doAnswer { invocation -> lastExpectedMonths = invocation.getArgument(1); BigDecimal(invocation.getArgument<Int>(1) * 10) }
            .`when`(interestDataService).calculateExpectedInterest(any(), any())
    }

    fun account(id: Long, number: String, status: AccountEnums.AccountStatus = AccountEnums.AccountStatus.ACTIVE, customerId: Long? = null): AccountResult =
        AccountResult(id, number, "account-$id", AccountEnums.AccountType.CHECKING, AccountEnums.ProductType.BASIC_CHECKING,
            status, BigDecimal("0.02"), LocalDateTime.now(), null).also {
            accounts[id] = it; customerId?.let { customer -> customers.getOrPut(customer) { linkedSetOf() }.add(id) }
        }

    fun balance(id: Long, amount: String): Balance = Balance(id, BigDecimal(amount), BigDecimal(amount), BigDecimal.ZERO,
        updatedAt = LocalDateTime.now()).also { balances[id] = it }

    fun payment(id: Long, accountId: Long, amount: String, start: LocalDate): InterestPayment =
        InterestPayment(id, accountId, BigDecimal(amount), BigDecimal("0.02"), start, start.plusDays(29), BigDecimal("1000"))
            .also(interestPayments::add)

    private fun lifecycle(number: String, status: AccountEnums.AccountStatus): AccountResult {
        val old = accounts.values.firstOrNull { it.accountNumber == number } ?: throw NoSuchElementException(number)
        return old.copy(status = status, closedAt = if (status == AccountEnums.AccountStatus.CLOSED) LocalDateTime.now() else old.closedAt)
            .also { accounts[old.id!!] = it }
    }
}
