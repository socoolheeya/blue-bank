# Spring Batch 6 금융 EOD 애플리케이션 설계

## 목표

`app/batch`에 Spring Batch 6 기반 일 마감 애플리케이션을 구축한다. 배치는 네 data 모듈을 직접 의존해 전일 거래 마감 및 원장 정리, 이자와 수수료 계산, 자동이체와 예약거래 실행, 카드 결제 및 타행 정산을 수행한다.

## 실행 계약

- 모든 수동 실행은 `businessDate=YYYY-MM-DD` Job Parameter를 필수로 받는다.
- 스케줄 실행은 매일 01:00 Asia/Seoul에 전일을 `businessDate`로 전달한다.
- 스케줄 활성화 여부와 cron은 설정으로 변경할 수 있다.
- 동일 `businessDate`의 재실행은 중복 출금, 이자 반영, 수수료 부과 또는 정산을 발생시키지 않는다.

## 아키텍처

하나의 `dailyEodJob`이 업무 순서를 보장하고 각 업무는 독립 Step으로 구성한다.

1. `openEodExecutionStep`
2. `closeLedgerStep`
3. `executeScheduledTransferStep`
4. `calculateInterestAndFeeStep`
5. `settleCardTransactionStep`
6. `settleExternalTransactionStep`
7. `closeEodExecutionStep`

대량 업무 Step은 Spring Batch 6의 chunk 기반 Reader/Processor/Writer로 구현하며 기본 chunk 크기는 100이다. 실행 상태 초기화와 종료처럼 단일 제어 작업은 tasklet Step으로 구현한다. 시스템 오류는 Step을 실패시켜 재시작할 수 있게 하고, 잔액 부족이나 유효하지 않은 거래 상태 같은 업무 오류는 해당 항목을 실패로 기록한 뒤 다음 항목을 계속 처리한다.

## 모듈과 패키지

`app/batch`는 다음 data 모듈을 직접 의존한다.

- `data:account-data`
- `data:card-data`
- `data:deposit-data`
- `data:loan-data`

배치 코드는 다음 책임별 패키지로 분리한다.

- `batch.config`: Job, Step, 스케줄 및 설정 속성
- `batch.eod`: EOD 실행 모델, 원장 마감, 실행 리스너
- `batch.transfer`: 자동이체와 예약거래
- `batch.interest`: 이자와 수수료 계산
- `batch.settlement`: 카드와 타행 정산
- `batch.support`: 업무일자, 멱등키 및 오류 기록 공통 기능

## 데이터 모델

### EodExecution

업무일자별 전체 실행 상태를 관리한다. `businessDate`에 유일 제약을 두고 `STARTED`, `COMPLETED`, `FAILED` 상태, 시작·종료 시각, 단계별 성공·실패 건수와 오류 요약을 저장한다.

### ScheduledTransfer

출금 계좌, 입금 계좌, 금액, 예약일, 반복 유형, 다음 실행일, 상태 및 재시도 횟수를 저장한다. 실행 결과에는 `businessDate + transferId` 기반 고유 멱등키를 사용한다. 성공 시 두 계좌 잔액과 양쪽 원장을 하나의 트랜잭션으로 변경한다. 잔액 부족은 해당 건만 `FAILED`로 기록한다.

### EodAccountingEntry

이자, 수수료, 카드 정산 및 타행 정산의 계산 결과와 근거를 저장하는 배치 보조 원장이다. `businessDate + referenceType + referenceId + entryType`에 유일 제약을 두고 기준 금액, 적용 요율, 결과 금액을 보관한다.

### ExternalSettlement

카드 또는 타행 정산의 대상 기관, 총액, 수수료, 순정산액, 상태와 업무일자를 저장한다. 기관·업무일자·정산 유형으로 재실행 중복을 방지한다.

## Step 상세

### 원장 마감

`closeLedgerStep`은 업무일자의 `LedgerEntry`를 계좌별로 집계하고 기초 잔액, 입금, 출금 및 기말 잔액의 일관성을 검증한다. 불일치 계좌는 업무 오류로 기록하고 다른 계좌의 마감은 계속한다.

### 자동이체와 예약거래

