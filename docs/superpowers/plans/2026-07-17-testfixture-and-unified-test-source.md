# TestFixture and Unified Test Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make scenario and MVC tests use TestBalloon-managed fixtures and run all application tests from `src/test` through the standard `test` task.

**Architecture:** Add a fixture-receiver `Scenario` overload on TestBalloon's fixture scope, then wrap each mutable scenario context with `testFixture { ... } asContextForEach`. Move MVC slice files into `src/test`, use the same per-test fixture lifecycle there, and simplify Gradle to two source sets: `test` for application tests and `integrationTest` for infrastructure tests.

**Tech Stack:** Kotlin 2.3.21, TestBalloon 1.0.0-K2.3.20, Gradle 9.6.1, JUnit Platform 1.13.4, Spring Boot 4.1.0, Spring MockMvc

## Global Constraints

- Mutable scenario and MVC fixtures use `asContextForEach`; never share them with `asContextForAll`.
- `AutoCloseable` cleanup remains owned by TestBalloon.
- Controller tests move to `src/test`; `integrationTest` remains separate.
- Every verification layer expected to contain tests must report a nonzero XML test count with zero failures, errors, and unintended skips.
- The final `./gradlew build --console=plain` must compile and succeed.
- After the Superpowers execution-review loop and fresh verification, run `/ce-compound mode:headless` before claiming completion.

---

## File Structure

- `testing/test-support/.../ScenarioDsl.kt`: owns Given/When/Then execution and fixture-scoped scenario registration.
- `testing/test-support/.../ScenarioDslTest.kt`: proves fixture isolation, execution order, cleanup, and failure decoration.
- Nine `app/*/src/test/.../*ScenarioTest.kt` files: declare domain-specific per-test contexts visibly with TestBalloon fixtures.
- Four controller test files: move unchanged packages from `src/sliceTest` to `src/test`, then let TestBalloon construct and close one MVC fixture per test.
- Root and four application `build.gradle.kts` files: remove the slice source set/task and keep a compatible combined TestBalloon/Jupiter runtime for `test`.
- `docs/testing/*.md`: describe the unified test source and task lifecycle.

### Task 1: Fixture-Scoped Scenario DSL

**Files:**
- Modify: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt`
- Modify: `testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/ScenarioDslTest.kt`

**Interfaces:**
- Consumes: `TestFixture.Scope<suspend C.(Test.ExecutionScope) -> Unit>` and `testFixture { C() } asContextForEach { ... }`.
- Produces: `fun <C : Any> TestFixture.Scope<suspend C.(Test.ExecutionScope) -> Unit>.Scenario(name: String, body: ScenarioScope<C>.() -> Unit)`.

- [ ] **Step 1: Add failing fixture contract tests**

Add the `testFixture` import, then replace factory-only lifecycle coverage with a visible fixture scope. Use a recording context and prove that each scenario starts with fresh state:

```kotlin
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
```

- [ ] **Step 2: Run the contract test and confirm compilation fails**

Run: `./gradlew :testing:test-support:test --tests '*scenarioDslContract*' --console=plain`

Expected: `compileTestKotlin` fails because the fixture-scope `Scenario(name, body)` overload does not exist.

- [ ] **Step 3: Implement the fixture-scope overload**

Keep `ScenarioScope` unchanged and add these imports and extension:

```kotlin
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestFixture

fun <C : Any> TestFixture.Scope<suspend C.(Test.ExecutionScope) -> Unit>.Scenario(
    name: String,
    body: ScenarioScope<C>.() -> Unit,
) {
    test(name) {
        ScenarioScope(this).body()
    }
}
```

Retain the existing `TestSuiteScope.Scenario(name, context, body)` overload until all callers migrate; remove it only in Task 2 after `rg` proves it has no callers.

- [ ] **Step 4: Run the fixture contract test**

Run: `./gradlew :testing:test-support:test --tests '*scenarioDslContract*' --console=plain`

Expected: PASS, and its XML reports three tests, zero failures/errors/skips. AutoCloseable cleanup remains covered by `TestSupportScenario.kt` and TestBalloon's fixture contract.

- [ ] **Step 5: Commit the DSL contract**

```bash
git add testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/ScenarioDslTest.kt
git commit -m "test: support fixture-scoped scenarios"
```

### Task 2: Application Scenario Fixture Migration

**Files:**
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/AccountServiceScenarioTest.kt`
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/BalanceServiceScenarioTest.kt`
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/InterestServiceScenarioTest.kt`
- Modify: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardApplicationScenarioTest.kt`
- Modify: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardServiceScenarioTest.kt`
- Modify: `app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/service/DepositServiceScenarioTest.kt`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/CreditScoreScenarioTest.kt`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanApplicationScenarioTest.kt`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanServiceScenarioTest.kt`
- Modify: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt`

