# Blue Bank 계층별 테스트 가이드

## 계층과 소유권

| 계층 | 위치 | 검증 경계 | fixture 소유권 | 기본 명령 |
| --- | --- | --- | --- | --- |
| 서비스/도메인 | `app/<domain>/src/test` | 도메인 규칙, DTO 변환, service orchestration | 각 test가 새 fake와 가변 상태를 소유 | `./gradlew :app:<domain>:test` |
| Controller slice | `app/<domain>/src/test` | route, path/query/body binding, JSON, validation, error mapping | 각 test가 새 controller와 deterministic fake service를 소유 | `./gradlew :app:<domain>:test` |
| Data integration | `data/<domain>-data/src/integrationTest` | 실제 Spring wiring, H2 repository, transaction과 rollback | integration test/context가 DB 행을 만들고 정리 | `./gradlew :data:<domain>-data:integrationTest` |

서비스 테스트는 외부 계층을 fake로 대체하되 service 자체를 fake로 만들지 않는다. Controller slice는 HTTP 경계 아래 service를 deterministic fake로 두고 repository나 DB를 시작하지 않는다. Data integration은 repository/data service와 실제 H2를 사용하며 controller 또는 원격 HTTP를 포함하지 않는다. fixture는 상태를 변경하는 가장 작은 test가 소유한다. suite-level 가변 fixture가 불가피하면 매 test 전 명시적 deterministic reset을 제공해야 하며 병렬 실행에 안전해야 한다. 현재 card slice를 포함한 controller slice는 각 test에서 새 fake와 `MockMvc`를 생성한다.

## API 커버리지 인벤토리

아래 행은 public controller/service 함수와 이를 소유하는 대표 suite의 매핑이다. 한 test가 하나의 lifecycle 시나리오로 여러 public 연산을 검증할 수 있지만, 각 연산의 입력 전달 또는 관찰 가능한 결과가 assertion에 포함되어야 한다.

| 도메인 | Service public API | Controller public API | 소유 suite |
| --- | --- | --- | --- |
| account | `AccountService`: create, number/id/customer lookup, modify, close, freeze, activate; `BalanceService`: lookup, deposit, withdraw, transfer; `InterestService`: monthly payment, average balance, total/history/period history, expected interest | external health; internal id/number/customer lookup, balance, validate | `AccountServiceScenarioTest`, `BalanceServiceScenarioTest`, `InterestServiceScenarioTest`, `AccountControllerSliceTest` |
| card | `CardService`: card/customer/active lookup, activate, toggle usage, report lost, terminate; `CardApplicationService`: apply, issue, application/customer lookup | health; all card lifecycle/query endpoints; apply/application/customer/issue endpoints | `CardServiceScenarioTest`, `CardApplicationScenarioTest`, `CardControllerSliceTest` |
| deposit | `DepositService`: create, activate, contribute, early withdraw, terminate, deposit/customer lookup | health/error; create/query and all lifecycle endpoints | `DepositServiceScenarioTest`, `DepositControllerSliceTest` |
| loan | `CreditScoreService`: score lookup; `LoanService`: loan/customer lookup, execute, repay; `LoanApplicationService`: apply/application/customer lookup, approve, reject | health; loan query/execute/repay; application apply/query/approve/reject | `CreditScoreScenarioTest`, `LoanServiceScenarioTest`, `LoanApplicationScenarioTest`, `LoanControllerSliceTest` |

저장소 계약은 각각 `AccountDataIntegrationTest`, `CardDataIntegrationTest`, `DepositDataIntegrationTest`, `LoanDataIntegrationTest`가 실제 H2를 사용해 보완한다. 2026-07-17 선언 감사 기준으로 위 public 연산에는 미커버 gap이 없어 추가 테스트가 필요하지 않았다.

## 이름과 배치

