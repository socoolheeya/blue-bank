# Blue Bank 테스트 작성 표준

## 기본 원칙

모든 테스트 선언은 TestBalloon DSL을 사용한다.

```kotlin
import de.infix.testBalloon.framework.core.testSuite

val accountTests by testSuite {
    test("계좌 잔액을 계산한다") {
        check(/* expectation */)
    }
}
```

TestBalloon 테스트는 top-level delegated `testSuite`로 등록하고, 중첩 suite로 시나리오를 표현한다. 가변 상태는 test-level fixture에서 만들고, 공유 가능한 고비용 읽기 전용 자원만 suite-level fixture로 공유한다.

테스트 이름은 구현 메서드명이 아니라 검증하는 행위와 결과를 설명한다. public controller/service 연산을 추가하거나 변경할 때는 같은 변경에서 [계층별 테스트 가이드](layered-test-guide.md)의 API 인벤토리를 확인하고 가장 작은 소유 계층에 성공·실패 경계 테스트를 둔다.

## TestBalloon만 사용하는 테스트

다음 범주에는 JUnit Jupiter를 사용하지 않는다.

- 도메인 로직 테스트
- 서비스 단위 테스트
- Mapper/Converter 테스트
- 직렬화·역직렬화 테스트
- 코루틴 테스트
- 파라미터화 테스트

파라미터화는 별도 runner나 annotation 대신 Kotlin 반복문과 중첩 `testSuite`를 사용한다. 코루틴 테스트는 TestBalloon의 suspend test 본문에서 직접 실행한다.

## JUnit Jupiter를 허용하는 인프라 호환 계층

다음 테스트는 Spring의 TestContext/JUnit extension 생태계가 필요한 경우에만 JUnit Jupiter를 허용한다.

- `@SpringBootTest`
- `@WebMvcTest`
- `@DataJpaTest`
- `@DataMongoTest`
- Spring Security 통합 테스트
- Spring Batch Job 테스트

이 예외 테스트는 별도 파일 또는 별도 integration 테스트 source set에 둔다. 테스트 이름과 문서에 인프라 의존성을 명시하고, 도메인·서비스·변환 로직을 JUnit 예외 계층으로 옮기지 않는다.

## 의존성 규칙

- TestBalloon plugin과 `testBalloon-framework-core`는 모든 Kotlin 테스트 모듈의 기본 의존성이다.
- JUnit Jupiter 의존성(`spring-boot-starter-test`, `kotlin-test-junit5`, `junit-platform-launcher`)은 위 인프라 테스트가 실제로 존재하는 모듈에만 추가한다.
- TestBalloon과 JUnit을 같은 모듈에서 사용할 때는 두 엔진을 모두 Gradle test task에서 발견할 수 있게 유지하고, 특정 엔진만 실행하는 전역 필터를 추가하지 않는다.
- Wrapper 파일은 루트 wrapper 통합 세션이 소유한다. 이 표준 변경에서는 wrapper를 수정하지 않는다.

## 실행과 유효성 규칙

- `test`: 도메인, 변환기, 서비스 시나리오처럼 Spring/실제 저장소가 필요 없는 빠른 테스트를 실행한다.
- `sliceTest`: controller HTTP binding, JSON, validation, exception mapping을 제한된 Spring MVC 경계에서 실행한다.
- `integrationTest`: 실제 Spring context와 H2 repository/transaction 경계를 실행한다.
- `check`: 해당 모듈의 세 계층(`test`, `sliceTest`, `integrationTest`)을 검증 lifecycle로 실행한다.
- `build`: compile, package, `check`를 포함하는 최종 모듈/루트 lifecycle이다.

검증 대상으로 선언한 모든 task는 XML 결과에서 **0보다 큰 test count**를 보고해야 한다. `NO-SOURCE`, 0 tests, 필터로 인한 전부 제외는 성공으로 간주하지 않는다. failures와 errors는 0이어야 하며, 의도하지 않은 skipped test도 0이어야 한다. 특정 계층을 소유하지 않는 모듈의 0건 결과는 루트 집계에서 성공한 계층으로 세지 말고, 그 계층을 소유한 모듈 task를 명시적으로 확인한다.

필터링은 가장 작은 소유 task와 fully-qualified suite/class 이름을 사용한다. 필터가 TestBalloon의 동적 test 발견을 제거하지 않는지 XML test count로 확인한다.

```bash
./gradlew :app:account:test --tests '*AccountServiceScenarioTest*' --console=plain
./gradlew :app:account:sliceTest --tests '*AccountControllerSliceTest*' --console=plain
./gradlew :data:account-data:integrationTest --tests '*AccountDataIntegrationTest*' --console=plain
```

## 리뷰 체크리스트

1. 테스트가 순수 로직·서비스·변환·직렬화·코루틴·파라미터화라면 TestBalloon DSL만 사용했는가?
2. JUnit import가 있다면 위 인프라 예외 중 하나로 설명되는가?
3. TestBalloon blue code에서 가변 상태를 green code로 누출하지 않는가?
4. Spring context, DB, batch 자원은 fixture 또는 JUnit 인프라 생명주기로 정리되는가?
5. 새 테스트가 가장 빠른 실행 계층에 배치되어 있는가?
6. 실행한 각 계층의 XML 결과가 0보다 큰 tests와 0 failures/errors/unintended skips를 보고하는가?