**Interfaces:**
- Consumes: fixture-scope `Scenario(name, body)` from Task 1.
- Produces: nine suites whose scenario contexts are created once per test and visible at suite declaration.

- [ ] **Step 1: Convert one account suite and compile it**

Add `import de.infix.testBalloon.framework.core.testFixture`, wrap all scenarios, and remove the context argument:

```kotlin
val accountServiceScenarios by testSuite("Account lifecycle") {
    testFixture { AccountScenarioContext() } asContextForEach {
        Scenario("lookups return accounts by id and number") {
            // existing Given/When/Then body unchanged
        }
        // every remaining scenario and loop-generated Scenario stays inside this scope
    }
}
```

Apply the same mechanical replacement to every `Scenario(..., ::AccountScenarioContext)` in the file, including loop-generated names.

Run: `./gradlew :app:account:compileTestKotlin --console=plain`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Convert the remaining eight suites**

For each file, import `testFixture`, wrap its existing scenarios with the listed context, and change `Scenario(name, ::Context) {` to `Scenario(name) {`:

```kotlin
testFixture { BalanceScenarioContext() } asContextForEach { /* all balance scenarios */ }
testFixture { InterestScenarioContext() } asContextForEach { /* all interest scenarios */ }
testFixture { CardApplicationScenarioContext() } asContextForEach { /* all card application scenarios */ }
testFixture { CardScenarioContext() } asContextForEach { /* all card scenarios */ }
testFixture { DepositScenarioContext() } asContextForEach { /* all deposit scenarios */ }
testFixture { CreditScoreScenarioContext() } asContextForEach { /* all score scenarios */ }
testFixture { LoanApplicationScenarioContext() } asContextForEach { /* all application scenarios */ }
testFixture { LoanServiceScenarioContext() } asContextForEach { /* all loan scenarios */ }
```

Do not alter scenario names, loop values, commands, assertions, or fake behavior.

- [ ] **Step 3: Remove the obsolete factory overload**

Run: `rg -n 'Scenario\([^\n]*::|context\s*=' app testing --glob '*.kt'`

Expected: no application or test-support caller uses the factory signature. Delete `TestSuiteScope` import and this overload from `ScenarioDsl.kt`:

```kotlin
fun <C> TestSuiteScope.Scenario(name: String, context: () -> C, body: ScenarioScope<C>.() -> Unit)
```

- [ ] **Step 4: Run every scenario-owning module**

Run: `./gradlew :app:account:test :app:card:test :app:deposit:test :app:loan:test --console=plain`

Expected: BUILD SUCCESSFUL; scenario suite XML totals remain nonzero with zero failures/errors/skips.

- [ ] **Step 5: Commit the migration**

```bash
git add testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt app/account/src/test app/card/src/test app/deposit/src/test app/loan/src/test
git commit -m "test: manage scenario contexts with fixtures"
```

### Task 3: Move MVC Tests and Adopt Fixtures

**Files:**
- Move: `app/account/src/sliceTest/kotlin/com/socoolheeya/bluebank/account/controller/AccountControllerSliceTest.kt` to `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/controller/AccountControllerSliceTest.kt`
- Move: `app/card/src/sliceTest/kotlin/com/socoolheeya/bluebank/card/controller/CardControllerSliceTest.kt` to `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/controller/CardControllerSliceTest.kt`
- Move: `app/deposit/src/sliceTest/kotlin/com/socoolheeya/bluebank/deposit/controller/DepositControllerSliceTest.kt` to `app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/controller/DepositControllerSliceTest.kt`
- Move: `app/loan/src/sliceTest/kotlin/com/socoolheeya/bluebank/loan/controller/LoanControllerSliceTest.kt` to `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/controller/LoanControllerSliceTest.kt`

**Interfaces:**
- Consumes: TestBalloon `testFixture` and `AutoCloseable` MVC fixture classes already present in each file.
- Produces: four controller suites under `src/test`, each with one fresh MVC application context per test.

- [ ] **Step 1: Move all four files without changing packages**

Use filesystem moves so Git records renames. Preserve each `com.socoolheeya.bluebank.<domain>.controller` package and all assertions.

- [ ] **Step 2: Replace manual `.use` lifecycle with TestBalloon fixture scopes**

In each controller suite add `testFixture` and wrap tests:

```kotlin
val accountControllerSlices by testSuite("Account controller slices") {
    testFixture { AccountMvcFixture() } asContextForEach {
        test("GET api accounts binds the external route") {
            mvc.get("/api/accounts").andExpect {
                status { isOk() }
                content { string("test") }
            }
        }
        // remaining tests use mvc/accountService/balanceService directly
    }
}
```

