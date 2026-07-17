package com.socoolheeya.bluebank.deposit.data

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.PeriodUnit
import com.socoolheeya.bluebank.deposit.data.domain.command.DepositCommand
import com.socoolheeya.bluebank.deposit.data.service.DepositDataService
import com.socoolheeya.bluebank.deposit.data.repository.DepositRepository
import com.socoolheeya.bluebank.deposit.data.repository.DepositTransactionRepository
import com.socoolheeya.bluebank.deposit.data.repository.InterestPaymentRepository
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootApplication(scanBasePackages = ["com.socoolheeya.bluebank.deposit.data"])
private class DepositDataIntegrationConfiguration

private val context by lazy {
    SpringApplicationBuilder(DepositDataIntegrationConfiguration::class.java)
        .web(WebApplicationType.NONE).profiles("integration").run()
}

private fun command(number: String, customer: Long, product: DepositProductType = DepositProductType.FREE_SAVINGS) = DepositCommand.Create(
    depositNumber = number, customerId = customer, accountId = customer + 100, productType = product,
    principalAmount = BigDecimal("100000.00"), baseRate = BigDecimal("3.25"), contractPeriod = 12,
    periodUnit = PeriodUnit.MONTH, startDate = LocalDate.of(2026, 7, 17), maturityDate = LocalDate.of(2027, 7, 17)
)

