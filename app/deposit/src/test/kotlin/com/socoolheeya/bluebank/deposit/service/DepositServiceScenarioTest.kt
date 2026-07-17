package com.socoolheeya.bluebank.deposit.service

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.dto.DepositDto
import com.socoolheeya.bluebank.deposit.testing.FakeAccountServiceClient
import com.socoolheeya.bluebank.deposit.testing.FakeDepositDataService
import com.socoolheeya.bluebank.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate

private val start = LocalDate.of(2026, 7, 17)
private fun request(customer: Long = 7, account: Long = 9, product: String = "FIXED_DEPOSIT") = DepositDto.CreateRequest(
    customer, account, product, BigDecimal("100000"), BigDecimal("3.25"), 12, "MONTH", start, start.plusYears(1),
    monthlyPayment = BigDecimal("10000"), autoTransferEnabled = true, autoTransferDay = 17
)

private class DepositScenarioContext {
    val data = FakeDepositDataService()
    val accounts = FakeAccountServiceClient()
    val service by lazy { DepositService(data, accounts) }
    var depositId: Long? = null
    var response: DepositDto.Response? = null
    var activationResponse: DepositDto.Response? = null
    var contributionResponse: DepositDto.Response? = null
    var withdrawalResponse: DepositDto.Response? = null
    var responses: List<DepositDto.Response>? = null
    var failure: Throwable? = null
    var secondFailure: Throwable? = null
    var pendingDepositFailure: Throwable? = null
    var pendingWithdrawFailure: Throwable? = null
    var pendingTerminateFailure: Throwable? = null
    var duplicateActivationFailure: Throwable? = null
    var invalidAmountFailures: List<Throwable?> = emptyList()
    var mutationFailures: List<Throwable?> = emptyList()
    var mutationStatuses: List<String> = emptyList()
    var pendingSnapshot: Triple<String, BigDecimal, Int>? = null
    var fundedSnapshot: Triple<String, BigDecimal, Int>? = null
    var terminatedSnapshot: Triple<String, BigDecimal, Int>? = null

    fun create(product: String = "FIXED_DEPOSIT"): Long =
        requireNotNull(service.createDeposit(request(product = product)).id).also { depositId = it }

    fun snapshot(): Triple<String, BigDecimal, Int> {
        val deposit = service.getDeposit(requireNotNull(depositId))
        return Triple(deposit.status, deposit.currentBalance, data.contributions.size)
    }
}

