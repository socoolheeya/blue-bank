# All-Domain Layered Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add TestBalloon scenario, Spring MVC slice, and Spring/JPA integration coverage for every public account, card, deposit, and loan operation and its meaningful rejection boundaries.

**Architecture:** Fast stateful-fake service scenarios remain in each app module’s `src/test`; controller HTTP contracts live in `src/sliceTest`; real data-service and repository behavior lives in the matching data module’s `src/integrationTest`. Root Gradle conventions give all three layers the same isolated JUnit Platform 1.13/TestBalloon runtime and attach slice and integration execution to `check`.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.0, Gradle 9.6.1, TestBalloon 1.0.0-K2.3.20, JUnit Platform 1.13.4, Spring MockMvc, H2

## Global Constraints

- All test declarations use top-level delegated TestBalloon `testSuite`; do not add JUnit `@Test` or lifecycle annotations.
- Cover every public controller endpoint and service method at least once, plus meaningful validation, ownership, missing-resource, state-transition, and monetary boundaries.
- Do not implement unfinished remote money movement or third-party integrations solely for testing.
- Disable Eureka and external network access in Spring fixtures.
- Create mutable fakes per test and close every Spring context deterministically.
- Use exact `BigDecimal` comparisons and stable invariants for time/random-derived identifiers.
- Keep existing user changes in `build.gradle.kts` and `docs/solutions/` intact.

---

### Task 1: Layered Gradle test infrastructure

**Files:**
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `testing/test-support/build.gradle.kts`
- Create: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/SpringContextFixture.kt`
- Create: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/ExceptionAssertions.kt`
- Create: `testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/TestSupportScenario.kt`

**Interfaces:**
- Produces: `SpringContextFixture.start(vararg sources: Class<*>, properties: Map<String, Any?> = emptyMap()): ConfigurableApplicationContext`
- Produces: `suspend inline fun <reified T : Throwable> expectThrows(noinline block: suspend () -> Unit): T`
- Produces: per-subproject `sliceTest` and `integrationTest` source sets and `Test` tasks using `testBalloonRuntime`

- [ ] **Step 1: Write the failing support contract suite**

Create `TestSupportScenario.kt` with TestBalloon tests proving `expectThrows<IllegalArgumentException>` returns the thrown instance and rejects the absence of an exception, and proving a context returned by `SpringContextFixture.start` can be closed.

```kotlin
val testSupportScenario by testSuite {
    test("expectThrows returns the expected failure") {
        val failure = expectThrows<IllegalArgumentException> { error("wrong type") }
        check(failure.message == "wrong type")
    }
    test("Spring context fixture owns a closeable context") {
        val context = SpringContextFixture.start(EmptyTestConfiguration::class.java)
        check(context.isActive)
        context.close()
        check(!context.isActive)
    }
}
```

- [ ] **Step 2: Run the support test and verify RED**

Run: `./gradlew :testing:test-support:test --console=plain`

Expected: FAIL because the module and helper APIs do not exist.

- [ ] **Step 3: Implement the module and helpers**

Implement context startup with `SpringApplicationBuilder(*sources).web(WebApplicationType.NONE)`, always adding `eureka.client.enabled=false` and `spring.cloud.discovery.enabled=false`. Implement `expectThrows` by catching `Throwable`, checking `T`, and failing when nothing is thrown.

Add `sliceTest` and `integrationTest` source sets whose compile/runtime configurations extend the normal test configurations. Configure both Test tasks with their source-set outputs, `testBalloonRuntime`, and `useJUnitPlatform()`. Make `check` depend on `sliceTest` and `integrationTest`, with `mustRunAfter(test)` and `integrationTest.mustRunAfter(sliceTest)`.

- [ ] **Step 4: Verify GREEN and execution order**

Run: `./gradlew :testing:test-support:test tasks --all --console=plain`

