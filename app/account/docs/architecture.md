# Account Service Architecture

## 1. 개요

Blue Bank의 통장(계좌) 서비스는 블루뱅크의 통장 상품을 참고하여 설계되었습니다.
이 문서는 계좌 시스템의 전체 아키텍처, 도메인 모델, 데이터 흐름, 상품별 상세 요구사항을 설명합니다.

## 2. 통장 상품 분석 (블루뱅크 기준)

### 2.1 상품 종류 및 상세 정보

#### 2.1.1 입출금통장

**상품 설명**
- 기본 입출금 계좌로 "누구나 쉽게 개설하고 자유롭게 입출금하며, 수수료 부담 없이 간편이체와 여유자금 관리" 가능

**금리 정보**
- 기본금리: 연 0.10% (세전, 2026.02.08 기준)
- 이자지급: 매월 넷째 금요일 결산 후 다음날 지급
- 세이프박스 연동 시: 연 1.60% (세전)

**주요 특징**
- 계좌번호 불필요한 간편이체 (카카오톡 친구에게 직접 송금)
- 모바일앱 기반 영업점 방문 없이 계좌개설, 대출, 해외송금
- 타행이체, 자동이체, ATM 입출금, 이체, 증명서 발급 모든 수수료 면제

**이용 조건**
- 가입대상: 만 14세 이상 내국인
- 1인 1계좌 원칙 (금융거래한도계좌)
- 20영업일 내 타행 계좌 개설 시 추가 개설 불가
- 예금자보호: 1억원까지

**연계 서비스**
- 세이프박스
- 저금통 (잔돈 자동 적립)

#### 2.1.2 모임통장

**상품 설명**
- "여럿이 함께 쓰고, 회비·생활비를 쉽고 투명하게 관리할 수 있는" 입출금통장 기반 서비스

**금리 정보**
- 기본금리: 연 0.10% (세전, 2026.02.08 기준)
- 이자지급: 매월 넷째 금요일 결산 후 다음날 지급

**주요 특징**
- **AI 모임총무**: 회비 요약과 지출 분석을 자동으로 정리
- 카카오톡 친구 초대로 손쉬운 모임 구성 (최대 100명)
- 모든 멤버가 회비 내역과 잔액 함께 조회
- 모임게시판에서 문자, 사진 공유
- 모임 전용 체크카드로 결제 시 랜덤 캐시백

**모임원 관리**
- 카카오톡 친구 또는 채팅방 멤버 초대
- AI 초대장으로 모임 내용, 일정, 장소, 회비 정보 공유
- 최대 100명까지 참여 가능

**회비 관리**
- 매달 회비 설정으로 자동 알림
- 월별 지출내역 한눈에 확인
- 포함/제외 내역 설정 가능
- 메시지 카드를 통한 회비 요청

**이용 조건**
- 모임주: 블루뱅크 입출금통장 보유자 (만 17세 이상)
- 모임원: 블루뱅크 회원
- 1인당 최대 100개 모임 개설 가능
- 1인당 최대 30개 모임 참여 가능
- 제한: 비상금대출, 마이너스통장, 개인사업자통장, 압류 계좌 신청 불가

**연계 서비스**
- 모임 체크카드 (캐시백 혜택)

#### 2.1.3 우리아이통장

**상품 설명**
- "부모가 자녀 명의로 만들어 주고, 성장과 함께 이용한도를 조절하며 추억까지 남길 수 있는" 전용 입출금통장

**금리 정보**
- 기본금리: 연 0.10% (세전, 2026.02.08 기준)
- 이자지급: 매월 넷째 금요일 결산 후 다음날 지급
- 일일 최종잔액에 약정금리 적용하여 누적 지급

**주요 특징**
- 만 0~17세 미만 자녀를 위한 블루뱅크 앱 내 간편 개설
- 저축금액 및 계약기간 제한 없음
- 타행 이체, ATM 수수료 면제
- **추억 기록 기능**: 거래 내역마다 축하·응원 메시지를 남겨 아이 성장 후 확인 가능

**부모-자녀 연동**
- 친권 보유 엄마·아빠가 함께 계좌 관리
- 법정대리인 연결 필수

**이용 조건**
- 가입대상: 만 17세 미만 (1인 1계좌)
- 법정대리인: 만 19세 이상의 부 또는 모 필수
- 거래방법: 블루뱅크 모바일앱 이체만 가능 (오픈뱅킹 불가)
- 우리아이서비스의 근거계좌로만 사용 가능

