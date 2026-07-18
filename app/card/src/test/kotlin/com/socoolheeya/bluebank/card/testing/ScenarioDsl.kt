package com.socoolheeya.bluebank.card.testing

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestFixture

class ScenarioScope<C>(private val context: C) {
    fun Given(description: String, action: C.() -> Unit) = step("Given", description, action)
    fun When(description: String, action: C.() -> Unit) = step("When", description, action)
    fun Then(description: String, assertion: C.() -> Unit) = step("Then", description, assertion)

    private fun step(kind: String, description: String, block: C.() -> Unit) {
        try {
            context.block()
        } catch (failure: Exception) {
            throw AssertionError("$kind: $description", failure)
        } catch (failure: AssertionError) {
            throw AssertionError("$kind: $description", failure)
        }
    }
}

fun <C : Any> TestFixture.Scope<suspend C.(Test.ExecutionScope) -> Unit>.Scenario(
    name: String,
    body: ScenarioScope<C>.() -> Unit,
) {
    test(name) {
        ScenarioScope(this).body()
    }
}
