---
title: Module-local Kotlin TestBalloon scenario DSL and reliable migration verification
date: 2026-07-17
last_updated: 2026-07-18
category: developer-experience
module: application scenario tests
problem_type: developer_experience
component: testing_framework
severity: medium
applies_when:
  - "Each application module must own its test helpers under src/test"
  - "A small test DSL would otherwise require a shared testing module"
  - "A repository-wide test migration needs structural and behavioral verification"
tags:
  - kotlin
  - testballoon
  - test-dsl
  - module-ownership
  - test-isolation
  - migration-verification
---

# Module-local Kotlin TestBalloon scenario DSL and reliable migration verification

## Context

Account, card, deposit, and loan scenario tests use the same small `Scenario`/`Given`/`When`/`Then` shape. The first implementation placed that DSL in a new `testing:test-support` Gradle module because several modules consumed identical code.

That reuse assumption violated the repository's ownership boundary: each module must manage its tests and helpers entirely within its own `src/test`. Cross-module duplication is intentional here. A build can compile and pass while still implementing the wrong architecture, so review must verify ownership as well as behavior.

## Guidance

Keep each module's DSL under its own test package:

```text
app/account/src/test/.../account/testing/ScenarioDsl.kt
app/card/src/test/.../card/testing/ScenarioDsl.kt
app/deposit/src/test/.../deposit/testing/ScenarioDsl.kt
app/loan/src/test/.../loan/testing/ScenarioDsl.kt
```

The fixture-scoped extension remains deliberately small:

```kotlin
fun <C : Any> TestFixture.Scope<suspend C.(Test.ExecutionScope) -> Unit>.Scenario(
    name: String,
    body: ScenarioScope<C>.() -> Unit,
) {
    test(name) {
        ScenarioScope(this).body()
    }
}
```

Each scenario suite imports only its domain-local helper and creates mutable state with `testFixture { Context() } asContextForEach`. This gives every execution fresh fakes and services while the steps within one scenario share ordered state. Step failures add `Given`/`When`/`Then` context and preserve the original cause.

Do not create a shared test module based only on repeated helper code. Shared infrastructure requires an explicit repository-level ownership decision; similarity alone is insufficient.

For a bulk migration, verify both structure and behavior:

- `settings.gradle.kts` does not include a testing support project.
- Application and data modules have no project dependency on test support.
- Every scenario imports its own domain's testing package.
- Scenario names, boundary values, ownership identities, command shapes, and snapshots remain intact.
- Each module compiles and the full build succeeds.

## Why This Matters

Module-local test support keeps each test source set self-contained. One module can change its fixtures or DSL without creating a repository-wide test API or coupling unrelated modules through a support project.

The duplicated helper is small and stable. Avoiding a shared Gradle module removes more architectural surface—settings registration, dependency edges, exported testing-framework types, and separate support tests—than the duplication costs.

## When to Apply

- Repository policy requires every module to own its test code under `src/test`.
- A helper is short enough to remain understandable in each owning module.
- Cross-module reuse would create a test-only Gradle project or public test framework API.
- Mechanical test migrations must preserve exact behavioral coverage.

Do not force this DSL onto ordinary unit, controller, integration, or application-context tests solely for uniformity.

## Examples

Structural and build verification:

```bash
rg -n 'testing:test-support|com\.socoolheeya\.bluebank\.testing' \
  settings.gradle.kts app data --glob '*.kts' --glob '*.kt'
find app -path '*/src/test/*/testing/ScenarioDsl.kt' -print
./gradlew :app:account:compileTestKotlin :app:card:compileTestKotlin \
  :app:deposit:compileTestKotlin :app:loan:compileTestKotlin --console=plain
./gradlew build --console=plain
git diff --check
```

The first search must return no output, the local DSL search must return one file for each owning application module, and the build must execute the application and integration test lifecycle successfully.

## Related

- [TestBalloon testing standard](../../testing/testballoon-testing-standard.md)
- [Unified src/test runtime](testballoon-unified-src-test-runtime-2026-07-18.md)
- The original shared-module design and implementation plan under `docs/superpowers/` are historical and superseded by module-local ownership.
