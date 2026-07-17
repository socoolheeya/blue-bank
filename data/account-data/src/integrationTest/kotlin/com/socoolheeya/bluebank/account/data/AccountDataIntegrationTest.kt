package com.socoolheeya.bluebank.account.data

import com.socoolheeya.bluebank.account.data.domain.*
import com.socoolheeya.bluebank.account.data.domain.command.AccountCommand
import com.socoolheeya.bluebank.account.data.repository.*
import com.socoolheeya.bluebank.account.data.service.AccountDataService
import com.socoolheeya.bluebank.account.data.service.BalanceDataService
import com.socoolheeya.bluebank.account.data.service.InterestDataService
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("com.socoolheeya.bluebank.account.data")
private open class AccountIntegrationApplication

private inline fun withAccountContext(block: (org.springframework.context.ConfigurableApplicationContext) -> Unit) {
    SpringApplicationBuilder(AccountIntegrationApplication::class.java).web(WebApplicationType.NONE).profiles("integration").run().use(block)
}

private fun createCommand(number: String, product: AccountEnums.ProductType = AccountEnums.ProductType.BASIC_CHECKING,
                          parentId: Long? = null, rate: String = "0.05") = AccountCommand.Create(
    accountNumber = number, name = number, accountType = AccountEnums.AccountType.CHECKING, productType = product,
    status = AccountEnums.AccountStatus.ACTIVE, interestRate = BigDecimal(rate), parentAccountId = parentId
)

private fun limit() = AccountLimit(0, BigDecimal("1000000"), BigDecimal("500000"))

