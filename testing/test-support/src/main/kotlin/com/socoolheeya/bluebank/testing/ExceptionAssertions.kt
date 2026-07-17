package com.socoolheeya.bluebank.testing

suspend inline fun <reified T : Throwable> expectThrows(
    noinline block: suspend () -> Unit,
): T {
    try {
        block()
    } catch (failure: Throwable) {
        if (failure is T) {
            return failure
        }
        throw AssertionError(
            "Expected ${T::class.qualifiedName}, but caught ${failure::class.qualifiedName}",
            failure,
        )
    }

    throw AssertionError("Expected ${T::class.qualifiedName} to be thrown")
}
