package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.testing.FakeAccountDataServices
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

val interestServiceScenarios by testSuite("Interest calculations") {
    test("monthly calculation returns the data service payment") {
        val fake = FakeAccountDataServices(); val expected = fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1))
        check(InterestService(fake.interestDataService).calculateAndPayMonthlyInterest(7, YearMonth.of(2026, 1)) === expected)
    }
    test("average calculation preserves its period") {
        val fake = FakeAccountDataServices(); val service = InterestService(fake.interestDataService)
        val start = LocalDate.of(2026, 1, 1); val end = LocalDate.of(2026, 1, 31)
        check(service.calculateAverageDailyBalance(7, start, end) == BigDecimal("125.50"))
        check(fake.lastAveragePeriod == Triple(7L, start, end))
    }
    test("total and complete history are returned") {
        val fake = FakeAccountDataServices(); fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1)); fake.payment(2, 7, "9", LocalDate.of(2026, 2, 1))
        val service = InterestService(fake.interestDataService)
        check(service.getTotalInterestReceived(7) == BigDecimal("20")); check(service.getInterestPaymentHistory(7).size == 2)
    }
    test("period history forwards dates and filters results") {
        val fake = FakeAccountDataServices(); fake.payment(1, 7, "11", LocalDate.of(2026, 1, 1)); fake.payment(2, 7, "9", LocalDate.of(2026, 3, 1))
        val start = LocalDate.of(2026, 2, 1); val end = LocalDate.of(2026, 4, 1); val service = InterestService(fake.interestDataService)
        check(service.getInterestPaymentHistoryByPeriod(7, start, end).single().id == 2L)
        check(fake.lastHistoryPeriod == Triple(7L, start, end))
    }
    test("expected interest preserves explicit and default month arguments") {
        val fake = FakeAccountDataServices(); val service = InterestService(fake.interestDataService)
        check(service.calculateExpectedInterest(7, 3) == BigDecimal("30")); check(fake.lastExpectedMonths == 3)
        check(service.calculateExpectedInterest(7) == BigDecimal("10")); check(fake.lastExpectedMonths == 1)
    }
}
