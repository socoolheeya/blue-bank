# TestBalloon Scenario DSL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared Given/When/Then DSL over TestBalloon and migrate all nine application scenario-test files to it without changing their covered behavior.

**Architecture:** `testing:test-support` exposes one small `ScenarioScope<C>` and one `TestSuiteScope.Scenario` extension. Each TestBalloon test creates one scenario-specific context at execution time; all steps execute synchronously against that context and add step details to ordinary failures. Application scenario tests use focused private context classes to make setup, actions, results, and assertions explicit.

**Tech Stack:** Kotlin/JVM, Gradle, TestBalloon `1.0.0-K2.3.20`, Java toolchain 25, Spring Boot test-support module.

## Global Constraints

- The DSL lives in the `testing:test-support` main source set.
- Migrate all nine existing files named `*ScenarioTest.kt` under `app`.
- Preserve existing scenario behavior and coverage.
- Do not migrate non-scenario application, domain, slice, or integration tests.
- A scenario registers exactly one TestBalloon test and creates fresh context inside that test.
- Repeated Given/When/Then steps are permitted; phase order is not enforced.
- Step failures retain the original throwable as their cause and include the step kind and description.
- No production source set depends on `testing:test-support`.

---

## File Structure

- Create `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt`: public DSL and failure decoration.
- Create `testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/ScenarioDslTest.kt`: DSL contract scenarios.
- Modify `app/account/build.gradle.kts` and `app/loan/build.gradle.kts`: direct test-only dependency on shared support.
- Modify the three account, one deposit, two card, and three loan `*ScenarioTest.kt` files: contexts plus Given/When/Then scenarios.

### Task 1: Shared Scenario DSL Contract and Implementation

**Files:**
- Create: `testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/ScenarioDslTest.kt`
- Create: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt`

**Interfaces:**
- Consumes: `de.infix.testBalloon.framework.core.TestSuiteScope.test(String, () -> Unit)`.
- Produces: `ScenarioScope<C>`, `Given`, `When`, `Then`, and `TestSuiteScope.Scenario(name, context, body)`.

- [ ] **Step 1: Write the failing DSL contract test**

Create a suite that imports the not-yet-defined API and exercises shared ordered state and failure decoration:

```kotlin
package com.socoolheeya.bluebank.testing

import de.infix.testBalloon.framework.core.testSuite

private class RecordingContext {
    val events = mutableListOf<String>()
}