Expected: support tests PASS; `sliceTest` and `integrationTest` appear for Kotlin JVM projects.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts settings.gradle.kts testing/test-support
git commit -m "test: add layered TestBalloon infrastructure"
```

### Task 2: Account scenario, slice, and integration coverage

**Files:**
- Create: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/testing/FakeAccountDataServices.kt`
- Create: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/AccountServiceScenarioTest.kt`
- Create: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/BalanceServiceScenarioTest.kt`
- Create: `app/account/src/test/kotlin/com/socoolheeya/bluebank/account/service/InterestServiceScenarioTest.kt`
- Create: `app/account/src/sliceTest/kotlin/com/socoolheeya/bluebank/account/controller/AccountControllerSliceTest.kt`
- Create: `data/account-data/src/integrationTest/kotlin/com/socoolheeya/bluebank/account/data/AccountDataIntegrationTest.kt`
- Create: `data/account-data/src/integrationTest/resources/application-integration.yml`
- Modify: `app/account/build.gradle.kts`
- Modify: `data/account-data/build.gradle.kts`

**Interfaces:**
- Consumes: TestBalloon `testSuite`, `expectThrows`, Spring MockMvc, isolated `sliceTest`/`integrationTest`
- Produces: stateful fakes for `AccountDataService`, `BalanceDataService`, and `InterestDataService` covering their invoked API surface

- [ ] **Step 1: Add RED account service scenarios**

Write scenarios for all public methods: create with each product’s default limit; lookup by ID/number and missing failures; customer listing; modify; close/freeze/activate; balance lookup and missing failure; deposit/withdraw; transfer success, zero/negative amount, same-account rejection; all six interest operations and period arguments.

Use nested suites named `Account lifecycle`, `Balance rules`, and `Interest calculations`; each test constructs fresh fake state inside its body.

- [ ] **Step 2: Verify scenario RED**

Run: `./gradlew :app:account:test --tests '*Scenario*' --console=plain`

Expected: FAIL on missing fake implementations or uncovered mapping assertions.

- [ ] **Step 3: Complete minimal stateful account fakes and verify GREEN**

Implement only methods invoked by the three services. Record last commands and mutate maps of account results, balances, and interest payments so scenarios assert both returned values and state changes.

Run: `./gradlew :app:account:test --console=plain`

Expected: all account scenario and existing tests PASS.

- [ ] **Step 4: Add RED account controller slices**

Cover `GET /api/accounts`, both internal lookups, customer listing, balance lookup, and validation for active, inactive, and missing accounts through MockMvc. Assert route, binding, HTTP status, JSON fields, and Korean validation messages.

Run: `./gradlew :app:account:sliceTest --console=plain`

Expected: FAIL until the focused MVC context, fake service beans, and JSON support are wired.

- [ ] **Step 5: Wire the focused account MVC fixture and verify GREEN**

Start a web application context containing `ExternalAccountController`, `InternalAccountController`, Jackson MVC infrastructure, and test fake service beans. Build MockMvc from the context and close it in fixture teardown.

Run: `./gradlew :app:account:sliceTest --console=plain`

Expected: every account endpoint test PASS.

- [ ] **Step 6: Add account data integration coverage and verify GREEN**

Use the real `AccountRepository`, holder/balance/ledger/limit/interest repositories, and data services with H2. Cover persistence and queries by number, customer, product, and parent; lifecycle changes; balance and ledger updates; rollback; interest history and period filtering.

Run: `./gradlew :data:account-data:integrationTest --console=plain`

Expected: all integration cases PASS with zero skips.

- [ ] **Step 7: Commit**

```bash
git add app/account data/account-data
git commit -m "test: cover account domain layers"
```

### Task 3: Card scenario, slice, and integration coverage

**Files:**
- Create: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/testing/FakeCardDataServices.kt`
- Create: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardServiceScenarioTest.kt`
- Create: `app/card/src/test/kotlin/com/socoolheeya/bluebank/card/service/CardApplicationScenarioTest.kt`
- Create: `app/card/src/sliceTest/kotlin/com/socoolheeya/bluebank/card/controller/CardControllerSliceTest.kt`
- Create: `data/card-data/src/integrationTest/kotlin/com/socoolheeya/bluebank/card/data/CardDataIntegrationTest.kt`
- Create: `data/card-data/src/integrationTest/resources/application-integration.yml`
- Modify: `app/card/build.gradle.kts`
- Modify: `data/card-data/build.gradle.kts`

**Interfaces:**
- Produces: stateful fakes for `CardDataService` and `CardApplicationDataService`
- Covers: all public methods of `CardService`, `CardApplicationService`, `CardController`, and `CardApplicationController`

- [ ] **Step 1: Write and run RED card scenarios**

