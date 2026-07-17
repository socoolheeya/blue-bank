---
title: Spring Batch에서 여러 JPA 데이터 모듈을 통합할 때 발생하는 이름 충돌과 정밀도 손실
date: 2026-07-17
category: integration-issues
module: batch-eod
problem_type: integration_issue
component: background_job
symptoms:
  - 여러 모듈의 동명 엔티티 때문에 애플리케이션 컨텍스트가 DuplicateMappingException으로 시작하지 못함
  - 동명 Repository 인터페이스를 함께 스캔하면 Spring 빈 이름이 충돌함
  - 연이율 0.0365가 DB 저장 후 0.04가 되어 일 이자가 10원 대신 11원으로 계산됨
  - 같은 컨텍스트에서 재실행하면 상태를 가진 ItemReader가 이미 소진되어 항목을 읽지 못함
  - Spring Batch JobInstance만 의존하면 다른 rerun 파라미터로 금전 거래가 중복 반영될 수 있음
root_cause: config_error
resolution_type: config_change
severity: high
related_components:
  - database
  - testing_framework
tags:
  - spring-batch-6
  - spring-data-jpa
  - multi-module
  - entity-scanning
  - bean-naming
  - decimal-precision
  - idempotency
  - restartability
---

# Spring Batch에서 여러 JPA 데이터 모듈을 통합할 때 발생하는 이름 충돌과 정밀도 손실

## Problem

독립적으로 개발된 account, card, deposit, loan JPA 모듈을 하나의 Spring Batch 6 EOD 애플리케이션이 직접 의존하면서 각 모듈 안에서는 보이지 않던 전역 이름 충돌과 DB 정밀도 문제가 드러났다. 컨텍스트 시작 실패뿐 아니라 재실행 시 누락 또는 중복 전기처럼 금전 결과를 훼손할 수 있는 문제였다.

## Symptoms

- account, deposit, loan 모듈에 각각 존재하는 `InterestPayment`가 동일한 기본 JPA 엔티티 이름을 사용해 Hibernate `DuplicateMappingException`이 발생했다.
- 여러 모듈의 동명 Repository가 기본 단순 클래스명 기반의 Spring 빈 이름을 공유했다.
- `Account.interestRate`의 DB 숫자 스케일이 부족해 `0.0365`가 `0.04`로 변환되었고, `balance × annualRate ÷ 365` 결과가 10원에서 11원으로 달라졌다.
- 싱글턴 람다 `ItemReader`가 첫 실행에서 iterator를 소진한 뒤 두 번째 JobInstance에서 빈 결과를 반환했다.
- `businessDate`가 같아도 `rerun` 파라미터를 추가하면 별도 JobInstance가 되므로, Spring Batch 메타데이터만으로는 업무 부작용의 중복을 막지 못한다.

## What Didn't Work

- 패키지가 다르면 JPA 엔티티와 Repository도 자동으로 격리된다고 가정했다. 하나의 persistence context와 application context로 합쳐지면 기본 엔티티명, 물리 테이블명, 빈 이름은 모두 전역 충돌 대상이다.
- 계산기 단위 테스트만으로 이자 공식을 검증했다. 계산 자체가 정확해도 DB 왕복 과정의 scale 강제 변환은 단위 테스트에서 발견되지 않는다.
- JobInstance의 중복 실행 방지만으로 업무 멱등성을 보장하려 했다. 운영 재처리를 위해 파라미터가 달라지면 같은 업무일의 금전 부작용을 다시 실행할 수 있다.
- 실행 간 상태를 보존하는 싱글턴 reader를 그대로 재사용했다. 첫 실행 성공이 reader의 재시작 안전성을 증명하지 않는다.

## Solution

각 모듈의 영속성 식별자를 전체 애플리케이션 범위에서 고유하게 만들었다. 엔티티명과 테이블명을 함께 명시하고, 문자열 JPQL도 새 엔티티명을 사용하도록 수정했다.

