# TestBalloon Test Architecture and JUnit Migration

## Goal

Make TestBalloon the only test declaration framework in blue-bank. Migrate every active JUnit test, remove JUnit lifecycle coupling, and provide fast, reusable structures for unit, slice, scenario, and integration tests across all Kotlin modules.

## Current State

- The repository contains seven Spring context smoke-test classes and three `AccountRepository` integration tests declared with JUnit Jupiter.
- `IntegrationTestBase` couples repository tests to `@SpringBootTest`, `@BeforeEach`, inheritance, and a full application context.
- Every module repeats test dependencies and JUnit Platform configuration.
- One commented-out JUnit context test remains as dead code.

## Architecture

### Central Gradle configuration

The root build defines the TestBalloon plugin version compatible with Kotlin 2.3.21 and applies consistent test dependencies and execution policy to Kotlin subprojects. Modules retain Spring-specific test dependencies only where their tests need them.

Test execution is separated by cost and isolation:

- `test`: unit and in-memory scenario suites that do not start Spring.
- `sliceTest`: focused Spring MVC or JPA contexts.
- `integrationTest`: complete application contexts and cross-component tests.

The standard verification lifecycle runs faster suites before integration tests. TestBalloon owns concurrency; Gradle `maxParallelForks` is not configured.

### Shared test support

A non-production `test-support` module provides reusable test APIs without exposing them to main runtime classpaths:

- Spring context fixtures with deterministic close behavior.
- JPA fixtures with per-test transaction rollback.
- Scenario vocabulary and domain test-data builders.
- Reusable context smoke-suite registration.

Shared helpers use composition rather than test base-class inheritance. Each fixture makes its resource lifetime and isolation policy explicit.

### TestBalloon suite conventions

Tests are top-level properties delegated to `testSuite`. Nested suites express Given/When/Then scenarios without annotation-based discovery.

Mutable resources are created inside TestBalloon fixtures. Registration-phase code contains immutable test cases only, preventing mutable state from leaking from TestBalloon blue code into green execution code.

- Test-level fixtures provide fresh mutable state for unit, slice, and repository tests.
- Suite-level fixtures are reserved for expensive, safely shared read-only contexts.
- Independent suites may use concurrent compartments.
- Database suites that cannot guarantee isolation use sequential compartments.

## JUnit Migration

All active JUnit declarations are migrated:

- Replace `@Test` and `@DisplayName` with TestBalloon `testSuite` and `test` names.
- Replace `@BeforeEach` and `IntegrationTestBase` with fixture setup and teardown.
- Replace `@SpringBootTest`-driven discovery with programmatic Spring context fixtures.
- Replace empty context tests with assertions for representative required beans.
- Consolidate the commented account-data context test into an active TestBalloon suite and delete the dead source.
- Remove JUnit imports and dependencies that are no longer required after verified migration.

AssertJ may remain as an assertion library because it is independent of the test declaration framework. New examples prefer Kotlin assertions or Power Assert-compatible expressions to minimize framework coupling.

## Coverage and Feedback Speed

Coverage is accumulated across the three test task types into a module-aware aggregate report. Local and pull-request feedback runs in increasing cost order:

1. Compile test sources.
2. Run `test`.
3. Run `sliceTest`.
4. Run `integrationTest` where required by the change or on the full verification path.

Test filters remain available through Gradle/TestBalloon paths so a developer can run one suite or scenario. Expensive context fixtures are reused only when doing so cannot leak mutable state between tests.

## Failure Handling

- Fixture setup failures fail the owning suite with the original cause.
- Every opened Spring context is closed through fixture teardown.
- Every repository test transaction is rolled back even when an assertion fails.
- No test depends on source order unless it is explicitly placed in a sequential compartment and documented as a workflow scenario.

## Verification

The migration is complete only when all of the following hold:

- No active Kotlin test source imports `org.junit` or uses JUnit test/lifecycle annotations.
- TestBalloon discovers and executes every migrated suite.
- Unit, slice, and integration test source sets compile.
- The repository tests pass against the configured test database.
- Every module build succeeds from the root Gradle wrapper.
- Coverage reporting completes without dropping the separated test execution data.

## Non-goals

- Rewriting production behavior solely to increase coverage.
- Introducing Testcontainers before a test requires database-vendor behavior that H2 cannot represent.
- Retaining compatibility helpers for authoring new JUnit tests.