val depositDataIntegrationTests by testSuite("Deposit data H2 integration") {
    val service = context.getBean(DepositDataService::class.java)
    fun clean() {
        context.getBean(InterestPaymentRepository::class.java).deleteAll()
        context.getBean(DepositTransactionRepository::class.java).deleteAll()
        context.getBean(DepositRepository::class.java).deleteAll()
    }

    test("create get number lookup and customer query persist through real repositories") {
        clean()
        val first = service.createDeposit(command("DEP-IT-CREATE-1", 7101))
        service.createDeposit(command("DEP-IT-CREATE-2", 7101, DepositProductType.FIXED_DEPOSIT))
        service.createDeposit(command("DEP-IT-OTHER", 7102))
        check(service.getDeposit(requireNotNull(first.id)).depositNumber == "DEP-IT-CREATE-1")
        check(service.getDepositByNumber("DEP-IT-CREATE-2")?.customerId == 7101L)
        check(service.getDepositByNumber("DEP-IT-MISSING") == null)
        check(service.getDepositsByCustomer(7101).map { it.depositNumber }.toSet() == setOf("DEP-IT-CREATE-1", "DEP-IT-CREATE-2"))
    }

    test("activation contribution and withdrawal update balance and persist transaction records") {
        clean()
        val id = requireNotNull(service.createDeposit(command("DEP-IT-MONEY", 7201)).id)
        check(service.activateDeposit(id).status == DepositStatus.ACTIVE)
        val contribution = service.deposit(id, BigDecimal("50000.50"), "monthly payment")
        check(contribution.balanceAfter.compareTo(BigDecimal("50000.50")) == 0 && contribution.description == "monthly payment")
        check(service.earlyWithdraw(id, BigDecimal("10000.25")).currentBalance.compareTo(BigDecimal("40000.25")) == 0)
        check(service.getDeposit(id).currentBalance.compareTo(BigDecimal("40000.25")) == 0)
        val transactions = service.getTransactions(id)
        check(transactions.map { it.transactionType }.toSet() == setOf(DepositTransactionType.DEPOSIT, DepositTransactionType.EARLY_WITHDRAWAL))
        check(transactions.first { it.transactionType == DepositTransactionType.EARLY_WITHDRAWAL }.balanceAfter.compareTo(BigDecimal("40000.25")) == 0)
    }

    test("termination state and payout record persist") {
        clean()
        val id = requireNotNull(service.createDeposit(command("DEP-IT-TERM", 7301, DepositProductType.FIXED_DEPOSIT)).id)
        service.activateDeposit(id)
        check(service.terminateDeposit(id).status == DepositStatus.TERMINATED)
        check(service.getDeposit(id).status == DepositStatus.TERMINATED)
        val payout = service.getTransactions(id).single()
        check(payout.transactionType == DepositTransactionType.TERMINATION)
        check(payout.amount.compareTo(BigDecimal("100000.00")) == 0 && payout.balanceAfter.compareTo(BigDecimal.ZERO) == 0)
    }

    test("maturity interest bonus rate and interest payment state survive reload") {
        clean()
        val matureId = requireNotNull(service.createDeposit(command("DEP-IT-MATURE", 7401, DepositProductType.FIXED_DEPOSIT)).id)
        service.activateDeposit(matureId)
        service.updateBonusRate(matureId, BigDecimal("0.50"))
        check(service.matureDeposit(matureId, BigDecimal("3250.00")).status == DepositStatus.MATURED)
        val matured = service.getDeposit(matureId)
        check(matured.accumulatedInterest.compareTo(BigDecimal("3250.00")) == 0)
        check(matured.appliedRate.compareTo(BigDecimal("3.75")) == 0)

        val interestId = requireNotNull(service.createDeposit(command("DEP-IT-INTEREST", 7402, DepositProductType.FIXED_DEPOSIT)).id)
        val payment = service.payInterest(interestId, BigDecimal("1000"))
        check(payment.taxAmount.compareTo(BigDecimal("154")) == 0 && payment.netInterest.compareTo(BigDecimal("846")) == 0)
        check(service.getInterestPayments(interestId).single().id == payment.id)
        check(service.getTransactions(interestId).single().transactionType == DepositTransactionType.INTEREST_PAYMENT)
        check(service.getDeposit(interestId).currentBalance.compareTo(BigDecimal("100846.00")) == 0)
    }

    test("missing deposit operations fail against real repository") {
        clean()
        check(runCatching { service.getDeposit(Long.MAX_VALUE) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.activateDeposit(Long.MAX_VALUE) }.exceptionOrNull() is IllegalArgumentException)
    }

    test("invalid lifecycle transitions preserve state balance and transaction count") {
        clean()
        val id = requireNotNull(service.createDeposit(command("DEP-IT-STATE", 7501)).id)
        fun snapshot() = Triple(service.getDeposit(id).status, service.getDeposit(id).currentBalance, service.getTransactions(id).size)
        val pending = snapshot()
        check(runCatching { service.deposit(id, BigDecimal.TEN, null) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.earlyWithdraw(id, BigDecimal.ONE) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.terminateDeposit(id) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == pending)
        service.activateDeposit(id)
        check(runCatching { service.activateDeposit(id) }.exceptionOrNull() is IllegalArgumentException)
        check(service.getDeposit(id).status == DepositStatus.ACTIVE && service.getTransactions(id).isEmpty())
        service.deposit(id, BigDecimal.TEN, null)
        val funded = snapshot()
        check(runCatching { service.earlyWithdraw(id, BigDecimal("11")) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == funded)
        service.terminateDeposit(id)
        val terminated = snapshot()
        check(runCatching { service.terminateDeposit(id) }.exceptionOrNull() is IllegalArgumentException)
        check(snapshot() == terminated)
    }

    test("zero and negative contributions and withdrawals are rejected without mutation") {
        clean()
        val id = requireNotNull(service.createDeposit(command("DEP-IT-SIGNED", 7601)).id)
        service.activateDeposit(id)
        service.deposit(id, BigDecimal("100"), "seed")
        val beforeBalance = service.getDeposit(id).currentBalance
        val beforeCount = service.getTransactions(id).size
        listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
            check(runCatching { service.deposit(id, amount, "invalid") }.exceptionOrNull() is IllegalArgumentException)
            check(runCatching { service.earlyWithdraw(id, amount) }.exceptionOrNull() is IllegalArgumentException)
        }
        check(service.getDeposit(id).currentBalance.compareTo(beforeBalance) == 0)
        check(service.getTransactions(id).size == beforeCount)
    }
}