```kotlin
@Entity(name = "AccountInterestPayment")
@Table(name = "account_interest_payment")
class InterestPayment(/* ... */)

@Query("select p from AccountInterestPayment p where ...")
fun findPayments(/* ... */): List<InterestPayment>
```

Repository 빈은 완전 수식 이름으로 생성해 동명 인터페이스를 구분했다.

```kotlin
@EnableJpaRepositories(
    basePackages = ["com.socoolheeya.batch", "com.socoolheeya.bluebank"],
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator::class,
)
```

요율 컬럼에는 업무상 필요한 정밀도와 스케일을 명시했다.

```kotlin
@Column(precision = 8, scale = 6)
var interestRate: BigDecimal
```

현재 reader는 `StepExecution.id`가 바뀔 때 `businessDate`와 iterator를 초기화해 순차 재실행을 지원한다. 병렬 실행 가능성이 생기면 mutable singleton 대신 step-scoped reader로 전환해야 한다.

업무 멱등성은 `businessDate + operation + reference`로 만든 결정적 키, 사전 존재 확인, DB unique constraint를 함께 사용했다. 잔액 변경과 원장·회계 감사 레코드는 동일 트랜잭션에 둔다. `exists` 후 `insert`에는 동시성 경합이 있으므로 최종 권위는 unique constraint이며, 병렬 실행 시 제약 위반을 이미 처리된 업무로 해석하는 정책도 필요하다.

마지막으로 동일 `businessDate`에 다른 `rerun` 파라미터를 준 두 번째 JobInstance를 실행해 잔액과 레코드 수가 변하지 않는 통합 테스트를 추가했다. 독립 실행 구성이 없는 라이브러리 모듈의 빈 `@SpringBootTest` 대신 실제 소비자인 batch 애플리케이션의 조합된 컨텍스트를 검증한다.

## Why This Works

모듈 경계와 런타임 네임스페이스는 같은 개념이 아니다. 명시적인 엔티티·테이블·빈 이름은 하나로 합쳐진 런타임 네임스페이스를 충돌 없이 구성한다. DB 컬럼의 precision/scale은 메모리 계산과 저장 결과 사이의 계약을 고정한다.

Spring Batch 메타데이터는 실행 이력을 관리하지만 업무적으로 동일한 금전 거래를 정의하지는 않는다. 결정적 업무 키와 DB 제약은 JobInstance가 달라도 동일 거래의 중복 반영을 차단하며, 재실행 통합 테스트는 reader 상태와 DB 부작용을 함께 검증한다.

## Prevention

- 새 데이터 모듈을 조합하기 전에 전체 entity scan 범위에서 기본 엔티티명, 테이블명, Repository 단순 이름의 중복을 검색한다.
- 금액과 요율의 모든 `BigDecimal` 컬럼에 업무 규칙에 맞는 precision/scale을 명시하고 DB 왕복 후 금전 결과를 단언한다.
- reader의 mutable 상태는 step scope에 두거나 `StepExecution` 단위로 명시적으로 초기화한다. 같은 컨텍스트에서 서로 다른 JobInstance를 연속 실행한다.
- 금전 부작용마다 결정적 업무 키와 DB unique constraint를 두고, 상태 변경과 감사 레코드를 한 트랜잭션으로 묶는다.
- 테이블명 변경은 운영 DB에서 명시적인 마이그레이션이 필요하다. 자동 DDL에 의존해 기존 데이터를 옮기지 않는다.
- 변경 후 `./gradlew :app:batch:test`, `./gradlew build`, `git diff --check`를 실행한다.

## Related Issues

- [Spring Batch 6 EOD 설계](../../superpowers/specs/2026-07-17-spring-batch-6-eod-design.md)
- [Spring Batch 6 EOD 구현 계획](../../superpowers/plans/2026-07-17-spring-batch-6-eod.md)
- [TestBalloon 테스트가 기본 test task에서 조용히 누락되는 문제](../test-failures/testballoon-default-test-task-silently-skips-tests-2026-07-17.md)