**연계 서비스**
- 우리아이적금

#### 2.1.4 기록통장

**상품 설명**
- "저축할 때마다 의미 있는 순간을 기록하고, 규칙적으로 모을 수 있는" 기록형 입출금 자유 예금

**금리 정보**
- 기본금리: 연 1.60% (세전, 2026.02.08 기준)
- 금리쿠폰: 추가 우대금리 제공 가능
- 이자지급: 매월 넷째 금요일 결산 후 다음날 지급
- 매일 최종 잔액에 약정금리 적용

**주요 특징**
- **기록별 관리**: 최대 10개 섹션으로 목표·용도별 관리
- **모으기 규칙**: 섹션마다 최대 20개의 규칙 설정으로 원하는 금액 자동 입금
- **특별한 순간 저장**: 입·출금 시 기록 저장, 메모 작성
- 섹션별 통계 관리
- 계좌 커버 사진 등록

**이용 조건**
- 가입대상: 블루뱅크 입출금통장 보유 실명 개인 (1인 1계좌)
- 최소가입금액: 0원 이상
- 납입한도: 섹션당 매월 최대 1천만원
- 입출금: 연결된 입출금통장으로만 가능
- 예금자보호: 1억원까지

**연계 서비스**
- 자유적금, 26주 적금
- 카카오톡, 페이스북 공유 기능

#### 2.1.5 세이프박스

**상품 설명**
- "여유자금을 하루만 맡겨도 이자가 붙는" 계좌 속 금고 상품

**금리 정보**
- 기본금리: 연 1.60% (세전, 2026.02.08 기준)
- 금리쿠폰: 조건에 따라 추가 우대금리 가능
- 하루만 입금해도 이자 적용
- **매일 복리**: 어제까지 쌓인 이자를 받으면 원금에 추가되어 복리 효과

**주요 특징**
- 계좌당 1개씩 개설 가능
- 이자 바로 받기 또는 매월 넷째 금요일 다음날 자동 지급
- 입출금통장 또는 개인사업자통장과 연결

**이용 조건**
- 가입대상: 실명의 개인 (블루뱅크 계좌 보유자)
- 입금한도: 기본 1천만원, 최대 1억원까지 증액 가능
- 연결된 블루뱅크 계좌로만 입출금 가능
- 명의변경 불가 (상속 시에만 가능)
- 예금자보호: 1인당 1억원까지

**연계 서비스**
- 입출금통장, 저금통, 자유적금

### 2.2 공통 요구사항

#### 기본 기능
- 실시간 잔액 조회 및 거래 내역 확인
- 계좌 간 이체 기능
- 거래 한도 설정 및 관리
- 계좌 상태 관리 (정상, 휴면, 동결, 해지)
- 보류 금액 관리 (Hold)

#### 이자 계산
- 일일 최종잔액 기준 계산
- 매월 넷째 금요일 결산
- 익일 자동 지급

#### 보안 및 규제
- 예금자보호법에 따른 1억원까지 보호
- 실명 확인 필수
- 1인 1계좌 원칙 (상품별 차이 있음)

## 3. 시스템 아키텍처

### 3.1 레이어 구조

```
┌─────────────────────────────────────┐
│   Presentation Layer (Controller)    │  - REST API
└──────────────┬──────────────────────┘
               │ DTO
┌──────────────▼──────────────────────┐
│   Application Layer (Service)        │  - 비즈니스 로직 조율
│   app/account/AccountService         │
└──────────────┬──────────────────────┘
               │ Command / Result
┌──────────────▼──────────────────────┐
│   Data Layer (DataService)           │  - 데이터 접근 및 트랜잭션
│   data/account-data/                 │
│   AccountDataService                 │
└──────────────┬──────────────────────┘
               │ Entity
┌──────────────▼──────────────────────┐
│   Persistence Layer (Repository)     │  - JPA Repository
└─────────────────────────────────────┘
```

### 3.2 의존성 방향

```
app/account  ──depends on──>  data/account-data
   (DTO)                        (Command/Result)
```

- **data 모듈**: 하위 레이어, app 모듈을 알지 못함
- **app 모듈**: 상위 레이어, data 모듈의 Command와 Result를 사용

