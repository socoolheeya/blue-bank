# TestFixture and Unified Test Source Design

## Goal

Use TestBalloon's fixture lifecycle explicitly in scenario tests and keep controller slice tests in the ordinary `src/test` source set. A module's fast service scenarios and Spring MVC slice tests will run through its standard `test` task, while repository and full Spring integration tests remain in `src/integrationTest` and run through `integrationTest`.

## Fixture Model

Scenario suites declare their mutable context with `testFixture { ... } asContextForEach { ... }`:

```kotlin
val bookingScenarios by testSuite("Booking scenarios") {
    testFixture {
        BookingTestContext()
    } asContextForEach {
        Scenario("a booking is created") {
            Given("an available product") { /* arrange */ }
            When("the customer books it") { /* act */ }
            Then("the booking is stored") { /* assert */ }
        }
    }
}
```

`asContextForEach` creates one fresh context per registered scenario, prevents mutable fake state from leaking between tests, and remains safe when tests run concurrently. If a context implements `AutoCloseable`, TestBalloon owns cleanup at the end of that test. Suite-level fixture forms such as `asContextForAll` are reserved for expensive, effectively read-only resources and are not used for mutable scenario contexts.

The shared scenario DSL gains a fixture-receiver form of `Scenario`. Inside the fixture scope, each scenario wraps the current context in `ScenarioScope`, so `Given`, `When`, and `Then` continue to execute in declaration order and retain their step-specific failure messages. The existing context-factory form may remain temporarily only if needed for a safe migration; migrated application scenarios use the visible fixture form.

## Test Source Layout

Controller slice tests move from each `app/*/src/sliceTest` tree into the corresponding package under `app/*/src/test`. The root build no longer creates a `sliceTest` source set or `sliceTest` task.

The standard `test` task must discover and execute both:

- TestBalloon service and domain tests.
- Spring/JUnit MVC slice tests such as `@WebMvcTest`.

The test runtime therefore retains the compatible JUnit Platform, Jupiter engine, Spring test dependencies, and TestBalloon engine together. Existing platform version alignment remains explicit so both engines use one compatible JUnit Platform runtime. Module build files replace `sliceTestImplementation` configuration with ordinary `testImplementation` where necessary.

Repository and full-context integration tests remain in `src/integrationTest`. The `integrationTest` source set and task remain separate because they have a materially slower lifecycle and infrastructure boundary.

## Gradle Lifecycle

- `test`: all tests under `src/test`, including TestBalloon scenarios and Spring MVC slices.
- `testBalloon`: optional focused task for the same `src/test` output, retaining only the configuration needed to select TestBalloon tests reliably.
- `integrationTest`: tests under `src/integrationTest`.
- `check`: depends on `test` and `integrationTest`; it no longer depends on `sliceTest`.
- `build`: compile, package, and run the complete `check` lifecycle.

No successful task is accepted as verification when it reports zero tests for a layer that is expected to contain tests.

## Migration Scope

1. Add fixture-scoped scenario DSL coverage in `testing:test-support` before changing application tests.
2. Convert the existing account, card, deposit, and loan scenario suites to visible `testFixture { Context() } asContextForEach` declarations.
3. Move all existing application controller slice test files into `src/test` without changing their package or tested behavior.
4. Remove slice source-set configuration and update affected module dependencies.
5. Update testing documentation, task examples, and API-to-test ownership mappings to describe the unified source layout.

This change does not merge `integrationTest` into `test`, change production behavior, or introduce shared mutable suite fixtures.

## Failure Handling and Compatibility

- Fixture construction failures fail the owning scenario before any step executes.
- `Given`, `When`, and `Then` continue to wrap failures with the step kind and description while preserving the original cause.
- Fixture cleanup is delegated to TestBalloon; `AutoCloseable.close()` runs after each scenario context lifetime.
- Moving slice files must preserve Spring annotations, profiles, resources, and package visibility.
- Test discovery must be checked for both engines after the classpath change; a Gradle exit code alone is insufficient.

## Verification

Run focused fixture DSL tests first, followed by each affected application module's unified test task, integration tasks that still exist, and the root build:

```bash
./gradlew :testing:test-support:test --console=plain
./gradlew :app:account:test :app:card:test :app:deposit:test :app:loan:test --console=plain
./gradlew integrationTest --console=plain
./gradlew build --console=plain
```

Inspect generated `TEST-*.xml` results to confirm that TestBalloon scenarios and Spring MVC slices both report nonzero test counts, with zero failures, errors, and unintended skips. Confirm that no `src/sliceTest` directory, `sliceTest` source-set configuration, or stale `:sliceTest` command remains.

## Acceptance Criteria

- Application scenario suites visibly use TestBalloon `testFixture` with `asContextForEach`.
- Every scenario receives an isolated context and retains Given/When/Then failure decoration.
- Controller slice tests live under `src/test` and execute via the module's `test` task.
- No custom `sliceTest` source set or task remains.
- `integrationTest` remains separate and operational.
- Documentation matches the resulting lifecycle.
- The complete project compiles and `./gradlew build` succeeds with nonzero expected test counts.
