# All-Domain Layered Test Design

## Goal

Add comprehensive TestBalloon coverage for the account, card, deposit, and loan domains across scenario, Spring MVC slice, and Spring/JPA integration layers. Every public controller and service operation receives meaningful coverage, including its principal success path and business-relevant rejection or boundary behavior.

## Scope

The test matrix covers all currently implemented public behavior in the four application domains. “All” means every public controller endpoint and service method is exercised at least once, with additional cases for meaningful validation branches, missing resources, ownership checks, state transitions, and monetary boundaries. It does not mean generating every Cartesian combination of enum values or duplicating the same framework behavior across endpoints.

Production code markers that represent unimplemented remote behavior, such as account money movement from the loan service, are documented and tested according to current observable behavior. This work does not implement those integrations merely to make a test possible.

The batch application and standalone data-module smoke tests remain outside the four requested business domains except where their existing verification must continue to pass.

## Test Architecture

### Scenario tests

Fast in-memory scenario suites remain under each application module’s `src/test` source set and run through `test`.

- Instantiate services directly with focused fakes for data services and remote clients.
- Cover every public service method, its command/result mapping, and meaningful validation branches.
- Express workflows with nested TestBalloon suites and Given/When/Then names.
- Create fresh mutable fakes inside each test so state cannot leak across TestBalloon registration and execution phases.
- Prefer stateful fakes over interaction-only mocks when a workflow changes state across multiple calls.

### Slice tests

Controller-focused suites live in `src/sliceTest` and run through `sliceTest`.

- Start the smallest practical Spring web context for the controller under test.
- Provide fake service beans at the application boundary.
- Exercise requests through Spring MockMvc or its Spring Boot 4 equivalent so routing, parameter binding, JSON serialization, HTTP status, and response shape are verified together.
- Test every endpoint’s normal response and representative invalid or missing-resource behavior supported by the current exception handling configuration.
- Close every Spring context deterministically from a TestBalloon fixture.

### Integration tests

Spring/JPA/H2 suites live in `src/integrationTest` and run through `integrationTest`.

- Start a focused application or data context with deterministic H2 configuration and discovery disabled.
- Exercise real repositories and data services without replacing the persistence layer.
- Cover persistence, query methods, state transitions, and transaction behavior used by each domain’s application services.
- Run database suites sequentially unless each test owns an independently isolated database.
- Roll back per-test transactions where possible; otherwise create and close a fresh context or database for the test.

## Gradle Execution Model

Each participating module receives `sliceTest` and `integrationTest` source sets and Test tasks. Both tasks reuse the same isolated JUnit Platform/TestBalloon runtime already shared by `test` and `testBalloon`.

Execution order is:

1. `test`
2. `sliceTest`
3. `integrationTest`

`check` depends on both new tasks so a normal `build` cannot silently omit them. Task reports remain separate, allowing failures and execution counts to be attributed to the correct layer.

## Domain Coverage Matrix

### Account

- Slice: external health endpoint; internal account lookup by ID and number; customer account listing; balance lookup; active, inactive, and missing-account validation responses.
- Scenario: account creation and product-specific default limits; lookup and missing-account errors; modification; close, freeze, and activation; balance lookup; deposit, withdrawal, transfer, positive amount, and distinct-account validation; interest operations.
- Integration: account and holder persistence; number, customer, product, and parent queries; balance and ledger updates; account lifecycle state changes; interest persistence and period queries.

### Card

- Slice: card lookup/listing; active-card listing; activation; usage toggle; lost and terminated transitions; application submission, lookup, customer listing, and issuance.
- Scenario: card state operations and ownership validation; application mapping; meeting-card account requirement; application lookup failure; issuance workflow and generated card attributes.
- Integration: card and application persistence; customer and active-state queries; application status changes; issue/state transitions; transaction, statement, benefit, and cashback persistence paths used by data services.

### Deposit

- Slice: deposit creation; lookup/listing; activation; contribution; early withdrawal; termination; request binding and ownership/validation failures.
- Scenario: account validity and ownership checks during creation; product/period mapping; activation, contribution, early withdrawal, and termination ownership checks; generated deposit number invariants; lookup/list mapping.
- Integration: deposit creation and retrieval; customer queries; activation and termination; contribution and withdrawal transaction records; balance and maturity-related state used by the data service.

### Loan

- Slice: loan and application lookup/listing; application submission; approval and rejection; loan execution and repayment; request and response mapping; missing-resource behavior.
- Scenario: account validation; credit score, income, and DSR eligibility boundaries; application lookup; approval command mapping; rejection; loan lookup; execute and repay commands; credit-score response.
- Integration: application creation, queries, approval and rejection; loan creation, execution, repayment, and outstanding balance updates; repayment and related history persistence used by data services.

## Shared Test Support

A `testing:test-support` module is introduced only for helpers consumed across modules. Its initial responsibilities are limited to:

- isolated TestBalloon task/runtime conventions if they cannot remain entirely in the root build;
- deterministic Spring context ownership and closing;
- reusable assertion helpers for expected exceptions;
- small test-data utilities that do not depend on a single domain.

Domain builders and fakes stay in their owning module unless duplication is demonstrated. Test support must not appear on production runtime classpaths.

## Error Handling and Determinism

- Tests assert the current HTTP error contract rather than inventing a new global exception format.
- Random or time-derived identifiers are checked by stable invariants unless production code already exposes an injectable generator.
- No external network, Eureka, or real third-party credit service is required.
- Monetary assertions use exact `BigDecimal` values and avoid floating-point conversion.
- Every opened context, transaction, and executor is closed even when an assertion fails.

## Verification

Completion requires all of the following:

- All four domains have scenario, slice, and integration suites.
- Every public controller endpoint and service method appears in the coverage matrix and is exercised.
- Meaningful validation and missing-resource branches are covered without exhaustive redundant combinations.
- `test`, `sliceTest`, and `integrationTest` each discover a non-zero number of TestBalloon tests.
- `check` and `build` execute the separated layers.
- No new JUnit test declarations or lifecycle annotations are introduced.
- `./gradlew compileKotlin compileTestKotlin test sliceTest integrationTest build` succeeds.
- XML reports show zero failures and zero unintended skips.
- `git diff --check` succeeds.

## Non-goals

- Implementing unfinished cross-service money movement or third-party integrations.
- Starting all four microservices as a networked end-to-end environment.
- Exhaustively testing every equivalent enum/input combination.
- Refactoring unrelated production code solely for coverage.