### 3.3 모듈 구조

```
blue-bank/
├── app/
│   └── account/                          # Application Layer
│       ├── src/main/kotlin/
│       │   ├── controller/               # REST API Controller
│       │   ├── service/                  # Business Logic Service
│       │   └── dto/                      # Data Transfer Objects
│       └── docs/
│           └── architecture.md
│
└── data/
    └── account-data/                     # Data Layer
        └── src/main/kotlin/
            ├── domain/                   # Domain Entities
            │   ├── command/              # Command Objects (Input)
            │   └── result/               # Result Objects (Output)
            ├── repository/               # JPA Repositories
            └── service/                  # Data Service (Transaction)
```

## 4. 도메인 모델

### 4.1 핵심 Entity

#### Account (계좌)
```kotlin
@Entity
class Account {
    id: Long?                       // 계좌 ID (자동생성)
    accountNumber: String           // 계좌번호 (유니크)
    name: String?                   // 계좌 별칭
    accountType: AccountType        // 계좌 유형
    productType: ProductType        // 상품 유형 (신규)
    status: AccountStatus           // 계좌 상태
    interestRate: BigDecimal        // 적용 금리
    openedAt: LocalDateTime?        // 개설일
    closedAt: LocalDateTime?        // 해지일
    parentAccountId: Long?          // 부모 계좌 ID (우리아이통장용)
    linkedAccountId: Long?          // 연결 계좌 ID (세이프박스, 기록통장용)
}
```

#### Balance (잔액)
```kotlin
@Entity
class Balance {
    accountId: Long                 // 계좌 ID (PK)
    ledgerBalance: BigDecimal       // 총 잔액
    availableBalance: BigDecimal    // 사용 가능 잔액
    holdBalance: BigDecimal         // 보류 금액
    interestAccumulated: BigDecimal // 누적 이자
    updatedAt: LocalDateTime        // 최종 업데이트 시각
    version: Long                   // 낙관적 락용
}
```

#### AccountHolder (예금주)
```kotlin
@Entity
class AccountHolder {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    customerId: Long                // 고객 ID
    role: HolderRole                // 역할
    relationshipType: String?       // 관계 유형 (부모-자녀 등)
    joinedAt: LocalDateTime         // 가입일
}
```

#### AccountLimit (한도)
```kotlin
@Entity
class AccountLimit {
    accountId: Long                 // 계좌 ID (PK)
    dailyTransferLimit: BigDecimal  // 하루 송금 한도
    singleTransferLimit: BigDecimal // 1회 송금 한도
    monthlyDepositLimit: BigDecimal? // 월 입금 한도 (기록통장 섹션용)
    updatedAt: LocalDateTime        // 업데이트 시각
}
```

#### Hold (보류)
```kotlin
@Entity
class Hold {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    amount: BigDecimal              // 보류 금액
    reason: String                  // 보류 사유
    status: HoldStatus              // 상태
    createdAt: LocalDateTime        // 생성 시각
    releasedAt: LocalDateTime?      // 해제 시각
    expiresAt: LocalDateTime?       // 만료 시각
}
```

#### LedgerEntry (원장 기록)
```kotlin
@Entity
class LedgerEntry {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    entryType: EntryType            // 유형 (입금/출금)
    amount: BigDecimal              // 금액
    balanceAfter: BigDecimal        // 거래 후 잔액
    description: String             // 거래 설명
    memo: String?                   // 사용자 메모 (기록통장, 우리아이통장용)
    sectionId: Long?                // 섹션 ID (기록통장용)
    transactionId: String?          // 거래 ID
    occurredAt: LocalDateTime       // 거래 시각
}
```

#### AccountStatusHistory (상태 이력)
```kotlin
@Entity
class AccountStatusHistory {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    previousStatus: AccountStatus   // 이전 상태
    newStatus: AccountStatus        // 새 상태
    reason: String                  // 변경 사유
    changedAt: LocalDateTime        // 변경 시각
    changedBy: String               // 변경자
}
```

#### AccountSection (섹션 - 기록통장용)
```kotlin
@Entity
class AccountSection {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    name: String                    // 섹션 이름
    description: String?            // 설명
    coverImageUrl: String?          // 커버 이미지
    targetAmount: BigDecimal?       // 목표 금액
    currentAmount: BigDecimal       // 현재 금액
    displayOrder: Int               // 표시 순서
    createdAt: LocalDateTime        // 생성일
}
```

