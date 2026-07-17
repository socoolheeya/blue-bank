# TestBalloon Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Migrate all active JUnit tests to TestBalloon and centralize reusable scenario, Spring-context, and JPA fixture patterns without changing wrapper ownership.

**Architecture:** The root Gradle build owns the TestBalloon plugin version and common test dependencies. Existing `src/test` tests remain the default fast test task for this migration; reusable fixture helpers live in a test-support module, while Spring smoke and repository suites use TestBalloon fixtures directly. Wrapper files are out of scope.

**Tech Stack:** Kotlin 2.3.21, Spring Boot 4.1.0, TestBalloon 1.0.0-K2.3.20, Kotlin assertions, H2.

## Global Constraints

- Use `de.infix.testBalloon` version `1.0.0-K2.3.20`.
- Use `de.infix.testBalloon:testBalloon-framework-core:1.0.0-K2.3.20`.
- Do not modify or commit Gradle wrapper files; invoke the root `./gradlew` only.
- No active test source may import `org.junit` or use JUnit test/lifecycle annotations.
- Spring resources must be created and closed by TestBalloon fixtures.
- Repository scenarios must rollback their transaction and run sequentially against shared H2 state.

### Task 1: Central TestBalloon Gradle setup

**Files:**
- Modify: `build.gradle.kts`, `settings.gradle.kts`
- Modify: every `app/*/build.gradle.kts` and `data/*/build.gradle.kts`
- Create: `testing/test-support/build.gradle.kts` if shared support is needed

- [ ] Add the TestBalloon plugin declaration and framework-core dependency consistently to all Kotlin JVM test modules, preserve existing Spring test dependencies, and keep `useJUnitPlatform()` only where Gradle requires it.
- [ ] Add `testing:test-support` to the root project only if its compiled helpers are consumed by more than one module; otherwise keep helpers in focused test sources to avoid an unnecessary production graph.
- [ ] Run `./gradlew compileTestKotlin` and fix dependency/plugin resolution errors before migrating tests.

### Task 2: Shared Spring/JPA fixture API

**Files:**
- Create: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/SpringTestFixtures.kt`
- Create: `testing/test-support/src/main/kotlin/com/socoolheeya/bluebank/testing/TestBalloonDsl.kt`
- Test: `testing/test-support/src/test/kotlin/com/socoolheeya/bluebank/testing/TestBalloonArchitectureTest.kt`

- [ ] Implement `SpringTestContext` as an `AutoCloseable` wrapper around `SpringApplicationBuilder(...).web(WebApplicationType.NONE).run()` with `bean<T>()` lookup.
- [ ] Implement a `rollback {}` helper with `TransactionTemplate` and `status.setRollbackOnly()`; never hold a transaction across a suspending TestBalloon boundary.
- [ ] Add reusable `testSuite` examples covering nested scenario names and fresh test fixtures.
- [ ] Run the support module test task and verify fixture teardown on success and failure.

### Task 3: Migrate application context tests in parallel

**Files:**
- Modify: `app/account/src/test/**/AccountApplicationTests.kt`
- Modify: `app/card/src/test/**/CardApplicationTests.kt`
- Modify: `app/deposit/src/test/**/DepositApplicationTests.kt`
- Modify: `app/loan/src/test/**/LoanApplicationTests.kt`
- Modify: `app/batch/src/test/**/BatchApplicationTests.kt`
- Modify: corresponding test resources only when required to disable Eureka or supply H2.

- [ ] Replace each JUnit class with a top-level delegated `testSuite` and a fixture that starts the module application class programmatically.
- [ ] Disable Eureka/discovery for smoke tests and assert at least one representative application bean, not an empty context.
- [ ] Use explicit test H2 settings for modules that otherwise depend on external services; keep batch configuration minimal and deterministic.
- [ ] Run each affected module’s test task through `./gradlew`.

### Task 4: Migrate data-module tests and repository scenarios in parallel

**Files:**
- Modify: `data/card-data/src/test/**/CardDataApplicationTests.kt`
- Modify: `data/deposit-data/src/test/**/DepositDataApplicationTests.kt`
- Modify: `data/loan-data/src/test/**/LoanDataApplicationTests.kt`
- Modify: `data/account-data/src/test/**/repository/AccountRepositoryTest.kt`
- Delete: `data/account-data/src/test/**/IntegrationTestBase.kt`
- Delete: commented `AccountDataApplicationTests.kt`

- [ ] Replace data context smoke tests with TestBalloon suites using module-specific test configuration classes where no main `@SpringBootConfiguration` exists.
- [ ] Convert the three repository tests to one sequential TestBalloon suite using a Spring/JPA fixture, explicit repository/entity-manager lookup, and per-test rollback.
- [ ] Replace AssertJ/JUnit imports with Kotlin assertions or explicit assertion dependencies compatible with TestBalloon.
- [ ] Run all data-module tests and verify no JUnit imports remain.

### Task 5: Final verification and coverage wiring

**Files:**
- Modify: root/module Gradle scripts only where required for `test` and JaCoCo reporting
- Create: `docs/testing/testballoon-guide.md`

- [ ] Add a concise guide showing unit scenario, Spring slice-style fixture, and full integration fixture patterns.
- [ ] Configure JaCoCo report tasks to include the migrated TestBalloon Gradle test task execution data without changing wrapper ownership.
- [ ] Run `./gradlew compileKotlin compileTestKotlin test build` and inspect `rg -n 'org\\.junit|@Test|@BeforeEach|@SpringBootTest' app data --glob '*.kt'`.
- [ ] Run `git diff --check`; report any pre-existing user changes separately.
