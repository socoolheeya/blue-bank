package com.socoolheeya.bluebank.loan.data

import de.infix.testBalloon.framework.core.testSuite
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums

val loanDataApplicationSuite by testSuite {
    test("loan data module smoke test") {
        check(LoanEnums.LoanType.entries.isNotEmpty())
    }
}
