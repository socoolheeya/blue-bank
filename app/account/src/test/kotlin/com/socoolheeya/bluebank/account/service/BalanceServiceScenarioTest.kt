package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import com.socoolheeya.bluebank.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

private class BalanceScenarioContext {
    val fake = FakeAccountDataServices()
    val service = BalanceService(fake.balanceDataService)
    var balance: Balance? = null
    var lookupResult: Balance? = null
    var transfer: Pair<Balance, Balance>? = null
    var failure: Throwable? = null
}

val balanceServiceScenarios by testSuite("Balance rules") {
    testFixture { BalanceScenarioContext() } asContextForEach {
        Scenario("balance lookup returns state and missing lookup fails") {
            Given("one account has a balance") {
                balance = fake.balance(1, "100")
            }
            When("existing and missing balances are requested") {
                lookupResult = service.getBalance(1)
                failure = runCatching { service.getBalance(2) }.exceptionOrNull()
            }
            Then("the existing balance is returned and the missing lookup fails") {
                check(lookupResult === balance)
                check(failure is NoSuchElementException)
            }
        }
        Scenario("deposit and withdraw forward all arguments and update balance") {
            Given("an account has a balance") {
                fake.balance(1, "100")
            }
            When("funds are deposited and withdrawn") {
                check(service.deposit(1, BigDecimal("25"), "deposit", "memo", 4, "tx-d").ledgerBalance == BigDecimal("125"))
                check(fake.lastDeposit == listOf(1L, BigDecimal("25"), "deposit", "memo", 4L, "tx-d"))
                balance = service.withdraw(1, BigDecimal("20"), "withdraw", "memo2", "tx-w")
            }
            Then("all arguments and the updated balance are preserved") {
                check(balance!!.ledgerBalance == BigDecimal("105"))
                check(fake.lastWithdraw == listOf(1L, BigDecimal("20"), "withdraw", "memo2", "tx-w"))
            }
        }
        Scenario("transfer moves funds and applies default descriptions") {
            Given("source and destination balances") {
                fake.balance(1, "100")
                fake.balance(2, "10")
            }
            When("funds are transferred") {
                transfer = service.transfer(1, 2, BigDecimal("30"), transactionId = "tx")
            }
            Then("both balances and descriptions are updated") {
                check(transfer!!.first.ledgerBalance == BigDecimal("70"))
                check(transfer!!.second.ledgerBalance == BigDecimal("40"))
                check(fake.lastWithdraw!![2] == "이체 출금")
                check(fake.lastDeposit!![2] == "이체 입금")
            }
        }
        listOf(BigDecimal.ZERO, BigDecimal("-1")).forEach { amount ->
            Scenario("transfer rejects $amount") {
                Given("source and destination balances") {
                    fake.balance(1, "100")
                    fake.balance(2, "0")
                }
                When("an invalid amount is transferred") {
                    failure = runCatching { service.transfer(1, 2, amount) }.exceptionOrNull()
                }
                Then("the transfer fails without changing the source balance") {
                    check(failure is IllegalArgumentException)
                    check(fake.balances[1]!!.ledgerBalance == BigDecimal("100"))
                }
            }
        }
        Scenario("transfer rejects the same account") {
            Given("an account has a balance") {
                fake.balance(1, "100")
            }
            When("the account transfers to itself") {
                failure = runCatching { service.transfer(1, 1, BigDecimal.ONE) }.exceptionOrNull()
            }
            Then("the transfer fails with the same-account message") {
                check(failure is IllegalArgumentException && failure!!.message == "동일한 계좌로는 이체할 수 없습니다")
            }
        }
    }
}