Apply the corresponding `CardMvcFixture`, `DepositMvcFixture`, and `LoanMvcFixture` wrapper. Remove every `Fixture().use { ... }`, replace `it.mvc` and `it.<service>` with receiver properties, and preserve request/assertion bodies exactly. TestBalloon calls `close()` after every test.

- [ ] **Step 3: Confirm source layout and compile**

Run: `find app -path '*/src/sliceTest/*' -type f`

Expected: no output.

Run: `./gradlew :app:account:compileTestKotlin :app:card:compileTestKotlin :app:deposit:compileTestKotlin :app:loan:compileTestKotlin --console=plain`

Expected: BUILD SUCCESSFUL because the moved files already consume ordinary `testImplementation` dependencies and main output. Fix Kotlin receiver or import failures before proceeding.

- [ ] **Step 4: Commit the source migration**

```bash
git add app/account/src app/card/src app/deposit/src app/loan/src
git commit -m "test: move controller tests into test source"
```

### Task 4: Unify Gradle Test Runtime and Lifecycle

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/account/build.gradle.kts`
- Modify: `app/card/build.gradle.kts`
- Modify: `app/deposit/build.gradle.kts`
- Modify: `app/loan/build.gradle.kts`

**Interfaces:**
- Consumes: controller files now compiled from `sourceSets["test"]`.
- Produces: `test` runs TestBalloon and Spring/Jupiter tests; `check` depends on `integrationTest`; no `sliceTest` task exists.

- [ ] **Step 1: Prove the pre-change unified task is incomplete**

Run: `./gradlew :app:account:test --tests '*accountControllerSlices*' --console=plain`

Expected: FAIL from missing runtime/test discovery or report zero matching controller tests, demonstrating that the existing isolated runtime cannot yet validate the moved suite.

- [ ] **Step 2: Remove the root slice source set and task**

Delete `val sliceTest = sourceSets.create("sliceTest")`, its configuration inheritance, and the `sliceTestTask` registration. Change integration ordering and `check` wiring to:

```kotlin
val integrationTest = sourceSets.create("integrationTest")

configurations[integrationTest.implementationConfigurationName]
    .extendsFrom(configurations["testImplementation"])
configurations[integrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations["testRuntimeOnly"])

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests with an isolated JUnit Platform 1.13 runtime"
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = files(integrationTest.output, testBalloonRuntime)
    useJUnitPlatform()
    mustRunAfter(tasks.named("test"))
}