val depositServiceScenarios by testSuite("Deposit service scenarios") {
    Scenario("creation validates account ownership and maps product period and money", ::DepositScenarioContext) {
        Given("an account owned by the deposit customer") {
            accounts.addAccount(9, 7)
        }
        When("a fixed deposit is created") {
            response = service.createDeposit(request())
        }
        Then("the response and creation command preserve product period and money") {
            val command = data.created.single()
            check(response!!.id != null && response!!.productType == DepositProductType.FIXED_DEPOSIT.description)
            check(command.productType == DepositProductType.FIXED_DEPOSIT && command.periodUnit.name == "MONTH")
            check(command.principalAmount.compareTo(BigDecimal("100000")) == 0)
            check(command.depositNumber.startsWith("DEP"))
        }
    }

    Scenario("creation rejects invalid accounts and accounts owned by another customer", ::DepositScenarioContext) {
        Given("an invalid account owned by another customer") {
            accounts.addAccount(9, 8)
            accounts.invalidAccountIds += 9
        }
        When("creation is attempted before and after the invalid marker is cleared") {
            failure = runCatching { service.createDeposit(request()) }.exceptionOrNull()
            accounts.invalidAccountIds.clear()
            secondFailure = runCatching { service.createDeposit(request()) }.exceptionOrNull()
        }
        Then("both invalidity and ownership prevent creation") {
            check(failure is IllegalArgumentException)
            check(secondFailure is IllegalArgumentException)
        }
    }

    Scenario("generated deposit numbers preserve prefix and uniqueness", ::DepositScenarioContext) {
        Given("an account owned by the deposit customer") {
            accounts.addAccount(9, 7)
        }
        When("twenty-five deposits are created") {
            repeat(25) { service.createDeposit(request()) }
        }
        Then("every deposit number has the prefix and is unique") {
            val numbers = data.created.map { it.depositNumber }
            check(numbers.all { it.startsWith("DEP") } && numbers.toSet().size == numbers.size)
        }
    }

    Scenario("owner can activate contribute withdraw and terminate with state changes", ::DepositScenarioContext) {
        Given("a pending free savings deposit owned by the customer") {
            accounts.addAccount(9, 7)
            create("FREE_SAVINGS")
        }
        When("the owner activates contributes withdraws and terminates") {
            activationResponse = service.activateDeposit(depositId!!, 7)
            contributionResponse = service.deposit(depositId!!, 7, DepositDto.DepositRequest(BigDecimal("50000"), "monthly"))
            withdrawalResponse = service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(BigDecimal("10000")))
            response = service.terminateDeposit(depositId!!, 7)
        }
        Then("each state and balance change and the contribution description are preserved") {
            check(activationResponse!!.status == "활성")
            check(contributionResponse!!.currentBalance.compareTo(BigDecimal("50000")) == 0)
            check(withdrawalResponse!!.currentBalance.compareTo(BigDecimal("40000")) == 0)
            check(response!!.status == "해지")
            check(data.contributions.single().description == "monthly")
        }
    }

    Scenario("all mutations reject a different owner without changing state", ::DepositScenarioContext) {
        Given("an account owned by another customer") {
            accounts.addAccount(9, 7)
        }
        When("each mutation is attempted against a fresh deposit by the different owner") {
            val actions = listOf<(Long) -> Unit>(
                { id -> service.activateDeposit(id, 8) },
                { id -> service.deposit(id, 8, DepositDto.DepositRequest(BigDecimal.ONE)) },
                { id -> service.earlyWithdraw(id, 8, DepositDto.WithdrawRequest(BigDecimal.ONE)) },
                { id -> service.terminateDeposit(id, 8) },
            )
            val ids = actions.map { action ->
                val id = create("FREE_SAVINGS")
                mutationFailures = mutationFailures + runCatching { action(id) }.exceptionOrNull()
                id
            }
            mutationStatuses = ids.map { service.getDeposit(it).status }
        }
        Then("every mutation fails and every fresh deposit remains pending") {
            check(mutationFailures.all { it is IllegalArgumentException })
            check(mutationStatuses == List(4) { "대기" })
        }
    }

    Scenario("lookup and customer listing return only matching deposits and missing lookup fails", ::DepositScenarioContext) {
        Given("two customers each have a deposit") {
            accounts.addAccount(9, 7)
            accounts.addAccount(10, 8)
            response = service.createDeposit(request())
            service.createDeposit(request(8, 10))
        }
        When("the first deposit and its customer's deposits are requested") {
            depositId = requireNotNull(response!!.id)
            responses = service.getDepositsByCustomer(7)
            failure = runCatching { service.getDeposit(Long.MAX_VALUE) }.exceptionOrNull()
        }
        Then("only the matching deposit is returned and the missing lookup fails") {
            check(service.getDeposit(depositId!!).customerId == 7L)
            check(responses!!.map { it.id } == listOf(response!!.id))
            check(failure is IllegalArgumentException)
        }
    }

    Scenario("invalid state transitions and monetary boundaries preserve state balance and commands", ::DepositScenarioContext) {
        Given("a pending free savings deposit") {
            accounts.addAccount(9, 7)
            create("FREE_SAVINGS")
            pendingSnapshot = snapshot()
        }
        When("pending-only invalid operations are attempted") {
            pendingDepositFailure = runCatching {
                service.deposit(depositId!!, 7, DepositDto.DepositRequest(BigDecimal.ONE))
            }.exceptionOrNull()
            pendingWithdrawFailure = runCatching {
                service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(BigDecimal.ONE))
            }.exceptionOrNull()
            pendingTerminateFailure = runCatching {
                service.terminateDeposit(depositId!!, 7)
            }.exceptionOrNull()
        }
        Then("the pending snapshot is unchanged") {
            check(pendingDepositFailure is IllegalArgumentException)
            check(pendingWithdrawFailure is IllegalArgumentException)
            check(pendingTerminateFailure is IllegalArgumentException)
            check(snapshot() == pendingSnapshot)
        }
        When("the deposit is activated and invalid monetary boundaries are attempted") {
            service.activateDeposit(depositId!!, 7)
            duplicateActivationFailure = runCatching { service.activateDeposit(depositId!!, 7) }.exceptionOrNull()
            invalidAmountFailures = listOf(BigDecimal.ZERO, BigDecimal("-1")).flatMap { amount ->
                listOf(
                    runCatching { service.deposit(depositId!!, 7, DepositDto.DepositRequest(amount)) }.exceptionOrNull(),
                    runCatching { service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(amount)) }.exceptionOrNull(),
                )
            }
        }
        Then("the active unfunded deposit remains unchanged") {
            check(duplicateActivationFailure is IllegalArgumentException)
            check(invalidAmountFailures.all { it is IllegalArgumentException })
            check(snapshot() == Triple("활성", BigDecimal.ZERO, 0))
        }
        When("a funded deposit is overdrawn") {
            service.deposit(depositId!!, 7, DepositDto.DepositRequest(BigDecimal.TEN))
            fundedSnapshot = snapshot()
            failure = runCatching {
                service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(BigDecimal("11")))
            }.exceptionOrNull()
        }
        Then("the funded snapshot is unchanged") {
            check(failure is IllegalArgumentException)
            check(snapshot() == fundedSnapshot)
        }
        When("a terminated deposit is terminated again") {
            service.terminateDeposit(depositId!!, 7)
            terminatedSnapshot = snapshot()
            failure = runCatching { service.terminateDeposit(depositId!!, 7) }.exceptionOrNull()
        }
        Then("the terminated snapshot is unchanged") {
            check(failure is IllegalArgumentException)
            check(snapshot() == terminatedSnapshot)
        }
    }
}