val accountDataIntegration by testSuite("Account data integration", compartment = { TestCompartment.Sequential }) {
    test("account creation persists related rows and supports number customer product and parent queries") {
        withAccountContext { context ->
            val service = context.getBean(AccountDataService::class.java)
            val accounts = context.getBean(AccountRepository::class.java)
            val balances = context.getBean(BalanceRepository::class.java)
            val holders = context.getBean(AccountHolderRepository::class.java)
            val limits = context.getBean(AccountLimitRepository::class.java)
            val parent = service.createAccount(createCommand("parent"), 42, AccountEnums.HolderRole.PRIMARY, limit())
            val child = service.createAccount(createCommand("child", AccountEnums.ProductType.CHILD_ACCOUNT, parent.id), 42, AccountEnums.HolderRole.CHILD, limit())

            check(service.searchAccount(AccountCommand.Search("parent"))!!.id == parent.id)
            check(service.getAccountById(child.id!!)!!.parentAccountId == parent.id)
            check(service.getAccountsByCustomerId(42).map { it.accountNumber }.toSet() == setOf("parent", "child"))
            check(accounts.findByProductType(AccountEnums.ProductType.CHILD_ACCOUNT).single().id == child.id)
            check(accounts.findByParentAccountId(parent.id!!).single().id == child.id)
            check(balances.findByAccountId(parent.id!!)!!.ledgerBalance.compareTo(BigDecimal.ZERO) == 0)
            check(holders.findByAccountId(parent.id!!).single().customerId == 42L)
            check(limits.findByAccountId(parent.id!!)!!.singleTransferLimit.compareTo(BigDecimal("500000")) == 0)
        }
    }

    test("lifecycle balance ledger updates and rollback use real transactions") {
        withAccountContext { context ->
            val accountService = context.getBean(AccountDataService::class.java)
            val balanceService = context.getBean(BalanceDataService::class.java)
            val ledgers = context.getBean(LedgerEntryRepository::class.java)
            val statuses = context.getBean(AccountStatusHistoryRepository::class.java)
            val balances = context.getBean(BalanceRepository::class.java)
            val tx = TransactionTemplate(context.getBean(PlatformTransactionManager::class.java))
            val active = accountService.createAccount(createCommand("money"), 1, AccountEnums.HolderRole.PRIMARY, limit())
            balanceService.deposit(active.id!!, BigDecimal("1000"), "cash", "memo", transactionId = "d-1")
            balanceService.withdraw(active.id!!, BigDecimal("250"), "purchase", transactionId = "w-1")
            check(balances.findByAccountId(active.id!!)!!.ledgerBalance.compareTo(BigDecimal("750")) == 0)
            check(ledgers.findAll().map { it.type }.toSet() == setOf(AccountEnums.EntryType.DEPOSIT, AccountEnums.EntryType.WITHDRAWAL))

            tx.executeWithoutResult { status -> balanceService.deposit(active.id!!, BigDecimal("99")); status.setRollbackOnly() }
            check(balances.findByAccountId(active.id!!)!!.ledgerBalance.compareTo(BigDecimal("750")) == 0)

            val lifecycle = accountService.createAccount(createCommand("lifecycle"), 2, AccountEnums.HolderRole.PRIMARY, limit())
            check(accountService.freezeAccount("lifecycle", 2, "review").status == AccountEnums.AccountStatus.FROZEN)
            check(accountService.activateAccount("lifecycle", 2).status == AccountEnums.AccountStatus.ACTIVE)
            check(accountService.closeAccount("lifecycle", 2, "finished").status == AccountEnums.AccountStatus.CLOSED)
            check(statuses.findByAccountIdOrderByChangedAtDesc(lifecycle.id!!).size == 4)
        }
    }

    test("deposit and withdrawal boundaries reject invalid amounts without changing state") {
        withAccountContext { context ->
            val accounts = context.getBean(AccountDataService::class.java)
            val balances = context.getBean(BalanceDataService::class.java)
            val balanceRepository = context.getBean(BalanceRepository::class.java)
            val ledgerRepository = context.getBean(LedgerEntryRepository::class.java)
            val result = accounts.createAccount(createCommand("boundaries"), 5, AccountEnums.HolderRole.PRIMARY, limit())
            balances.deposit(result.id!!, BigDecimal("100"))
            val initialLedgerCount = ledgerRepository.count()

            listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
                check(runCatching { balances.deposit(result.id!!, amount) }.exceptionOrNull() is IllegalArgumentException)
                check(runCatching { balances.withdraw(result.id!!, amount) }.exceptionOrNull() is IllegalArgumentException)
            }
            val insufficient = runCatching { balances.withdraw(result.id!!, BigDecimal("100.01")) }.exceptionOrNull()
            check(insufficient is IllegalArgumentException && insufficient.message == "Insufficient balance")
            check(balanceRepository.findByAccountId(result.id!!)!!.ledgerBalance.compareTo(BigDecimal("100")) == 0)
            check(ledgerRepository.count() == initialLedgerCount)
        }
    }

    test("interest payment updates balance and supports totals history and period filtering") {
        withAccountContext { context ->
            val accounts = context.getBean(AccountDataService::class.java)
            val balances = context.getBean(BalanceDataService::class.java)
            val interest = context.getBean(InterestDataService::class.java)
            val result = accounts.createAccount(createCommand("interest", rate = "0.10"), 3, AccountEnums.HolderRole.PRIMARY, limit())
            balances.deposit(result.id!!, BigDecimal("100000"))
            val month = YearMonth.of(2026, 6)
            val before = balances.getBalance(result.id!!)!!.ledgerBalance
            val paid = interest.calculateAndPayMonthlyInterest(result.id!!, month)
            check(paid != null && paid.amount > BigDecimal.ZERO)
            val after = balances.getBalance(result.id!!)!!.ledgerBalance
            check(after.compareTo(before + paid.amount) == 0)
            val interestEntries = context.getBean(LedgerEntryRepository::class.java)
                .findAll().filter { it.accountId == result.id && it.type == AccountEnums.EntryType.INTEREST }
            check(interestEntries.single().amount.compareTo(paid.amount) == 0)
            check(interestEntries.single().balanceAfter.compareTo(after) == 0)
            check(interest.getTotalInterestReceived(result.id!!).compareTo(paid.amount) == 0)
            check(interest.getInterestPaymentHistory(result.id!!).single().id == paid.id)
            check(interest.getInterestPaymentHistoryByPeriod(result.id!!, month.atDay(1), month.atEndOfMonth()).single().id == paid.id)
            check(interest.getInterestPaymentHistoryByPeriod(result.id!!, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 31)).isEmpty())
            check(interest.calculateExpectedInterest(result.id!!, 2) > BigDecimal.ZERO)
        }
    }
}