Cover card lookup/list/active list; activation; usage enable/disable; lost and terminate transitions; ownership failures; application submission mapping; missing meeting-account rejection; application lookup/list; missing lookup; issue workflow and stable generated-card invariants.

Run: `./gradlew :app:card:test --tests '*Scenario*' --console=plain`

Expected: FAIL until stateful fakes supply the complete invoked API.

- [ ] **Step 2: Implement card fakes and verify GREEN**

Store application and card results by ID, record commands, and implement state transitions without duplicating production validation.

Run: `./gradlew :app:card:test --console=plain`

Expected: card scenario and existing smoke tests PASS.

- [ ] **Step 3: Add and verify card controller slices**

Exercise every endpoint in both controllers through MockMvc, asserting path/query/body binding, response status, list shape, and state fields. Include missing application/card and invalid request cases supported by current exception handling.

Run: `./gradlew :app:card:sliceTest --console=plain`

Expected: non-zero tests, zero failures and skips.

- [ ] **Step 4: Add and verify card data integration tests**

With real H2 repositories/data services cover card and application persistence, customer/active queries, application status changes, issuance and card state transitions, transaction/statement/benefit/cashback persistence paths invoked by services.

Run: `./gradlew :data:card-data:integrationTest --console=plain`

Expected: all card integration cases PASS.

- [ ] **Step 5: Commit**

```bash
git add app/card data/card-data
git commit -m "test: cover card domain layers"
```

### Task 4: Deposit scenario, slice, and integration coverage

**Files:**
- Create: `app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/testing/FakeDepositDependencies.kt`
- Create: `app/deposit/src/test/kotlin/com/socoolheeya/bluebank/deposit/service/DepositServiceScenarioTest.kt`
- Create: `app/deposit/src/sliceTest/kotlin/com/socoolheeya/bluebank/deposit/controller/DepositControllerSliceTest.kt`
- Create: `data/deposit-data/src/integrationTest/kotlin/com/socoolheeya/bluebank/deposit/data/DepositDataIntegrationTest.kt`
- Create: `data/deposit-data/src/integrationTest/resources/application-integration.yml`
- Modify: `app/deposit/build.gradle.kts`
- Modify: `data/deposit-data/build.gradle.kts`

**Interfaces:**
- Produces: stateful fake `DepositDataService` and fake `AccountServiceClient`
- Covers: all public `DepositService` and `DepositController` operations

- [ ] **Step 1: Write and run RED deposit scenarios**

Cover valid creation and product/period mapping; invalid account; wrong account owner; deposit-number prefix/uniqueness invariants; activate/contribute/early-withdraw/terminate success and ownership rejection; lookup and customer listing.

Run: `./gradlew :app:deposit:test --tests '*Scenario*' --console=plain`

Expected: FAIL until fake account and deposit dependencies model state.

- [ ] **Step 2: Implement deposit fakes and verify GREEN**

Store deposit results and commands, mutate balances/status for operations, and expose deterministic account validation and ownership responses.

Run: `./gradlew :app:deposit:test --console=plain`

Expected: all deposit scenario and existing tests PASS.

- [ ] **Step 3: Add and verify deposit controller slices**

Exercise every controller endpoint through MockMvc, including JSON `BigDecimal` and date binding, customer query parameters, response status/shape, invalid account, ownership, and missing-deposit behavior.

Run: `./gradlew :app:deposit:sliceTest --console=plain`

Expected: non-zero tests, zero failures and skips.

- [ ] **Step 4: Add and verify deposit data integration tests**

Use real H2 repositories/data services for create/get/customer queries, activate/terminate, contribution and withdrawal transaction records, current balance, and maturity/state persistence.

Run: `./gradlew :data:deposit-data:integrationTest --console=plain`

Expected: all deposit integration cases PASS.

- [ ] **Step 5: Commit**

```bash
git add app/deposit data/deposit-data
git commit -m "test: cover deposit domain layers"
```

### Task 5: Loan scenario, slice, and integration coverage

