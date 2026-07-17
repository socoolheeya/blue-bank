package com.socoolheeya.bluebank.testing

import de.infix.testBalloon.framework.core.testSuite

private class RecordingContext {
    val events = mutableListOf<String>()
}

val scenarioDslContract by testSuite("Scenario DSL contract") {
    testFixture { RecordingContext() } asContextForEach {
        Scenario("steps share one context in declaration order") {
            Given("an empty recording") { check(events.isEmpty()); events += "given" }
            When("an action is recorded") { events += "when" }
            Then("the ordered recording is visible") { check(events == listOf("given", "when")) }
        }
        Scenario("each scenario receives fresh state") {
            Then("the recording starts empty") { check(events.isEmpty()) }
        }
        Scenario("failure identifies its step") {
            Then("the assertion fails with step context") {
                val failure = runCatching {
                    ScenarioScope(this).Then("balance is updated") { error("original") }
                }.exceptionOrNull()
                check(failure is AssertionError)
                check(failure.message == "Then: balance is updated")
                check(failure.cause?.message == "original")
            }
        }
    }
}