#### SavingRule (저축 규칙 - 기록통장용)
```kotlin
@Entity
class SavingRule {
    id: Long?                       // ID (자동생성)
    sectionId: Long                 // 섹션 ID
    name: String                    // 규칙 이름
    amount: BigDecimal              // 금액
    frequency: RuleFrequency        // 빈도 (매일, 매주, 매월)
    dayOfWeek: Int?                 // 요일 (1-7)
    dayOfMonth: Int?                // 일자 (1-31)
    isActive: Boolean               // 활성화 여부
    createdAt: LocalDateTime        // 생성일
}
```

#### GroupMeeting (모임 정보 - 모임통장용)
```kotlin
@Entity
class GroupMeeting {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    name: String                    // 모임 이름
    description: String?            // 모임 설명
    monthlyFee: BigDecimal?         // 월 회비
    maxMembers: Int                 // 최대 인원
    currentMembers: Int             // 현재 인원
    coverImageUrl: String?          // 커버 이미지
    createdAt: LocalDateTime        // 생성일
}
```

#### InterestPayment (이자 지급 이력)
```kotlin
@Entity
class InterestPayment {
    id: Long?                       // ID (자동생성)
    accountId: Long                 // 계좌 ID
    amount: BigDecimal              // 이자 금액
    interestRate: BigDecimal        // 적용 금리
    calculationPeriodStart: LocalDate // 계산 기간 시작
    calculationPeriodEnd: LocalDate   // 계산 기간 종료
    paidAt: LocalDateTime           // 지급일
}
```

### 4.2 Enum 정의

#### AccountType (계좌 유형)
```kotlin
enum class AccountType {
    CHECKING,           // 입출금 계좌
    SAVINGS,            // 저축 계좌
    TIME_DEPOSIT,       // 정기 예금
    SAFEBOX             // 세이프박스
}
```

#### ProductType (상품 유형) - 신규
```kotlin
enum class ProductType {
    BASIC_CHECKING,     // 입출금통장
    GROUP_MEETING,      // 모임통장
    CHILD_ACCOUNT,      // 우리아이통장
    RECORD_BOOK,        // 기록통장
    SAFEBOX             // 세이프박스
}
```

#### AccountStatus (계좌 상태)
```kotlin
enum class AccountStatus {
    ACTIVE,             // 정상
    DORMANT,            // 휴면
    FROZEN,             // 동결
    CLOSED              // 해지
}
```

#### HolderRole (예금주 역할)
```kotlin
enum class HolderRole {
    PRIMARY,            // 주 예금주
    JOINT,              // 공동 예금주
    PARENT,             // 부모 (우리아이통장)
    CHILD,              // 자녀 (우리아이통장)
    MEETING_OWNER,      // 모임주 (모임통장)
    MEETING_MEMBER      // 모임원 (모임통장)
}
```

#### EntryType (원장 기록 유형)
```kotlin
enum class EntryType {
    CREDIT,             // 입금
    DEBIT               // 출금
}
```

#### HoldStatus (보류 상태)
```kotlin
enum class HoldStatus {
    ACTIVE,             // 보류 중
    RELEASED,           // 해제됨
    CONVERTED           // 실제 출금으로 전환
}
```

#### RuleFrequency (저축 규칙 빈도)
```kotlin
enum class RuleFrequency {
    DAILY,              // 매일
    WEEKLY,             // 매주
    MONTHLY             // 매월
}
```

## 5. 데이터 흐름

### 5.1 Command Pattern (입력)

요청 데이터는 Command 객체로 표현됩니다.

**위치**: `data/account-data/src/main/kotlin/com/socoolheeya/bluebank/account/data/domain/command/AccountCommand.kt`

```kotlin
sealed interface AccountCommand {

    data class Create(
        val accountNumber: String,
        val name: String?,
        val accountType: AccountType,
        val productType: ProductType,
        val status: AccountStatus,
        val interestRate: BigDecimal,
        val customerId: Long,
        val parentAccountId: Long? = null,
        val linkedAccountId: Long? = null
    ) : AccountCommand {
        fun toEntity(): Account
    }

    data class Modify(
        val accountNumber: String,
        val name: String?
    ) : AccountCommand

    data class Search(
        val accountNumber: String
    ) : AccountCommand

    data class AddSection(
        val accountNumber: String,
        val name: String,
        val description: String?,
        val targetAmount: BigDecimal?
    ) : AccountCommand
}
```