- suite 이름은 경계를 드러낸다: `... scenarios`, `... controller slices`, `... data integration`.
- test 이름은 `조건 + 행위 + 관찰 결과`로 작성한다. 내부 호출 횟수만 검증하지 말고 반환값, 상태, command 또는 HTTP 응답을 검증한다.
- 순수 service/도메인 규칙을 slice나 integration 계층에서만 검증하지 않는다. 빠른 `test`에 규칙을 두고, 상위 계층에는 wiring 계약만 둔다.
- 실제 DB가 필요한 repository/query/transaction 검증은 owning `data/*-data` 모듈에 둔다.

## 실행, 필터링, 판정

```bash
# 가장 빠른 소유 계층
./gradlew :app:card:test --console=plain
./gradlew :data:card-data:integrationTest --console=plain

# 모듈 및 루트 lifecycle
./gradlew :app:card:check --console=plain
./gradlew check --console=plain
./gradlew build --console=plain

# 전체 fresh layered verification
./gradlew cleanTest cleanIntegrationTest \
  compileKotlin compileTestKotlin test integrationTest build \
  --rerun-tasks --console=plain
```

집중 검증은 파일/class 이름이 아니라 TestBalloon delegated suite identity에 적용한다. 먼저 owning task를 필터 없이 실행하고 `build/test-results/<task>/TEST-suite_*.xml`에서 identity를 확인한다. 예를 들어 `accountServiceScenarios`, `accountControllerSlices`, `accountDataIntegration`은 각각 `--tests '*<property>*'`로 실제 검증되었다. 실행 후 XML을 집계해 해당 task의 tests가 0보다 큰지 확인한다. 실행한 모든 계층에서 failures/errors는 0, 의도하지 않은 skipped는 0이어야 한다. Gradle exit code가 0이어도 `NO-SOURCE`, tests=0, 필터가 모든 TestBalloon test를 제외한 결과는 검증 성공이 아니다. 동적 중첩 suite/test 하나만 안정적으로 선택할 identity가 없다면 suite보다 좁게 추측하지 말고 owning task 전체를 실행한다.

## 변경 체크리스트

1. controller/service의 public 함수를 선언 검색으로 인벤토리한다.
2. 각 함수의 성공 경로와 중요한 validation/state 실패 경로를 owning suite에 매핑한다.
3. gap만 가장 작은 계층에 추가하고 그 task를 먼저 실행한다.
4. `test`, `integrationTest`, `check`, `build`를 위험도에 맞게 확대 실행한다.
5. XML test count와 failures/errors/skips를 확인하고 선언 감사 및 `git diff --check`를 통과시킨다.

## 리뷰에서 확인할 실패 신호

- 증분 빌드 성공을 clean compile 성공으로 간주하지 않는다. 실제 closeout에서는 증분 실행이 숨긴 테스트 소스 오타를 `cleanTest`가 발견했다.
- 공유 Spring context에서 각 test가 `cleanDatabase()`를 호출하게 두지 않는다. context를 test가 `use`로 닫거나, 안전한 suite fixture와 `TestCompartment.Sequential`을 함께 사용한다.
- 상태 이름 하나만 확인하지 않는다. 카드 발급은 실제 card row, application의 `cardId`, 최종 `ISSUED` 상태를 repository 재조회로 함께 검증한다.
- 서로 다른 값을 넣고 결과가 다름을 기대하는 assertion은 false-positive 신호다. 대출 승인은 approved amount/rate가 생성된 loan의 principal, outstanding balance, interest rate와 정확히 일치해야 한다.
- 금융 경계는 정상값만 검증하지 않는다. 0·음수 금액, 잔액 초과, 정확한 DSR 한계와 한계 직후 값을 포함하고 실패 후 잔액·원장·상태가 보존되는지 확인한다.

2026-07-17 통합 전 전체 검증에서는 application `test` 71개와 controller test 15개, `integrationTest` 16개가 실행되었고 failures/errors/skips는 모두 0이었다. 현재 controller test는 application `test`에 포함된다.
