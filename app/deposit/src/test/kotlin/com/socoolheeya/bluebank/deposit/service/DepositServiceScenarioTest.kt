package com.socoolheeya.bluebank.deposit.service

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.dto.DepositDto
import com.socoolheeya.bluebank.deposit.testing.FakeAccountServiceClient
import com.socoolheeya.bluebank.deposit.testing.FakeDepositDataService
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate

private val start = LocalDate.of(2026, 7, 17)
private fun request(customer: Long = 7, account: Long = 9, product: String = "FIXED_DEPOSIT") = DepositDto.CreateRequest(
    customer, account, product, BigDecimal("100000"), BigDecimal("3.25"), 12, "MONTH", start, start.plusYears(1),
    monthlyPayment = BigDecimal("10000"), autoTransferEnabled = true, autoTransferDay = 17
)

val depositServiceScenarios by testSuite("Deposit service scenarios") {
    test("creation validates account ownership and maps product period and money") {
        val data = FakeDepositDataService()
        val accounts = FakeAccountServiceClient().apply { addAccount(9, 7) }
        val response = DepositService(data, accounts).createDeposit(request())
        val command = data.created.single()
        check(response.id != null && response.productType == DepositProductType.FIXED_DEPOSIT.description)
        check(command.productType == DepositProductType.FIXED_DEPOSIT && command.periodUnit.name == "MONTH")
        check(command.principalAmount.compareTo(BigDecimal("100000")) == 0)
        check(command.depositNumber.startsWith("DEP"))
    }

    test("creation rejects invalid accounts and accounts owned by another customer") {
        val accounts = FakeAccountServiceClient().apply { addAccount(9, 8) }
        val service = DepositService(FakeDepositDataService(), accounts)
        accounts.invalidAccountIds += 9
        check(runCatching { service.createDeposit(request()) }.exceptionOrNull() is IllegalArgumentException)
        accounts.invalidAccountIds.clear()
        check(runCatching { service.createDeposit(request()) }.exceptionOrNull() is IllegalArgumentException)
    }

    test("generated deposit numbers preserve prefix and uniqueness") {
        val data = FakeDepositDataService()
        val service = DepositService(data, FakeAccountServiceClient().apply { addAccount(9, 7) })
        repeat(25) { service.createDeposit(request()) }
        val numbers = data.created.map { it.depositNumber }
        check(numbers.all { it.startsWith("DEP") } && numbers.toSet().size == numbers.size)
    }

    test("owner can activate contribute withdraw and terminate with state changes") {
        val data = FakeDepositDataService()
        val service = DepositService(data, FakeAccountServiceClient().apply { addAccount(9, 7) })
        val id = requireNotNull(service.createDeposit(request(product = "FREE_SAVINGS")).id)
        check(service.activateDeposit(id, 7).status == "활성")
        check(service.deposit(id, 7, DepositDto.DepositRequest(BigDecimal("50000"), "monthly")).currentBalance.compareTo(BigDecimal("50000")) == 0)
        check(service.earlyWithdraw(id, 7, DepositDto.WithdrawRequest(BigDecimal("10000"))).currentBalance.compareTo(BigDecimal("40000")) == 0)
        check(service.terminateDeposit(id, 7).status == "해지")
        check(data.contributions.single().description == "monthly")
    }

    test("all mutations reject a different owner without changing state") {
        fun fixture(): Pair<DepositService, Long> {
            val service = DepositService(FakeDepositDataService(), FakeAccountServiceClient().apply { addAccount(9, 7) })
            return service to requireNotNull(service.createDeposit(request(product = "FREE_SAVINGS")).id)
        }
        listOf<(DepositService, Long) -> Unit>(
            { s, id -> s.activateDeposit(id, 8) },
            { s, id -> s.deposit(id, 8, DepositDto.DepositRequest(BigDecimal.ONE)) },
            { s, id -> s.earlyWithdraw(id, 8, DepositDto.WithdrawRequest(BigDecimal.ONE)) },
            { s, id -> s.terminateDeposit(id, 8) }
        ).forEach { action ->
            val (service, id) = fixture()
            check(runCatching { action(service, id) }.exceptionOrNull() is IllegalArgumentException)
            check(service.getDeposit(id).status == "대기")
        }
    }

    test("lookup and customer listing return only matching deposits and missing lookup fails") {
        val data = FakeDepositDataService()
        val accounts = FakeAccountServiceClient().apply { addAccount(9, 7); addAccount(10, 8) }
        val service = DepositService(data, accounts)
        val first = service.createDeposit(request())
        service.createDeposit(request(8, 10))
        check(service.getDeposit(requireNotNull(first.id)).customerId == 7L)
        check(service.getDepositsByCustomer(7).map { it.id } == listOf(first.id))
        check(runCatching { service.getDeposit(Long.MAX_VALUE) }.exceptionOrNull() is IllegalArgumentException)
    }

    test("invalid state transitions and monetary boundaries preserve state balance and commands") {
        val data = FakeDepositDataService()
        val service = DepositService(data, FakeAccountServiceClient().apply { addAccount(9, 7) })
        val id = requireNotNull(service.createDeposit(request(product = "FREE_SAVINGS")).id)
        fun snapshot() = Triple(service.getDeposit(id).status, service.getDeposit(id).currentBalance, data.contributions.size)
        val pending = snapshot()
        check(runCatching { service.deposit(id, 7, DepositDto.DepositRequest(BigDecimal.ONE)) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.earlyWithdraw(id, 7, DepositDto.WithdrawRequest(BigDecimal.ONE)) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.terminateDeposit(id, 7) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == pending)

        service.activateDeposit(id, 7)
        check(runCatching { service.activateDeposit(id, 7) }.exceptionOrNull() is IllegalArgumentException)
        listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
            check(runCatching { service.deposit(id, 7, DepositDto.DepositRequest(amount)) }.exceptionOrNull() is IllegalArgumentException)
            check(runCatching { service.earlyWithdraw(id, 7, DepositDto.WithdrawRequest(amount)) }.exceptionOrNull() is IllegalArgumentException)
        }
        check(snapshot() == Triple("활성", BigDecimal.ZERO, 0))
        service.deposit(id, 7, DepositDto.DepositRequest(BigDecimal.TEN))
        val funded = snapshot()
        check(runCatching { service.earlyWithdraw(id, 7, DepositDto.WithdrawRequest(BigDecimal("11"))) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == funded)
        service.terminateDeposit(id, 7)
        val terminated = snapshot()
        check(runCatching { service.terminateDeposit(id, 7) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == terminated)
    }
}
