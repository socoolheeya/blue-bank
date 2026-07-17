---
title: Gradle test가 TestBalloon 테스트를 조용히 건너뛰는 문제
date: 2026-07-17
category: test-failures
module: blue-bank-multi-module-test-infrastructure
problem_type: test_failure
component: testing_framework
symptoms:
  - "표준 Gradle test 태스크가 성공하지만 TestBalloon 테스트 결과가 0건이다."
  - "TestBalloon 엔진 제외를 제거하면 JUnitException과 NoSuchMethodError가 발생한다."
root_cause: config_error
resolution_type: config_change
severity: medium
related_components:
  - development_workflow
tags:
  - testballoon
  - gradle
  - junit-platform
  - test-discovery
  - runtime-classpath
---

# Gradle test가 TestBalloon 테스트를 조용히 건너뛰는 문제

## Problem

`./gradlew test`가 성공했지만 TestBalloon 테스트를 하나도 실행하지 않았다. TestBalloon을 별도 태스크에서만 실행 가능하게 만들면서 표준 검증 생명주기에서 엔진을 제외한 것이 요구사항과 반대되는 동작을 만들었다.

## Symptoms

- `./gradlew test`는 성공하지만 TestBalloon 결과 XML이 생성되지 않는다.
- `./gradlew testBalloon`을 직접 실행해야만 테스트가 발견된다.
- 엔진 제외만 제거하면 TestBalloon 엔진 초기화 중 `JUnitException`과 `NoSuchMethodError`가 발생한다.

## What Didn't Work

- 표준 `test` 태스크에서 `excludeEngines("de.infix.testBalloon")`를 사용하면 호환성 오류는 감춰지지만 테스트 전체가 조용히 누락된다.
- 엔진 제외만 제거하면 Spring 테스트 런타임의 JUnit Platform과 TestBalloon용 JUnit Platform이 섞여 바이너리 호환성 오류가 발생한다.

## Solution

루트 `build.gradle.kts`에서 기존 테스트 런타임의 JUnit 관련 아티팩트를 제외하고 JUnit Platform 1.13.4를 추가한 격리 클래스패스를 한 번 정의한다. 표준 `test`와 전용 `testBalloon` 태스크가 이 클래스패스를 함께 사용하게 한다.

```kotlin
val testBalloonRuntime = files(
    sourceSets["main"].output,
    sourceSets["test"].output,
    bootRuntime.map { configuration ->
        configuration.filterNot { file ->
            file.name.startsWith("junit-platform-") ||
                file.name.startsWith("junit-jupiter-") ||
                file.name.startsWith("junit-bom-")
        }
    },
    rootProject.configurations[testBalloonPlatform.name]
)

tasks.named<Test>("test") {
    classpath = testBalloonRuntime
    useJUnitPlatform()
}

tasks.register<Test>("testBalloon") {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = testBalloonRuntime
    useJUnitPlatform()
}
```

## Why This Works

TestBalloon 엔진, JUnit Platform launcher, engine, commons를 호환되는 동일 버전으로 정렬하므로 런타임 메서드 불일치가 사라진다. 두 Gradle 태스크가 같은 테스트 클래스와 런타임을 사용하므로 표준 생명주기와 명시적 TestBalloon 실행 결과도 일치한다.

## Prevention

- 엔진 호환성 문제를 테스트 엔진 제외로 우회하지 않는다. 테스트 0건 성공은 정상 검증으로 취급하지 않는다.
- TestBalloon 또는 JUnit Platform 설정 변경 후 두 태스크의 결과 XML에서 실행 수와 스킵 수를 확인한다.
- 다음 명령을 함께 실행해 표준 생명주기, 전용 태스크, 컴파일과 패키징을 검증한다.

```bash
./gradlew cleanTest test
./gradlew testBalloon
./gradlew compileKotlin compileTestKotlin build
```

## Related Issues

- [TestBalloon testing standard](../../testing/testballoon-testing-standard.md)
- [TestBalloon migration design](../../superpowers/specs/2026-07-17-testballoon-migration-design.md)