tasks.named("check") { dependsOn(integrationTestTask) }
```

- [ ] **Step 3: Make `test` capable of discovering both engines**

Use the ordinary `testRuntimeClasspath` plus the explicitly aligned JUnit Platform artifacts for `test`; keep `testBalloon` as the focused isolated task:

```kotlin
val combinedTestRuntime = files(
    sourceSets["main"].output,
    sourceSets["test"].output,
    configurations.named("testRuntimeClasspath"),
    rootProject.configurations[testBalloonPlatform.name],
)
tasks.named<Test>("test") {
    classpath = combinedTestRuntime
    useJUnitPlatform()
}
```

If duplicate older platform jars remain, use `dependencyInsight` and the existing forced 1.13.4 constraints to resolve them; do not restore the filter that removes all `junit-jupiter-*` files because Spring/JUnit tests need the Jupiter engine.

- [ ] **Step 4: Remove module slice configurations**

Delete these obsolete blocks:

```kotlin
add("sliceTestImplementation", sourceSets["main"].output)
sourceSets.named("sliceTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
```

Keep each module's existing `testImplementation` dependencies unchanged: account and loan retain their explicit Mockito, Spring Test, and JsonPath dependencies; card and deposit retain `spring-boot-starter-test`.

- [ ] **Step 5: Run unified module tests and inspect XML**

Run: `./gradlew :app:account:test :app:card:test :app:deposit:test :app:loan:test --console=plain`

Expected: BUILD SUCCESSFUL.

Run: `rg -n 'tests="0"|failures="[1-9]|errors="[1-9]' app/*/build/test-results/test/TEST-*.xml`

Expected: no output. Separately confirm `accountControllerSlices`, `cardControllerSlices`, `depositControllerSlices`, and `loanControllerSlices` identities exist in the XML.

- [ ] **Step 6: Verify the removed task and retained integration task**

Run: `./gradlew tasks --all --console=plain | rg '(^|:)sliceTest|integrationTest'`

Expected: integration tasks are listed and no slice task is listed.

- [ ] **Step 7: Commit Gradle unification**

```bash
git add build.gradle.kts app/account/build.gradle.kts app/card/build.gradle.kts app/deposit/build.gradle.kts app/loan/build.gradle.kts
git commit -m "build: unify application test source set"
```

### Task 5: Update Test Documentation

**Files:**
- Modify: `docs/testing/testballoon-testing-standard.md`
- Modify: `docs/testing/layered-test-guide.md`

**Interfaces:**
- Consumes: final task names and source layout from Task 4.
- Produces: contributor guidance matching executable Gradle behavior.

- [ ] **Step 1: Update the testing standard**

State that both service scenarios and MVC controller tests live in `src/test` and run through `test`; describe `testFixture { Context() } asContextForEach` as the default mutable-test-context form. Replace the lifecycle bullets and commands with:

```markdown
- `test`: TestBalloon domain/service scenarios and Spring MVC controller tests under `src/test`.
- `integrationTest`: repository, transaction, and full Spring context tests under `src/integrationTest`.
- `check`: runs `test` and `integrationTest`.

./gradlew :app:account:test --tests '*accountServiceScenarios*' --console=plain
./gradlew :app:account:test --tests '*accountControllerSlices*' --console=plain
./gradlew :data:account-data:integrationTest --tests '*accountDataIntegration*' --console=plain
```

- [ ] **Step 2: Update the layered guide**

Change the controller row to `app/<domain>/src/test` and `./gradlew :app:<domain>:test`. Replace validation sequences containing `sliceTest` with `test integrationTest build`. Keep controller suite names and API coverage mappings unchanged, and describe the historical 15 controller tests as part of the unified application `test` total rather than a current `sliceTest` result.

- [ ] **Step 3: Prove stale guidance is gone**

Run: `rg -n 'src/sliceTest|:sliceTest|`sliceTest`|sliceTestImplementation|sourceSets\.named\("sliceTest"' . --glob '!**/build/**' --glob '!docs/superpowers/**'`

Expected: no output.

- [ ] **Step 4: Commit documentation**

```bash
git add docs/testing/testballoon-testing-standard.md docs/testing/layered-test-guide.md
git commit -m "docs: describe unified application tests"
```

### Task 6: Full Verification, Review, and Compound Closeout

**Files:**
- Verify: all files changed in Tasks 1–5
- Create when reusable lessons exist: `docs/solutions/<category>/<descriptive-name>.md` through `/ce-compound`

**Interfaces:**
- Consumes: completed fixture and source-set migration.
- Produces: compile/test evidence, review resolution, and durable lessons before completion.

- [ ] **Step 1: Run structural checks**

Run:

```bash
rg -n 'testFixture\s*\{' app/*/src/test testing/test-support/src/test --glob '*.kt'
find app -path '*/src/sliceTest/*' -type f
rg -n 'sliceTest' build.gradle.kts app/*/build.gradle.kts docs/testing --glob '*.kts' --glob '*.md'
git diff --check
```

Expected: fixtures appear in all nine scenario suites and four controller suites; both slice searches produce no output; `git diff --check` succeeds.

- [ ] **Step 2: Run focused and integration verification**

Run:

```bash
./gradlew :testing:test-support:test :app:account:test :app:card:test :app:deposit:test :app:loan:test --console=plain
./gradlew integrationTest --console=plain
```

Expected: BUILD SUCCESSFUL with nonzero XML counts for every owning module and zero failures/errors/unintended skips.

- [ ] **Step 3: Run the required full compile/test lifecycle**

Run: `./gradlew build --console=plain`

Expected: BUILD SUCCESSFUL. Inspect all generated test XML and reject any expected suite with zero tests.

- [ ] **Step 4: Review the final diff and resolve findings**

Run: `git diff HEAD~5 --stat && git diff HEAD~5 --check`

Review fixture isolation, cleanup, dual-engine discovery, source moves, stale task references, and accidental behavioral changes. Fix every confirmed issue and rerun the smallest affected test followed by `./gradlew build --console=plain`.

- [ ] **Step 5: Run Compound closeout after fresh verification**

Run:

```text
/ce-compound mode:headless TestBalloon testFixture and unified src/test migration; capture fixture isolation and cleanup, fixture-scope DSL typing, TestBalloon plus Jupiter runtime discovery, removal of sliceTest source sets, review signals, relevant files, XML count checks, and Gradle verification commands.
```

If there is no reusable lesson, explicitly record that result. Do not skip this step.

- [ ] **Step 6: Commit closeout changes**

```bash
git add docs/solutions docs/testing testing/test-support app build.gradle.kts
git commit -m "docs: capture unified test fixture lessons"
```