### 5.2 Result Pattern (출력)

응답 데이터는 Result 객체로 표현됩니다.

**위치**: `data/account-data/src/main/kotlin/com/socoolheeya/bluebank/account/data/domain/result/AccountResult.kt`

```kotlin
data class AccountResult(
    val id: Long?,
    val accountNumber: String,
    val name: String?,
    val accountType: AccountType,
    val productType: ProductType,
    val status: AccountStatus?,
    val interestRate: BigDecimal,
    val openedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val balance: BalanceInfo?,
    val sections: List<SectionInfo>? = null  // 기록통장용
) {
    companion object {
        fun from(account: Account, balance: Balance? = null): AccountResult
    }

    data class BalanceInfo(
        val ledgerBalance: BigDecimal,
        val availableBalance: BigDecimal,
        val holdBalance: BigDecimal,
        val interestAccumulated: BigDecimal
    )

    data class SectionInfo(
        val id: Long,
        val name: String,
        val currentAmount: BigDecimal,
        val targetAmount: BigDecimal?
    )
}
```

### 5.3 전체 데이터 흐름

```
[Client]
    ↓ HTTP Request (JSON)
[Controller]
    ↓ AccountDto.CreateRequest
[AccountService]
    ↓ AccountCommand.Create (toCommand())
[AccountDataService]
    ↓ Account Entity (toEntity())
[AccountRepository]
    ↓ Database
[AccountRepository]
    ↑ Account Entity
[AccountDataService]
    ↑ AccountResult (from())
[AccountService]
    ↑ AccountDto.Response (from())
[Controller]
    ↑ HTTP Response (JSON)
[Client]
```

## 6. 주요 비즈니스 로직

### 6.1 계좌 개설 (Create Account)

#### 입출금통장 개설
1. 계좌번호 생성 및 중복 확인
2. Account Entity 생성
3. 초기 Balance 생성 (잔액 0, 금리 0.10%)
4. AccountLimit 기본값 설정
5. AccountHolder 연결 (PRIMARY 역할)
6. AccountStatusHistory 기록
7. 세이프박스 자동 생성 옵션 제공

#### 모임통장 개설
1. 기본 입출금통장 개설 프로세스 수행
2. GroupMeeting 정보 생성
3. 모임주를 MEETING_OWNER 역할로 AccountHolder 추가
4. 초대 링크 생성

#### 우리아이통장 개설
1. 부모의 입출금통장 확인
2. 자녀 명의로 Account 생성
3. 부모를 PARENT 역할로 AccountHolder 추가
4. 자녀를 CHILD 역할로 AccountHolder 추가
5. parentAccountId 연결
6. 초기 한도 설정 (부모가 조정 가능)

#### 기록통장 개설
1. 연결할 입출금통장 확인
2. Account 생성 (금리 1.60%)
3. linkedAccountId에 입출금통장 연결
4. 기본 섹션 1개 생성
5. 한도: 섹션당 월 1천만원 설정

#### 세이프박스 개설
1. 연결할 입출금통장 또는 개인사업자통장 확인
2. 계좌당 1개 제한 확인
3. Account 생성 (금리 1.60%, 매일 복리)
4. linkedAccountId 연결
5. 입금 한도: 기본 1천만원 설정

### 6.2 잔액 조회 (Get Balance)

1. 계좌번호로 Account 조회
2. Balance 조회
3. 사용 가능 잔액 계산: `availableBalance = ledgerBalance - holdBalance`
4. 누적 이자 포함 반환

### 6.3 거래 처리 (Transaction)

#### 입금 처리
1. 계좌 상태 확인 (ACTIVE, DORMANT 허용)
2. Balance 업데이트 (낙관적 락)
3. LedgerEntry 기록 (CREDIT)
4. 기록통장: 섹션별 금액 업데이트
5. 우리아이통장: 메모 저장 옵션
6. 이자 계산 트리거

#### 출금 처리
1. 계좌 상태 확인 (ACTIVE만 허용)
2. 한도 확인 (AccountLimit)
3. 잔액 확인 (availableBalance >= amount)
4. Hold 확인 및 처리
5. Balance 업데이트
6. LedgerEntry 기록 (DEBIT)
7. 기록통장: 섹션별 금액 차감

