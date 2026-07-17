package com.socoolheeya.bluebank.batch.interest

import com.socoolheeya.bluebank.batch.support.BusinessDate
import com.socoolheeya.bluebank.batch.support.IdempotencyKeys
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate

val dailyAmountCalculatorTests by testSuite("Daily EOD amount calculation") {
    test("calculates daily interest and rounds half up to won") {
        val calculator = DailyAmountCalculator()

        check(calculator.interest(BigDecimal("100000"), BigDecimal("0.0365")) == BigDecimal("10"))
        check(calculator.fee(BigDecimal("150"), BigDecimal("0.01")) == BigDecimal("2"))
    }

    test("returns zero for zero balance or rate") {
        val calculator = DailyAmountCalculator()

        check(calculator.interest(BigDecimal.ZERO, BigDecimal("0.0365")) == BigDecimal.ZERO)
        check(calculator.interest(BigDecimal("100000"), BigDecimal.ZERO) == BigDecimal.ZERO)
    }

    test("creates stable business idempotency keys") {
        val date = LocalDate.of(2026, 7, 16)

        check(IdempotencyKeys.accounting(date, "ACCOUNT", 42L, "INTEREST") == "2026-07-16:ACCOUNT:42:INTEREST")
        check(IdempotencyKeys.transfer(date, 7L) == "2026-07-16:TRANSFER:7")
    }

    test("requires an ISO business date") {
        check(BusinessDate.parse("2026-07-16") == LocalDate.of(2026, 7, 16))
        check(runCatching { BusinessDate.parse(null) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { BusinessDate.parse("16-07-2026") }.exceptionOrNull() is IllegalArgumentException)
    }
}
