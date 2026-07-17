package com.socoolheeya.bluebank.deposit.data

import de.infix.testBalloon.framework.core.testSuite
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums

val depositDataApplicationSuite by testSuite {
    test("deposit data module smoke test") {
        check(DepositEnums.DepositStatus.entries.isNotEmpty())
    }
}
