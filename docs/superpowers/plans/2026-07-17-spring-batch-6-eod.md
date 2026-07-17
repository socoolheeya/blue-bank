# Spring Batch 6 Financial EOD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a restartable and idempotent Spring Batch 6 daily EOD job for ledger close, scheduled transfers, daily interest and fees, and card/external settlement.

**Architecture:** `app/batch` directly uses all four data modules and owns batch-specific entities and repositories. A single `dailyEodJob` orders tasklet control steps and chunk-oriented business steps, with `businessDate` passed as a required job parameter and persisted idempotency keys protecting every monetary result.

**Tech Stack:** Kotlin 2.3.21, Java 25, Spring Boot 4.1.0, Spring Batch 6, Spring Data JPA, H2, TestBalloon, Gradle 9.6.1

## Global Constraints

- `businessDate` uses ISO `YYYY-MM-DD` and is mandatory for manual launches.
- Scheduled launches run at 01:00 Asia/Seoul with the previous calendar date.
- Monetary calculations use `RoundingMode.HALF_UP` at zero fractional digits.
- Business failures are recorded per item; infrastructure failures fail the step.
- The same business date and reference cannot create duplicate monetary effects.
- Existing domain module APIs and unrelated user changes remain intact.

---

### Task 1: EOD domain model and calculation policies

**Files:**
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/eod/EodModels.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/eod/EodRepositories.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/interest/DailyAmountCalculator.kt`
- Create: `app/batch/src/test/kotlin/com/socoolheeya/batch/interest/DailyAmountCalculatorTest.kt`

**Interfaces:**
- Produces: `DailyAmountCalculator.interest(balance, annualRate)` and `fee(baseAmount, rate)` returning rounded `BigDecimal`
- Produces: JPA models `EodExecution`, `EodAccountingEntry`, `ScheduledTransfer`, `ScheduledTransferExecution`, `ExternalSettlement`, and `LedgerClose`

- [ ] Write tests proving `100000 × 3.65% ÷ 365 = 10`, half-up rounding, zero-rate behavior, and stable idempotency keys.
- [ ] Run `./gradlew :app:batch:test` and verify the new tests fail because the policy and models are missing.
- [ ] Implement enums, entities, unique constraints, repositories, and the minimal pure calculation policy.
- [ ] Re-run the targeted tests and verify they pass.

### Task 2: Scheduled-transfer processing

**Files:**
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/transfer/ScheduledTransferService.kt`
- Create: `app/batch/src/test/kotlin/com/socoolheeya/batch/transfer/ScheduledTransferServiceTest.kt`
- Modify: `data/account-data/src/main/kotlin/com/socoolheeya/bluebank/account/data/repository/LedgerEntryRepository.kt`

**Interfaces:**
- Consumes: `BalanceRepository`, `LedgerEntryRepository`, `ScheduledTransferRepository`, `ScheduledTransferExecutionRepository`
- Produces: `ScheduledTransferService.execute(transferId: Long, businessDate: LocalDate): TransferOutcome`

- [ ] Write tests for atomic debit/credit, two ledger entries, insufficient-funds isolation, and duplicate execution returning `ALREADY_PROCESSED`.
- [ ] Run the targeted test and verify expected failures.
- [ ] Implement transactional transfer processing and deterministic `EOD-TRANSFER-{businessDate}-{transferId}` transaction IDs.
- [ ] Re-run targeted tests and verify green.

### Task 3: Ledger, interest, fee, and settlement services

**Files:**
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/eod/LedgerCloseService.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/interest/InterestAndFeeService.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/settlement/SettlementService.kt`
- Create: `app/batch/src/test/kotlin/com/socoolheeya/batch/eod/EodServiceTest.kt`
- Modify: `data/account-data/src/main/kotlin/com/socoolheeya/bluebank/account/data/repository/LedgerEntryRepository.kt`
- Modify: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/repository/CardTransactionRepository.kt`

**Interfaces:**
- Produces: `LedgerCloseService.close(accountId, businessDate)`
- Produces: `InterestAndFeeService.processAccount/processDeposit/processLoan`
- Produces: `SettlementService.settleCardTransaction(transactionId, businessDate)` and `aggregateExternalSettlements(businessDate)`

- [ ] Write service tests for ledger reconciliation, daily interest entries, approved-card-only settlement, settlement fee calculation, and duplicate suppression.
- [ ] Run targeted tests and verify expected failures.
- [ ] Add date/status repository queries and implement the three transactional services.
- [ ] Re-run targeted tests and verify green.

### Task 4: Spring Batch 6 job, scheduling, and configuration

**Files:**
- Modify: `app/batch/build.gradle.kts`
- Modify: `app/batch/src/main/kotlin/com/socoolheeya/batch/BatchApplication.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/config/EodProperties.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/config/DailyEodJobConfiguration.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/config/EodScheduler.kt`
- Create: `app/batch/src/main/kotlin/com/socoolheeya/batch/support/BusinessDate.kt`
- Modify: `app/batch/src/main/resources/application.yaml`
- Create: `app/batch/src/test/resources/application.yaml`

**Interfaces:**
- Produces: Spring Batch bean `dailyEodJob`
- Produces: seven named steps in the approved order
- Produces: configurable `EodProperties` and previous-day scheduler launch

- [ ] Write a context/job-structure test requiring all seven steps and mandatory `businessDate` parsing.
- [ ] Run it and verify failure before configuration exists.
- [ ] Add four direct data-module dependencies, JPA/H2 test dependencies, entity/repository scanning, properties, Job/Step beans, listeners, and scheduler.
- [ ] Run `./gradlew :app:batch:test` and verify green.

### Task 5: Integration verification and legacy test cleanup

**Files:**
- Create: `app/batch/src/test/kotlin/com/socoolheeya/batch/eod/DailyEodJobIntegrationTest.kt`
- Delete: `data/card-data/src/test/kotlin/com/socoolheeya/bluebank/card/data/CardDataApplicationTests.kt`
- Delete: `data/deposit-data/src/test/kotlin/com/socoolheeya/bluebank/deposit/data/DepositDataApplicationTests.kt`
- Delete: `data/loan-data/src/test/kotlin/com/socoolheeya/bluebank/loan/data/LoanDataApplicationTests.kt`

**Interfaces:**
- Consumes: Complete `dailyEodJob`
- Produces: Executable evidence for first run, idempotent rerun, transfer failure isolation, and card settlement

- [ ] Add H2-backed integration fixtures and assertions for the approved workflows.
- [ ] Run `./gradlew :app:batch:test` and verify integration behavior.
- [ ] Remove invalid library-module context smoke tests that have no `@SpringBootConfiguration`.
- [ ] Run `./gradlew build` and fix only failures caused by this implementation.
- [ ] Run fresh structural, batch-test, and full-build verification before closeout.
