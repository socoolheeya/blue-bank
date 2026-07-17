package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite

private class CreditScoreScenarioContext {
    val service = CreditScoreService()
    var result: CreditScoreService.CreditScoreResult? = null
}

val creditScoreScenarios by testSuite("Credit score scenarios") {
    Scenario("credit score response is stable for every customer", ::CreditScoreScenarioContext) {
        When("a customer's credit score is requested") { result = service.getCreditScore(42) }
        Then("the stable NICE score is returned") {
            check(result?.score == 750)
            check(result?.grade == "3등급")
            check(result?.agency == "NICE")
        }
    }
}