val scenarioDslContract by testSuite("Scenario DSL contract") {
    Scenario("steps share one context in declaration order", ::RecordingContext) {
        Given("an empty recording") { check(events.isEmpty()); events += "given" }
        When("an action is recorded") { events += "when" }
        Then("the ordered recording is visible") { check(events == listOf("given", "when")) }
    }

    Scenario("failure identifies its step", ::RecordingContext) {
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
```

- [ ] **Step 2: Run the test-support compilation and verify RED**

Run: `./gradlew :testing:test-support:compileTestKotlin --console=plain`

Expected: FAIL with unresolved references to `Scenario` and `ScenarioScope`.

- [ ] **Step 3: Implement the minimal shared DSL**

```kotlin
package com.socoolheeya.bluebank.testing

import de.infix.testBalloon.framework.core.TestSuiteScope

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

- [ ] **Step 4: Verify GREEN and the test-support TestBalloon suite**

Run: `./gradlew :testing:test-support:compileTestKotlin :testing:test-support:testBalloon --console=plain`

Expected: BUILD SUCCESSFUL and all test-support scenarios pass.

- [ ] **Step 5: Commit the shared DSL**

```bash
git add testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ScenarioDsl.kt testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/ScenarioDslTest.kt
git commit -m "test: add shared TestBalloon scenario DSL"
```

### Task 2: Account Scenario Migration

**Files:**
- Modify: `app/account/build.gradle.kts`
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/AccountServiceScenarioTest.kt`
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/BalanceServiceScenarioTest.kt`
- Modify: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/InterestServiceScenarioTest.kt`

**Interfaces:**
- Consumes: `Scenario(name, context, body)` and the three receiver steps from Task 1.
- Produces: account scenarios with one fresh fake/service context per registered TestBalloon test.

- [ ] **Step 1: Add the shared test-support dependency**

Add to `dependencies` in `app/account/build.gradle.kts`:

```kotlin
testImplementation(project(":testing:test-support"))
```

- [ ] **Step 2: Convert account lifecycle tests to named context state**

Import `Given`, `When`, `Then`, and `Scenario`; define focused contexts such as:

```kotlin
private class AccountScenarioContext {
    val fake = FakeAccountDataServices()
    val service = AccountService(fake.accountDataService)
    var result: AccountResult? = null
    var failure: Throwable? = null
}
```

Convert every current `test` registration, including each product-limit loop item, using this shape:

```kotlin
Scenario("create uses the $product default limit", ::AccountScenarioContext) {
    When("the customer creates a $product account") {
        result = service.createAccount(createRequest(product))
    }
    Then("the product defaults and account number are preserved") {
        check(result!!.accountNumber == "number-$product")
        check(fake.lastLimit!!.dailyTransferLimit == BigDecimal(expected.first))
        check(fake.lastLimit!!.singleTransferLimit == BigDecimal(expected.second))
        check(fake.lastLimit!!.monthlyDepositLimit == expected.third?.let(::BigDecimal))
    }
}
```

- [ ] **Step 3: Convert balance and interest scenarios**

Give each file its own private context holding its fake, service, inputs, result, and captured exception. Preserve exact assertions, including transfer descriptions, invalid amounts, date periods, and default interest months:

```kotlin
Scenario("transfer moves funds and applies default descriptions", ::BalanceScenarioContext) {
    Given("source and destination balances") {
        fake.balance(1, "100")
        fake.balance(2, "10")
    }
    When("funds are transferred") {
        transfer = service.transfer(1, 2, BigDecimal("30"), transactionId = "tx")
    }
    Then("both balances and descriptions are updated") {
        check(transfer!!.first.ledgerBalance == BigDecimal("70"))
        check(transfer!!.second.ledgerBalance == BigDecimal("40"))
        check(fake.lastWithdraw!![2] == "이체 출금")
        check(fake.lastDeposit!![2] == "이체 입금")
    }
}
```

- [ ] **Step 4: Compile and run account TestBalloon tests**

Run: `./gradlew :app:account:compileTestKotlin :app:account:testBalloon --console=plain`

Expected: BUILD SUCCESSFUL with all previous account scenario names passing.

- [ ] **Step 5: Commit the account migration**

```bash
git add app/account/build.gradle.kts app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/*ScenarioTest.kt
git commit -m "test: express account behavior with scenario DSL"
```

### Task 3: Deposit Scenario Migration

**Files:**
- Modify: `app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/service/DepositServiceScenarioTest.kt`

**Interfaces:**
- Consumes: shared DSL and the existing deposit test-support dependency.
- Produces: seven deposit service scenarios with explicit fixture, action, and observed-state properties.

- [ ] **Step 1: Introduce a deposit context**

Use one context that owns `FakeDepositDataService`, `FakeAccountServiceClient`, lazily constructed `DepositService`, created deposit ID, response, and snapshots:

```kotlin
private class DepositScenarioContext {
    val data = FakeDepositDataService()
    val accounts = FakeAccountServiceClient()
    val service by lazy { DepositService(data, accounts) }
    var depositId: Long? = null
    var failure: Throwable? = null

    fun create(product: String = "FIXED_DEPOSIT"): Long =
        requireNotNull(service.createDeposit(request(product = product)).id).also { depositId = it }
}
```

- [ ] **Step 2: Convert all deposit tests**

Retain each behavioral boundary and split the long invalid-state test into descriptive sequential steps within one scenario:

```kotlin
Scenario("invalid state transitions and monetary boundaries preserve state balance and commands", ::DepositScenarioContext) {
    Given("a pending free savings deposit") {
        accounts.addAccount(9, 7)
        create("FREE_SAVINGS")
    }
    When("pending-only invalid operations are attempted") {
        pendingDepositFailure = runCatching {
            service.deposit(depositId!!, 7, DepositDto.DepositRequest(BigDecimal.ONE))
        }.exceptionOrNull()
        pendingWithdrawFailure = runCatching {
            service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(BigDecimal.ONE))
        }.exceptionOrNull()
        pendingTerminateFailure = runCatching {
            service.terminateDeposit(depositId!!, 7)
        }.exceptionOrNull()
    }
    Then("the pending snapshot is unchanged") {
        check(pendingDepositFailure is IllegalArgumentException)
        check(pendingWithdrawFailure is IllegalArgumentException)
        check(pendingTerminateFailure is IllegalArgumentException)
        check(snapshot() == pendingSnapshot)
    }
    When("the deposit is activated and invalid monetary boundaries are attempted") {
        service.activateDeposit(depositId!!, 7)
        duplicateActivationFailure = runCatching { service.activateDeposit(depositId!!, 7) }.exceptionOrNull()
        invalidAmountFailures = listOf(BigDecimal.ZERO, BigDecimal("-1")).flatMap { amount ->
            listOf(
                runCatching { service.deposit(depositId!!, 7, DepositDto.DepositRequest(amount)) }.exceptionOrNull(),
                runCatching { service.earlyWithdraw(depositId!!, 7, DepositDto.WithdrawRequest(amount)) }.exceptionOrNull(),
            )
        }
    }
    Then("the active unfunded deposit remains unchanged") {
        check(duplicateActivationFailure is IllegalArgumentException)
        check(invalidAmountFailures.all { it is IllegalArgumentException })
        check(snapshot() == Triple("활성", BigDecimal.ZERO, 0))
    }
}
```

Add the referenced nullable failure properties, `invalidAmountFailures`, `pendingSnapshot`, and `snapshot()` to `DepositScenarioContext`. Add the final funded-overdraft and repeated-termination steps with the existing `BigDecimal("11")`, funded snapshot, and terminated snapshot assertions unchanged.

- [ ] **Step 3: Compile and run deposit TestBalloon tests**

Run: `./gradlew :app:deposit:compileTestKotlin :app:deposit:testBalloon --console=plain`

Expected: BUILD SUCCESSFUL and all deposit scenarios pass.

- [ ] **Step 4: Commit the deposit migration**

```bash
git add app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/service/DepositServiceScenarioTest.kt
git commit -m "test: express deposit behavior with scenario DSL"
```

### Task 4: Card Scenario Migration

**Files:**
- Modify: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardApplicationScenarioTest.kt`
- Modify: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardServiceScenarioTest.kt`

**Interfaces:**
- Consumes: shared DSL and the existing card test-support dependency.
- Produces: card application and lifecycle scenarios that keep all command-shape and ownership assertions.

- [ ] **Step 1: Convert card application scenarios**

Define `CardApplicationScenarioContext` with application/card fakes, service, response, and failure. Use separate Given/When/Then steps for request setup, submission/issuance, and command/result assertions:

```kotlin
Scenario("approved application issues a stable shaped card and marks application issued", ::CardApplicationScenarioContext) {
    Given("an approved card application") {
        applications.applications[1] = approved()
        before = LocalDate.now()
    }
    When("the approved application is issued") {
        issueResponse = service.issueCard(1)
        after = LocalDate.now()
    }
    Then("the card shape expiry and issued link are stable") {
        check(issueResponse!!.cardId == 100L)
        check(issueResponse!!.cardNumberMasked.matches(Regex("5234-\\*{4}-\\*{4}-\\d{4}")))
        check(cards.created.single().cardNumber.length == 16)
        check(applications.issued.single() == (1L to 100L))
    }
}
```

- [ ] **Step 2: Convert card lifecycle scenarios**

Define `CardScenarioContext` with fake/service and captured results. Preserve lookup lists, every lifecycle transition, all four wrong-owner failures, and all four missing-card failures.

- [ ] **Step 3: Compile and run card TestBalloon tests**

Run: `./gradlew :app:card:compileTestKotlin :app:card:testBalloon --console=plain`

Expected: BUILD SUCCESSFUL and all card scenarios pass.

- [ ] **Step 4: Commit the card migration**

```bash
git add app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/*ScenarioTest.kt
git commit -m "test: express card behavior with scenario DSL"
```

### Task 5: Loan Scenario Migration

**Files:**
- Modify: `app/loan/build.gradle.kts`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanServiceScenarioTest.kt`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanApplicationScenarioTest.kt`
- Modify: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/CreditScoreScenarioTest.kt`

**Interfaces:**
- Consumes: shared DSL from Task 1.
- Produces: loan query/execution/repayment, application boundary, approval-family, rejection, and stable credit-score scenarios.

- [ ] **Step 1: Add the shared test-support dependency**

Add to `dependencies` in `app/loan/build.gradle.kts`:

```kotlin
testImplementation(project(":testing:test-support"))
```

- [ ] **Step 2: Convert loan service and credit score scenarios**

Define contexts with fake data, account client, service, added loan, results, and failures. The credit-score test uses a minimal context:

```kotlin
private class CreditScoreScenarioContext {
    val service = CreditScoreService()
    var result: CreditScoreResult? = null
}

Scenario("credit score response is stable for every customer", ::CreditScoreScenarioContext) {
    When("a customer's credit score is requested") { result = service.getCreditScore(42) }
    Then("the stable NICE score is returned") {
        check(result?.score == 750)
        check(result?.grade == "3등급")
        check(result?.agency == "NICE")
    }
}
```

- [ ] **Step 3: Convert all loan application scenarios**

Define `LoanApplicationScenarioContext` with injectable account and credit-score fakes, data service, lazily constructed application service, results, commands, and failures. Preserve the exact account, income, DSR, minimum-score, lookup, product-family, amount/rate, rejection-reason, and missing-ID boundaries.

- [ ] **Step 4: Compile and run loan TestBalloon tests**

Run: `./gradlew :app:loan:compileTestKotlin :app:loan:testBalloon --console=plain`

Expected: BUILD SUCCESSFUL and all loan scenarios pass.

- [ ] **Step 5: Commit the loan migration**

```bash
git add app/loan/build.gradle.kts app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/*ScenarioTest.kt
git commit -m "test: express loan behavior with scenario DSL"
```

### Task 6: Cross-Module Review, Verification, and Learning Closeout

**Files:**
- Inspect: all modified files and `docs/superpowers/specs/2026-07-17-testballoon-scenario-dsl-design.md`
- Optionally create/update: the durable learning file selected by `/ce-compound mode:headless`.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verified build evidence and a reusable lesson record or explicit no-lesson result.

- [ ] **Step 1: Check migration completeness and formatting**

Run:

```bash
rg -L 'Scenario\(' $(rg --files app | rg 'ScenarioTest\.kt$')
rg -n '^\s*test\(' $(rg --files app | rg 'ScenarioTest\.kt$')
git diff --check HEAD~4
```

Expected: both searches produce no file/content output, and `git diff --check` reports no whitespace errors.

- [ ] **Step 2: Run all affected TestBalloon tasks together**

Run:

```bash
./gradlew :testing:test-support:testBalloon :app:account:testBalloon :app:deposit:testBalloon :app:card:testBalloon :app:loan:testBalloon --console=plain
```

Expected: BUILD SUCCESSFUL with zero failed tests.

- [ ] **Step 3: Run the complete compile/build verification**

Run: `./gradlew build --console=plain`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run Compound review closeout after fresh verification**

Run: `/ce-compound mode:headless TestBalloon shared Scenario DSL migration; capture DSL failure-decoration decisions, context isolation, module test-support dependency gaps, migration review signals, relevant files, and Gradle verification commands.`

Expected: durable learning is written, or the command explicitly reports that no reusable lesson exists.

- [ ] **Step 5: Inspect final repository state and report**

Run: `git status --short && git log -6 --oneline`

Expected: only pre-existing unrelated user changes and any documented compound-learning output remain uncommitted; implementation commits are visible.