`executeScheduledTransferStep`은 `businessDate`에 실행 예정인 `PENDING` 건을 조회한다. 잔액과 계좌 상태를 검증한 뒤 출금·입금 잔액 및 원장을 하나의 트랜잭션으로 반영한다. 반복 이체는 성공 후 다음 실행일을 갱신하고, 단건 이체는 완료 상태로 전환한다.

### 이자와 수수료

`calculateInterestAndFeeStep`은 활성 계좌, 예금 및 대출을 대상으로 다음 기본 공식을 적용한다.

- 계좌·예금 일 이자: `기준 잔액 × 연이율 ÷ 365`
- 대출 일 이자: `대출 잔액 × 연이율 ÷ 365`
- 계좌 관리 수수료: 기본 0원, 상품별 설정값
- 카드 타행 정산 수수료: 거래금액 × 설정 요율
- 통화 금액은 원 단위 `RoundingMode.HALF_UP`

모든 요율과 고정 수수료는 설정으로 외부화한다. 이자와 수수료는 `EodAccountingEntry`의 고유 키로 중복 반영을 방지한다.

### 카드와 타행 정산

`settleCardTransactionStep`은 승인 상태이고 정산일이 도래한 카드 거래를 처리하여 `SETTLED`로 전환한다. 국내·해외 및 타행 구분에 따라 수수료와 순정산액을 계산한다.

`settleExternalTransactionStep`은 미정산 외부 거래를 업무일자와 기관별로 집계하고 `ExternalSettlement`을 생성한 뒤 대상 건의 상태를 갱신한다.

## 트랜잭션과 오류 처리

- Spring Batch 메타 테이블과 업무 테이블은 동일 데이터소스와 트랜잭션 관리자를 사용한다.
- 각 chunk가 트랜잭션 경계가 된다.
- 업무 오류는 오류 코드와 메시지를 실행 결과에 남기고 skip한다.
- DB 연결, 잠금, 직렬화 및 예상하지 못한 시스템 오류는 Step을 실패시킨다.
- 실패 Job은 동일 `businessDate`와 식별 가능한 재실행 파라미터로 실패 Step부터 재시작한다.
- 모든 금전 변경은 업무 결과와 대응하는 원장 또는 정산 기록을 같은 트랜잭션에 저장한다.

## 설정

`application.yaml`에서 다음을 설정한다.

- 스케줄 활성화 여부와 cron
- Asia/Seoul 시간대
- chunk 크기
- 계좌·예금·대출 이자 계산 설정
- 상품별 계좌 관리 수수료
- 국내·해외 및 타행 카드 정산 요율
- 업무 실패 skip 한도

테스트 환경에서는 스케줄과 외부 서비스 탐색을 비활성화한다.

## 테스트

단위 테스트는 이자·수수료 공식, 원 단위 반올림, 업무일자 경계, 멱등키와 업무 오류 분류를 검증한다.

Spring Batch 통합 테스트는 다음을 검증한다.

- 지정한 `businessDate`의 최초 EOD 성공
- 동일 업무일자 재실행 시 금전 변경 중복 없음
- 중간 Step 실패 후 해당 Step부터 재시작
- 업무 실패 한 건 이후 다음 항목 처리 지속
- 승인되고 정산일이 도래한 카드 거래만 정산
- 자동이체 성공 시 두 계좌 잔액과 원장이 함께 반영
- 잔액 부족 시 해당 이체만 실패하고 잔액은 변경되지 않음

검증 명령은 `./gradlew :app:batch:test`와 `./gradlew build`이다. data 모듈은 독립 실행 애플리케이션이 아닌 라이브러리이므로, 실행 구성이 없는 빈 `@SpringBootTest`는 제거하고 실제 repository 및 배치 통합 테스트를 유지한다.

## 완료 기준

- 네 업무군이 `dailyEodJob`의 정의된 순서로 실행된다.
- 동일 업무일자 재실행이 금전 결과를 중복 생성하지 않는다.
- 업무 오류는 건별 격리되고 시스템 오류는 재시작 가능한 실패로 남는다.
- Spring Batch 메타데이터와 EOD 업무 실행 기록에서 처리 결과를 확인할 수 있다.
- batch 테스트와 전체 Gradle 빌드가 성공한다.
