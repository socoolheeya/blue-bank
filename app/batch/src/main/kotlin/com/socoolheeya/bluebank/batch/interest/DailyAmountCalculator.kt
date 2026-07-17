package com.socoolheeya.bluebank.batch.interest

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class DailyAmountCalculator {
    fun interest(balance: BigDecimal, annualRate: BigDecimal): BigDecimal =
        balance.multiply(annualRate).divide(DAYS_PER_YEAR, 12, RoundingMode.HALF_UP).toWon()

    fun fee(baseAmount: BigDecimal, rate: BigDecimal): BigDecimal =
        baseAmount.multiply(rate).toWon()

    private fun BigDecimal.toWon(): BigDecimal = setScale(0, RoundingMode.HALF_UP)

    private companion object {
        val DAYS_PER_YEAR = BigDecimal("365")
    }
}