#### 이체 처리
1. 출금 계좌 처리 (출금 프로세스)
2. 입금 계좌 처리 (입금 프로세스)
3. 트랜잭션 원자성 보장
4. 수수료 면제 확인
5. 한도 사용량 업데이트 (LimitUsage)

### 6.4 이자 계산 및 지급

#### 일일 이자 계산
```kotlin
// 매일 자정 실행
fun calculateDailyInterest(accountId: Long) {
    val balance = balanceRepository.findByAccountId(accountId)
    val account = accountRepository.findById(accountId)

    // 일 이자 = 잔액 × (연 이율 / 365)
    val dailyInterest = balance.ledgerBalance * (account.interestRate / 365 / 100)

    balance.interestAccumulated += dailyInterest
    balanceRepository.save(balance)
}
```

#### 월말 이자 지급
```kotlin
// 매월 넷째 금요일 실행
fun payMonthlyInterest(accountId: Long) {
    val balance = balanceRepository.findByAccountId(accountId)
    val interestAmount = balance.interestAccumulated

    // 세금 공제 (15.4%)
    val taxAmount = interestAmount * 0.154
    val netInterest = interestAmount - taxAmount

    // 잔액에 이자 추가
    balance.ledgerBalance += netInterest
    balance.interestAccumulated = BigDecimal.ZERO

    // 원장 기록
    ledgerEntryRepository.save(LedgerEntry(
        accountId = accountId,
        entryType = EntryType.CREDIT,
        amount = netInterest,
        description = "이자 지급"
    ))

    // 이자 지급 이력
    interestPaymentRepository.save(InterestPayment(...))
}
```

#### 세이프박스 매일 복리
```kotlin
// 세이프박스: 매일 이자 지급 및 원금 추가
fun payDailyCompoundInterest(safeboxId: Long) {
    val balance = balanceRepository.findByAccountId(safeboxId)
    val account = accountRepository.findById(safeboxId)

    val dailyInterest = balance.ledgerBalance * (account.interestRate / 365 / 100)
    val taxAmount = dailyInterest * 0.154
    val netInterest = dailyInterest - taxAmount

    // 매일 원금에 추가 (복리)
    balance.ledgerBalance += netInterest

    ledgerEntryRepository.save(LedgerEntry(
        accountId = safeboxId,
        entryType = EntryType.CREDIT,
        amount = netInterest,
        description = "일일 복리 이자"
    ))
}
```

### 6.5 계좌 상태 변경 (Change Status)

1. 현재 상태 확인
2. 상태 전환 가능 여부 검증
3. Account.status 업데이트
4. AccountStatusHistory 기록
5. 상태별 후처리:
   - DORMANT: 출금/이체 제한
   - FROZEN: 모든 거래 중단
   - CLOSED: 잔액 0 확인, 연결 해제

### 6.6 모임통장 특화 기능

#### 모임원 초대
1. 초대 링크 생성
2. 카카오톡 초대장 발송
3. 수락 시 AccountHolder 추가 (MEETING_MEMBER)
4. 최대 인원 확인 (100명)

#### 회비 관리
1. 월 회비 설정
2. 매월 지정일에 알림 발송
3. 회비 납부 여부 추적
4. AI 모임총무: 지출 분석 및 요약

### 6.7 기록통장 특화 기능

#### 섹션 관리
1. 최대 10개 섹션 생성 가능
2. 섹션별 목표 금액 설정
3. 섹션별 현재 금액 추적
4. 커버 이미지, 메모 관리

#### 저축 규칙 설정
1. 섹션당 최대 20개 규칙
2. 규칙별 금액, 빈도 설정
3. 자동 이체 실행
4. 규칙 활성화/비활성화

## 7. 보안 및 제약사항

### 7.1 계좌 상태별 제약

| 상태 | 입금 | 출금 | 이체 | 조회 | 이자 |
|------|------|------|------|------|------|
| ACTIVE | ✓ | ✓ | ✓ | ✓ | ✓ |
| DORMANT | ✓ | ✗ | ✗ | ✓ | ✓ |
| FROZEN | ✗ | ✗ | ✗ | ✓ | ✗ |
| CLOSED | ✗ | ✗ | ✗ | ✓ | ✗ |

### 7.2 한도 관리