**Files:**
- Create: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/testing/FakeLoanDependencies.kt`
- Create: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanApplicationScenarioTest.kt`
- Create: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/LoanServiceScenarioTest.kt`
- Create: `app/loan/src/test/kotlin/com/socoolheeya/bluebank/loan/service/CreditScoreScenarioTest.kt`
- Create: `app/loan/src/sliceTest/kotlin/com/socoolheeya/bluebank/loan/controller/LoanControllerSliceTest.kt`
- Create: `data/loan-data/src/integrationTest/kotlin/com/socoolheeya/bluebank/loan/data/LoanDataIntegrationTest.kt`
- Create: `data/loan-data/src/integrationTest/resources/application-integration.yml`
- Modify: `app/loan/build.gradle.kts`
- Modify: `data/loan-data/build.gradle.kts`

**Interfaces:**
- Produces: stateful fakes for `LoanDataService`, `LoanApplicationDataService`, and `AccountClient`
- Covers: all public methods of loan services and both loan controllers

- [ ] **Step 1: Write and run RED loan scenarios**

Cover account validity; credit-score response; minimum score and income boundaries; DSR at and above 40%; application submit/get/list/missing; approve command mapping and loan-type mapping across credit, secured, and refinance families; reject; loan get/list/missing; execute and repay command mapping and amount preservation.

Run: `./gradlew :app:loan:test --tests '*Scenario*' --console=plain`

Expected: FAIL until fake data/client boundaries are complete.

- [ ] **Step 2: Implement loan fakes and verify GREEN**

Store loan/application results and commands, mutate application/loan states, and provide configurable account validation while leaving unfinished remote money movement unimplemented.

Run: `./gradlew :app:loan:test --console=plain`

Expected: all loan scenario and existing tests PASS.

- [ ] **Step 3: Add and verify loan controller slices**

Exercise every endpoint through MockMvc, asserting request binding for approval/rejection/repayment values, list and object response shapes, execution state, and missing-resource or invalid-request behavior.

Run: `./gradlew :app:loan:sliceTest --console=plain`

Expected: non-zero tests, zero failures and skips.

- [ ] **Step 4: Add and verify loan data integration tests**

Use real H2 repositories/data services for application create/query/approve/reject; loan creation/query/execution; repayment and outstanding-balance updates; repayment and credit history records used by data services.

Run: `./gradlew :data:loan-data:integrationTest --console=plain`

Expected: all loan integration cases PASS.

- [ ] **Step 5: Commit**

```bash
git add app/loan data/loan-data
git commit -m "test: cover loan domain layers"
```

### Task 6: Coverage audit, documentation, and full verification

**Files:**
- Modify: `docs/testing/testballoon-testing-standard.md`
- Create: `docs/testing/layered-test-guide.md`
- Modify: only test/build files identified by the audit

**Interfaces:**
- Consumes: all domain suites and layered Gradle tasks
- Produces: executable coverage matrix and verified root lifecycle

- [ ] **Step 1: Audit public APIs against tests**

Generate controller/service function inventories with:

```bash
rg -n '^\s*fun ' app/{account,card,deposit,loan}/src/main/kotlin --glob '*Controller.kt' --glob '*Service.kt'
rg -n 'test\(|testSuite' app/{account,card,deposit,loan}/src/{test,sliceTest} data/*/src/integrationTest --glob '*.kt'
```

Add a test for every uncovered public operation and rerun its smallest owning task.

- [ ] **Step 2: Document the three layers and commands**

Document placement, fixture ownership, fake-vs-real boundary, naming, filtering, and these commands: `test`, `sliceTest`, `integrationTest`, `check`, and `build`. Add an explicit rule that every task must report a non-zero test count.

- [ ] **Step 3: Run fresh compile and layered verification**

Run:

```bash
./gradlew cleanTest compileKotlin compileTestKotlin test sliceTest integrationTest build --console=plain
```

Expected: `BUILD SUCCESSFUL`; every layer has non-zero XML test totals; failures, errors, and unintended skips are zero.

- [ ] **Step 4: Verify declaration and formatting constraints**

Run:

```bash
rg -n 'org\.junit|@Test|@BeforeEach|@AfterEach|@SpringBootTest' app data testing --glob '*.kt'
git diff --check
```

Expected: no unauthorized JUnit declarations; `git diff --check` exits 0.

- [ ] **Step 5: Run Superpowers closeout and commit**

After fresh verification, run `/ce-compound mode:headless all-domain TestBalloon layered test implementation`, then commit only the implementation, test, and documentation files belonging to this plan.

```bash
git add build.gradle.kts settings.gradle.kts testing app data docs/testing docs/solutions
git commit -m "test: add all-domain layered coverage"
```
