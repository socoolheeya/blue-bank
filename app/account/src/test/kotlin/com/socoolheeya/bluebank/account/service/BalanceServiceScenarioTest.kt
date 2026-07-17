package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

val balanceServiceScenarios by testSuite("Balance rules") {
    test("balance lookup returns state and missing lookup fails") {
        val fake = FakeAccountDataServices(); val expected = fake.balance(1, "100"); val service = BalanceService(fake.balanceDataService)
        check(service.getBalance(1) === expected)
        check(runCatching { service.getBalance(2) }.exceptionOrNull() is NoSuchElementException)
    }
    test("deposit and withdraw forward all arguments and update balance") {
        val fake = FakeAccountDataServices(); fake.balance(1, "100"); val service = BalanceService(fake.balanceDataService)
        check(service.deposit(1, BigDecimal("25"), "deposit", "memo", 4, "tx-d").ledgerBalance == BigDecimal("125"))
        check(fake.lastDeposit == listOf(1L, BigDecimal("25"), "deposit", "memo", 4L, "tx-d"))
        check(service.withdraw(1, BigDecimal("20"), "withdraw", "memo2", "tx-w").ledgerBalance == BigDecimal("105"))
        check(fake.lastWithdraw == listOf(1L, BigDecimal("20"), "withdraw", "memo2", "tx-w"))
    }
    test("transfer moves funds and applies default descriptions") {
        val fake = FakeAccountDataServices(); fake.balance(1, "100"); fake.balance(2, "10"); val service = BalanceService(fake.balanceDataService)
        val (from, to) = service.transfer(1, 2, BigDecimal("30"), transactionId = "tx")
        check(from.ledgerBalance == BigDecimal("70")); check(to.ledgerBalance == BigDecimal("40"))
        check(fake.lastWithdraw!![2] == "이체 출금"); check(fake.lastDeposit!![2] == "이체 입금")
    }
    listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
        test("transfer rejects $amount") {
            val fake = FakeAccountDataServices(); fake.balance(1, "100"); fake.balance(2, "0")
            check(runCatching { BalanceService(fake.balanceDataService).transfer(1, 2, amount) }.exceptionOrNull() is IllegalArgumentException)
            check(fake.balances[1]!!.ledgerBalance == BigDecimal("100"))
        }
    }
    test("transfer rejects the same account") {
        val fake = FakeAccountDataServices(); fake.balance(1, "100")
        val error = runCatching { BalanceService(fake.balanceDataService).transfer(1, 1, BigDecimal.ONE) }.exceptionOrNull()
        check(error is IllegalArgumentException && error.message == "동일한 계좌로는 이체할 수 없습니다")
    }
}