#### 입출금통장
- 1회 이체 한도: 기본 500만원
- 일일 이체 한도: 기본 1천만원
- LimitUsage를 통해 실시간 사용량 추적

#### 기록통장
- 섹션당 월 입금 한도: 1천만원
- 총 10개 섹션 × 1천만원 = 월 최대 1억원

#### 세이프박스
- 기본 입금 한도: 1천만원
- 증액 가능: 최대 1억원

### 7.3 동시성 제어

#### Balance 업데이트
```kotlin
@Entity
class Balance {
    @Version
    var version: Long = 0  // 낙관적 락

    // 또는

    @Lock(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락
    fun updateBalance(amount: BigDecimal) { ... }
}
```

#### 거래 원자성
```kotlin
@Transactional(isolation = Isolation.SERIALIZABLE)
fun transfer(fromAccount: String, toAccount: String, amount: BigDecimal) {
    // 출금과 입금을 하나의 트랜잭션으로 처리
}
```

### 7.4 예금자 보호

- 예금보험공사를 통한 1인당 1억원까지 보호
- 계좌별이 아닌 고객별 합산
- 이자 포함 보호

### 7.5 개인정보 보호

#### 우리아이통장
- 법정대리인 확인 필수
- 만 19세 이상 부모만 개설 가능
- 거래 제한: 블루뱅크 앱 내에서만 가능

#### 모임통장
- 모임원 정보 공개 범위 설정
- 게시판 권한 관리
- 탈퇴 시 데이터 처리

## 8. 확장 가능성

### 8.1 상품별 추가 기능

#### 입출금통장
- 저금통 (잔돈 자동 적립)
- 자동 이체 관리
- 해외 송금 연동

#### 모임통장
- AI 모임총무 고도화
  - 지출 예측
  - 회비 추천
  - 이상 거래 탐지
- 모임 캘린더 연동
- 모임 투표 기능

#### 우리아이통장
- 금융 교육 콘텐츠
- 용돈 기입장
- 소비 패턴 리포트
- 목표 설정 및 달성 보상

#### 기록통장
- AI 기반 지출 분석
- 카테고리 자동 분류
- 목표 달성률 시각화
- SNS 공유 기능

#### 세이프박스
- 목표 금액 설정
- 자동 이체 저축
- 목표 달성 알림

### 8.2 이벤트 기반 아키텍처

```kotlin
sealed class AccountEvent {
    data class AccountCreated(val accountId: Long, val productType: ProductType)
    data class BalanceChanged(val accountId: Long, val newBalance: BigDecimal)
    data class TransactionCompleted(val accountId: Long, val transactionId: String)
    data class StatusChanged(val accountId: Long, val newStatus: AccountStatus)
    data class InterestPaid(val accountId: Long, val amount: BigDecimal)
    data class LimitExceeded(val accountId: Long, val attemptedAmount: BigDecimal)
    data class MemberJoined(val meetingId: Long, val memberId: Long)
    data class SectionCreated(val accountId: Long, val sectionId: Long)
}
```

### 8.3 연계 서비스 확장

- 체크카드/신용카드 연동
- 대출 상품 연결
- 적금/예금 상품 연결
- 보험 상품 연계
- 투자 상품 (주식, 펀드) 연동

## 9. 성능 최적화

### 9.1 캐싱 전략

```kotlin
@Cacheable(value = "account", key = "#accountNumber")
fun findByAccountNumber(accountNumber: String): Account?

@CacheEvict(value = "account", key = "#accountNumber")
fun updateAccount(accountNumber: String, command: AccountCommand.Modify)
```

### 9.2 인덱싱

```sql
-- 계좌번호 조회 최적화
CREATE UNIQUE INDEX idx_account_number ON account(account_number);

-- 고객별 계좌 조회 최적화
CREATE INDEX idx_account_holder_customer_id ON account_holder(customer_id);

-- 거래 내역 조회 최적화
CREATE INDEX idx_ledger_entry_account_occurred ON ledger_entry(account_id, occurred_at DESC);

-- 모임 조회 최적화
CREATE INDEX idx_group_meeting_account ON group_meeting(account_id);
```

### 9.3 배치 처리

