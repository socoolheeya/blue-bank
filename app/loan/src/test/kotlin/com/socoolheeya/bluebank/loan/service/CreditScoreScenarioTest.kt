package com.socoolheeya.bluebank.loan.service

import de.infix.testBalloon.framework.core.testSuite

val creditScoreScenarios by testSuite("Credit score scenarios") {
    test("credit score response is stable for every customer") {
        val score = CreditScoreService().getCreditScore(42)
        check(score.score == 750 && score.grade == "3등급" && score.agency == "NICE")
    }
}
