---
title: Shared Kotlin TestBalloon scenario DSL and reliable migration verification
date: 2026-07-17
category: developer-experience
module: testing/test-support and application scenario tests
problem_type: developer_experience
component: testing_framework
severity: medium
applies_when:
  - "A shared Kotlin test DSL is published from a support module main source set"
  - "The DSL public API exposes a third-party framework type"
  - "A repository-wide test migration needs structural and behavioral verification"
tags:
  - kotlin
  - testballoon
  - test-dsl
  - gradle-api
  - migration-verification
---

# Shared Kotlin TestBalloon scenario DSL and reliable migration verification

## Context

Nine application `*ScenarioTest.kt` files used TestBalloon directly but lacked a shared executable-specification shape. A common `Scenario`/`Given`/`When`/`Then` DSL was added to `testing:test-support`, then account, deposit, card, and loan scenarios were migrated without changing their behavioral boundaries.

Two assumptions failed during the work:

- TestBalloon core was declared as `testImplementation`, but the new DSL lives in `src/main` and exposes `TestSuiteScope` publicly. Main compilation therefore could not resolve TestBalloon types.
- The first completeness check treated `rg -L` like GNU grep's “files without a match.” In ripgrep, `-L` means `--follow`, so the command did not prove migration completeness.

A semantic review also caught a Given description that contradicted its fixture ownership even though the assertions passed. Compilation alone cannot validate executable-spec prose.

## Guidance

Keep the DSL thin and create context inside the registered TestBalloon test:

```kotlin
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

fun <C> TestSuiteScope.Scenario(
    name: String,
    context: () -> C,
    body: ScenarioScope<C>.() -> Unit,
) {
    test(name) {
        ScenarioScope(context()).body()
    }
}
```

This gives every execution fresh fakes and services while all steps in one scenario share ordered state. Catch ordinary exceptions and `AssertionError` to attach `Given`/`When`/`Then` context while retaining the original failure as `cause`; do not catch every `Throwable`.

Because the main-source public API exposes `TestSuiteScope`, export TestBalloon core from the support module:

```kotlin
api("de.infix.testBalloon:testBalloon-framework-core:1.0.0-K2.3.20")
```

Consumer applications still depend on the support module only from tests:

```kotlin
testImplementation(project(":testing:test-support"))
```

Use a private context per suite to own fixtures, the system under test, inputs, responses, failures, commands, and snapshots. Do not enforce a rigid phase type-state: repeated setup or assertion steps are useful for long state-transition scenarios.

For a bulk migration, verify both structure and behavior:

- Every `*ScenarioTest.kt` contains `Scenario(`.
- No migrated file contains a raw registration matching `^\s*test\(`.
- Every Scenario receives a context factory.
- Scenario names, loop-generated cases, exact strings, boundary values, ownership identities, command shapes, and state snapshots remain intact.
- Each module compiles and runs its TestBalloon suite, all affected suites pass together, and the full build passes.
- In a dirty shared worktree, inspect staged paths and committed paths so unrelated user changes are not included.

Use explicit per-file ripgrep checks rather than relying on a remembered flag:

```bash
missing=0
count=0
while IFS= read -r -d '' file; do
    count=$((count + 1))
    if ! rg -q 'Scenario\(' "$file"; then
        printf '%s\n' "$file"
        missing=$((missing + 1))
    fi
done < <(rg --files -0 -g '*ScenarioTest.kt' app)

printf 'SCENARIO_TEST_FILES=%s\nMISSING_SCENARIO_FILES=%s\n' "$count" "$missing"
test "$missing" -eq 0
```

For `rg -n '^\s*test\(' -g '*ScenarioTest.kt' app`, exit code 1 with no output is the expected result.

## Why This Matters

Source-set placement determines dependency scope even if downstream consumers use the API only in tests. A third-party type exposed by a main-source public signature must be present during main compilation and transitively visible to consumers.

Constructing context inside `test(name) { ... }` prevents suite-declaration-time fixture sharing. This is important for mutable fakes, captured commands, loop-generated scenarios, and possible concurrent execution.

Step-level error decoration makes reports identify the business step without discarding the underlying exception. Structural search prevents missed files, while semantic review prevents assertion, boundary, and narrative drift that compilation cannot detect.

## When to Apply

- Multiple Kotlin TestBalloon suites need a consistent Given/When/Then style.
- Ordered steps share mutable state but every scenario needs a fresh fixture.
- A reusable test-support main API exposes testing-framework types.
- Existing tests are migrated mechanically and must retain exact behavioral coverage.
- Completeness checks span many files and need shell-safe, tool-correct evidence.

Do not force this DSL onto ordinary unit, slice, integration, or application-context tests solely for uniformity.

## Examples

The contract suite should verify shared ordered state and failure causes:

```kotlin
Scenario("steps share one context in declaration order", ::RecordingContext) {
    Given("an empty recording") {
        check(events.isEmpty())
        events += "given"
    }
    When("an action is recorded") {
        events += "when"
    }
    Then("the ordered recording is visible") {
        check(events == listOf("given", "when"))
    }
}
```

Fresh verification commands used for this migration:

```bash
./gradlew :testing:test-support:compileTestKotlin :testing:test-support:testBalloon --console=plain
./gradlew :app:account:compileTestKotlin :app:account:testBalloon --console=plain
./gradlew :app:deposit:compileTestKotlin :app:deposit:testBalloon --console=plain
./gradlew :app:card:compileTestKotlin :app:card:testBalloon --console=plain
./gradlew :app:loan:compileTestKotlin :app:loan:testBalloon --console=plain
./gradlew :testing:test-support:testBalloon :app:account:testBalloon :app:deposit:testBalloon :app:card:testBalloon :app:loan:testBalloon --console=plain
git diff --check f925282..HEAD
./gradlew build --console=plain
```

## Related

- [TestBalloon testing standard](../../testing/testballoon-testing-standard.md)
- [TestBalloon default test task silently skips tests](../test-failures/testballoon-default-test-task-silently-skips-tests-2026-07-17.md)
- [Scenario DSL design](../../superpowers/specs/2026-07-17-testballoon-scenario-dsl-design.md)
- [Scenario DSL implementation plan](../../superpowers/plans/2026-07-17-testballoon-scenario-dsl.md)