```kotlin
// 이자 계산: 매일 자정 배치
@Scheduled(cron = "0 0 0 * * *")
fun calculateAllDailyInterest() {
    val activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE)
    activeAccounts.chunked(1000).forEach { chunk ->
        chunk.parallelStream().forEach { account ->
            interestService.calculateDailyInterest(account.id)
        }
    }
}

// 이자 지급: 매월 넷째 금요일
@Scheduled(cron = "0 0 0 * * FRI")
fun payMonthlyInterest() {
    if (isLastFridayOfMonth()) {
        val accounts = accountRepository.findAll()
        accounts.forEach { account ->
            interestService.payMonthlyInterest(account.id)
        }
    }
}
```

## 10. 모니터링 및 알림

### 10.1 주요 메트릭

- 계좌 개설 수 (상품별)
- 일일 거래량 (금액, 건수)
- 평균 잔액
- 이자 지급 총액
- 에러율 (거래 실패, 한도 초과 등)

### 10.2 알림

- 거래 발생 알림 (푸시)
- 한도 초과 알림
- 휴면 계좌 전환 예정 알림
- 이자 지급 알림
- 모임통장: 회비 알림, 모임원 가입 알림
- 기록통장: 목표 달성 알림

## 11. 기술 스택

- **언어**: Kotlin 1.9+
- **프레임워크**: Spring Boot 3.2+
- **ORM**: JPA (Hibernate)
- **데이터베이스**:
  - 테스트: H2
  - 운영: PostgreSQL 15+ 또는 MySQL 8+
- **캐싱**: Redis
- **메시징**: Kafka (이벤트 발행)
- **배치**: Spring Batch
- **빌드**: Gradle (Kotlin DSL)
- **모니터링**: Prometheus, Grafana
- **로깅**: ELK Stack

## 12. API 설계 (예시)

### 12.1 계좌 API

```
POST   /api/v1/accounts                    # 계좌 개설
GET    /api/v1/accounts/{accountNumber}    # 계좌 조회
PATCH  /api/v1/accounts/{accountNumber}    # 계좌 수정
DELETE /api/v1/accounts/{accountNumber}    # 계좌 해지

GET    /api/v1/accounts/{accountNumber}/balance        # 잔액 조회
GET    /api/v1/accounts/{accountNumber}/transactions   # 거래 내역
POST   /api/v1/accounts/{accountNumber}/transfer       # 이체
```

### 12.2 기록통장 API

```
POST   /api/v1/accounts/{accountNumber}/sections           # 섹션 생성
GET    /api/v1/accounts/{accountNumber}/sections           # 섹션 목록
PATCH  /api/v1/accounts/{accountNumber}/sections/{id}      # 섹션 수정
DELETE /api/v1/accounts/{accountNumber}/sections/{id}      # 섹션 삭제

POST   /api/v1/sections/{sectionId}/rules                  # 저축 규칙 생성
GET    /api/v1/sections/{sectionId}/rules                  # 규칙 목록
```

### 12.3 모임통장 API

```
POST   /api/v1/accounts/{accountNumber}/meeting            # 모임 정보 설정
GET    /api/v1/accounts/{accountNumber}/meeting/members    # 모임원 목록
POST   /api/v1/accounts/{accountNumber}/meeting/invite     # 모임원 초대
DELETE /api/v1/accounts/{accountNumber}/meeting/members/{id} # 모임원 제거
GET    /api/v1/accounts/{accountNumber}/meeting/fees       # 회비 현황
```

### 12.4 우리아이통장 API

```
POST   /api/v1/accounts/{accountNumber}/child             # 우리아이통장 생성
GET    /api/v1/accounts/{accountNumber}/memos             # 메모 목록
POST   /api/v1/transactions/{transactionId}/memo         # 거래에 메모 추가
PATCH  /api/v1/accounts/{accountNumber}/limits            # 한도 조정
```

## 13. 참고 자료

- [블루뱅크 입출금통장](https://www.kakaobank.com/products/withdrawal)
- [블루뱅크 모임통장](https://www.kakaobank.com/products/moim)
- [블루뱅크 우리아이통장](https://www.kakaobank.com/products/childwithdrawl)
- [블루뱅크 기록통장](https://www.kakaobank.com/products/recordBook/bias)
- [블루뱅크 세이프박스](https://www.kakaobank.com/products/safeboxes)
- DDD (Domain-Driven Design) 패턴
- Clean Architecture 원칙
- CQRS (Command-Query Responsibility Segregation) 패턴
- Event Sourcing 패턴