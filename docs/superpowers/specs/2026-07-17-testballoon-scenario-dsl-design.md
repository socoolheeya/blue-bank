# TestBalloon Scenario DSL Design

## Goal

Provide a shared Kotlin DSL for readable Given/When/Then scenario tests on top of TestBalloon, then migrate every existing `*ScenarioTest.kt` test to that DSL.

## Scope

- Add the DSL to the `testing:test-support` main source set so application test source sets can import it.
- Add direct `testImplementation(project(":testing:test-support"))` dependencies where scenario-test modules do not already have one.
- Migrate all nine existing files named `*ScenarioTest.kt` under `app`.
- Preserve the behavior and coverage of the existing scenarios.
- Verify the DSL itself, each affected module's TestBalloon tests, and the complete build.

This change does not migrate application, domain, slice, or integration tests that are not named `*ScenarioTest.kt`.

## DSL API

The public API consists of:

```kotlin
class ScenarioScope<C>(private val context: C) {
    fun Given(description: String, action: C.() -> Unit)
    fun When(description: String, action: C.() -> Unit)
    fun Then(description: String, assertion: C.() -> Unit)
}

fun <C> TestSuiteScope.Scenario(
    name: String,
    context: () -> C,
    body: ScenarioScope<C>.() -> Unit
)
```

`Scenario` registers exactly one TestBalloon test. Its context factory runs inside that test, ensuring fresh state for each execution. `Given`, `When`, and `Then` execute synchronously and in declaration order against the same context instance.

The capitalized function names are intentional: scenario files should read as executable specifications.

## Failure Behavior

Each step catches failures only to add the step kind and description, then rethrows with the original failure as its cause. This makes a failing report identify the exact Given/When/Then step without hiding the underlying error. Fatal JVM errors are not converted.

The DSL does not enforce a rigid phase order. Repeated `Given` or `Then` steps remain valid, keeping the API useful for scenarios with multiple setup or assertion steps.

## Scenario Structure

Each migrated scenario defines a small private context class near the suite. The context owns its fakes, system under test, input values, and captured results or failures. Steps communicate through those named properties instead of semicolon-separated local declarations.

Data-driven cases may still register multiple `Scenario` tests in a loop. Every generated scenario receives an independent context.

## Dependencies

`app/deposit` and `app/card` already depend on `testing:test-support`. Equivalent test dependencies will be added to `app/account` and `app/loan`. No production source set will depend on the testing module.

## Test Strategy

Development follows red-green-refactor:

1. Add DSL contract tests that fail because the API is absent.
2. Implement the smallest DSL that passes them.
3. Migrate scenario files without changing covered behavior.
4. Run affected TestBalloon tasks and compilation after migration.
5. Run the full build as the final verification.

The DSL contract tests cover fresh context creation, ordered shared-context execution, and step failure context. Migrated tests demonstrate the intended public API across the account, deposit, card, and loan modules.

## Completion Criteria

- All nine existing `*ScenarioTest.kt` files use `Scenario`, `Given`, `When`, and `Then`.
- Scenario files no longer directly register ordinary `test` blocks for scenario behavior.
- All affected test source sets compile.
- TestBalloon runs succeed for affected modules.
- The complete Gradle build succeeds.
- A headless compound-learning closeout records reusable lessons or explicitly states that none were found.
