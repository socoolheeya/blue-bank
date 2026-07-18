package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.InterestPayment
import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import com.socoolheeya.bluebank.account.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

private class InterestScenarioContext {
    val fake = FakeAccountDataServices()
    val service = InterestService(fake.interestDataService)
    var payment: InterestPayment? = null
    var amount: BigDecimal? = null
    var payments: List<InterestPayment>? = null
}

val interestServiceScenarios by testSuite("Interest calculations") {
    testFixture { InterestScenarioContext() } asContextForEach {
        Scenario("monthly calculation returns the data service payment") {
            val month = YearMonth.of(2026, 1)
            Given("a monthly interest payment") {
                payment = fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1))
            }
            Then("the data service payment and request month are preserved") {
                check(service.calculateAndPayMonthlyInterest(7, month) === payment)
                check(fake.lastMonthlyInterestRequest == 7L to month)
            }
        }
        Scenario("average calculation preserves its period") {
            val start = LocalDate.of(2026, 1, 1)
            val end = LocalDate.of(2026, 1, 31)
            When("the average daily balance is calculated") {
                amount = service.calculateAverageDailyBalance(7, start, end)
            }
            Then("the amount and date period are preserved") {
                check(amount == BigDecimal("125.50"))
                check(fake.lastAveragePeriod == Triple(7L, start, end))
            }
        }
        Scenario("total and complete history are returned") {
            Given("two interest payments") {
                fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1))
                fake.payment(2, 7, "9", LocalDate.of(2026, 2, 1))
            }
            When("the total and complete history are requested") {
                amount = service.getTotalInterestReceived(7)
                payments = service.getInterestPaymentHistory(7)
            }
            Then("the total and history include both payments") {
                check(amount == BigDecimal("20"))
                check(payments!!.size == 2)
            }
        }
        Scenario("period history forwards dates and filters results") {
            val start = LocalDate.of(2026, 2, 1)
            val end = LocalDate.of(2026, 4, 1)
            Given("payments inside and outside the period") {
                fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1))
                fake.payment(2, 7, "9", LocalDate.of(2026, 3, 1))
            }
            When("history is requested for the period") {
                payments = service.getInterestPaymentHistoryByPeriod(7, start, end)
            }
            Then("the dates are forwarded and results are filtered") {
                check(payments!!.single().id == 2L)
                check(fake.lastHistoryPeriod == Triple(7L, start, end))
            }
        }
        Scenario("expected interest preserves explicit and default month arguments") {
            When("expected interest is calculated with explicit and default months") {
                check(service.calculateExpectedInterest(7, 3) == BigDecimal("30"))
                check(fake.lastExpectedMonths == 3)
                amount = service.calculateExpectedInterest(7)
            }
            Then("both month arguments are preserved") {
                check(amount == BigDecimal("10"))
                check(fake.lastExpectedMonths == 1)
            }
        }
    }
}
