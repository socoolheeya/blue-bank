package com.socoolheeya.bluebank.testing

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.context.annotation.Configuration

val testSupportScenario by testSuite {
    test("expectThrows returns the expected failure") {
        val failure = expectThrows<IllegalArgumentException> {
            throw IllegalArgumentException("wrong type")
        }

        check(failure.message == "wrong type")
    }

    test("expectThrows rejects the absence of an exception") {
        val failure = try {
            expectThrows<IllegalArgumentException> {}
            null
        } catch (caught: AssertionError) {
            caught
        }

        check(failure != null)
    }

    test("Spring context fixture owns a closeable context") {
        val context = SpringContextFixture.start(EmptyTestConfiguration::class.java)

        check(context.isActive)
        context.close()
        check(!context.isActive)
    }
}

@Configuration(proxyBeanMethods = false)
private class EmptyTestConfiguration
