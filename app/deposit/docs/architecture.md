# Deposit Service Architecture

## 1. 개요

Deposit Service는 블루뱅크의 예금/적금 상품을 지원하는 마이크로서비스입니다. 정기예금, 자유적금, 저금통, 26주적금, 한달적금, 우리아이적금 등 6개 수신(Deposit) 상품의 가입, 입금, 출금, 만기, 이자 계산 등 전체 라이프사이클을 관리합니다.

### 1.1 지원 예금/적금 상품

#### 정기예금 (Fixed Deposit)
1. **정기예금**: 목돈을 일정 기간 맡기고 만기 시 원금과 이자를 받는 상품
   - 최소금액: 100만원
   - 기간: 1~36개월
   - 금리: 2.60% ~ 2.95% (기간별 차등)
   - 중도인출: 최대 2회 가능
   - 자동재예치: 최대 5회

#### 적금 (Savings)
2. **자유적금**: 매일/매주/매월 자유롭게 적립
   - 월 납입한도: 1천원 ~ 300만원
   - 기간: 6~36개월
   - 기본금리: 3.15%
   - 우대금리: +0.20% (자동이체 조건 충족 시)
   - 중도인출: 최대 2회 가능

3. **저금통 (Coinbox)**: 자동으로 소액 저축
   - 잔돈 모으기: 평일 1,000원 미만 자동 저축
   - AI 자동저축: 매주 토요일 1,000~10,000원 제안
   - 캐시백 적립: 브랜드 쿠폰 캐시백 자동 입금
   - 금리: 4.00%
   - 최대잔액: 10만원
   - 이자지급: 매월 넷째 금요일

4. **26주적금**: 매주 증액되는 챌린지형 적금
   - 초기금액: 1천원 ~ 1만원
   - 증액방식: 매주 초기금액씩 증가 (1주차: 1배, 2주차: 2배...)
   - 기본금리: 2.00%
   - 최대금리: 5.00% (26주 완납 시)
   - 우대금리: 7주차 +1.00%p, 26주차 +2.00%p
   - 기간: 6개월 (26주)

5. **한달적금 (M1 Savings)**: 31일 동안 매일 저축
   - 일 납입: 100원 ~ 30,000원
   - 자동이체 없음 (매일 수동 입금)
   - 기본금리: 0.50%
   - 일별 우대: +0.10% (최대 3.10%)
   - 목표달성 우대: 5회, 10회, 15회, 20회, 25회, 31회 (최대 2.40%)
   - 최대금리: 6.00%
   - 기간: 31일
   - 1인 최대 3계좌

6. **우리아이적금 (Child Savings)**: 부모가 자녀 명의로 개설
   - 대상: 만 17세 미만 자녀
   - 월 납입: 1천원 ~ 20만원
   - 기간: 12개월
   - 기본금리: 3.00%
   - 우대금리: +4.00%p (자동이체 6개월 이상)
   - 최대금리: 7.00%
   - 가족 공동 참여 가능
   - 만 19세까지 자동 연장

---

## 2. 도메인 모델

### 2.1 핵심 엔티티

#### 2.1.1 Deposit (예금/적금)
예금/적금의 핵심 정보를 담는 메인 엔티티

```kotlin
@Entity
class Deposit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    var depositNumber: String,                 // 예금/적금 번호
    var customerId: Long,                      // 고객 ID
    var accountId: Long,                       // 연결된 입출금 계좌 ID

    // 상품 유형
    @Enumerated(EnumType.STRING)
    var productType: DepositProductType,       // 상품 유형

    // 금액 정보
    var principalAmount: BigDecimal,           // 원금 (정기예금) 또는 목표금액 (적금)
    var currentBalance: BigDecimal,            // 현재 잔액
    var accumulatedInterest: BigDecimal = BigDecimal.ZERO,  // 누적 이자

    // 금리 정보
    var baseRate: BigDecimal,                  // 기본 금리 (연 %)
    var bonusRate: BigDecimal = BigDecimal.ZERO,  // 우대 금리 (연 %)
    var appliedRate: BigDecimal,               // 적용 금리 (기본 + 우대)

    // 기간 정보
    var contractPeriod: Int,                   // 계약 기간 (일/주/월)
    @Enumerated(EnumType.STRING)
    var periodUnit: PeriodUnit,                // 기간 단위
    var startDate: LocalDate,                  // 시작일
    var maturityDate: LocalDate,               // 만기일

    // 납입 정보 (적금)
    var monthlyPayment: BigDecimal? = null,    // 월 납입액 (정액적금)
    var minMonthlyPayment: BigDecimal? = null, // 최소 월 납입액 (자유적금)
    var maxMonthlyPayment: BigDecimal? = null, // 최대 월 납입액 (자유적금)
    var totalDepositCount: Int = 0,            // 총 납입 횟수

    // 자동이체 정보
    var autoTransferEnabled: Boolean = false,
    var autoTransferDay: Int? = null,          // 자동이체 일자
    var autoTransferAmount: BigDecimal? = null, // 자동이체 금액

    // 상태
    @Enumerated(EnumType.STRING)
    var status: DepositStatus,

    // 중도인출 정보
    var earlyWithdrawalCount: Int = 0,         // 중도인출 횟수
    var maxEarlyWithdrawals: Int = 2,          // 최대 중도인출 횟수

    // 자동재예치 정보 (정기예금)
    var autoRenewalEnabled: Boolean = false,
    var renewalCount: Int = 0,                 // 재예치 횟수
    var maxRenewals: Int = 5,                  // 최대 재예치 횟수

    // 26주적금 전용
    var initialWeeklyAmount: BigDecimal? = null,  // 초기 주당 금액
    var currentWeek: Int = 0,                  // 현재 주차

    // 한달적금 전용
    var currentDay: Int = 0,                   // 현재 일차 (1~31)
    var depositDaysCompleted: Int = 0,         // 완료한 일수

    // 우리아이적금 전용
    var childId: Long? = null,                 // 자녀 ID
    var parentIds: String? = null,             // 부모 IDs (콤마 구분)

    // 저금통 전용
    var maxBalance: BigDecimal? = null,        // 최대 잔액 (저금통: 10만원)
    var spareChangeEnabled: Boolean = false,   // 잔돈 모으기 활성화
    var aiSavingsEnabled: Boolean = false,     // AI 자동저축 활성화

    // 이자 지급
    var lastInterestPaymentDate: LocalDate? = null,  // 마지막 이자 지급일
    var interestPaymentDay: Int? = null,       // 이자 지급일 (매월)

    // 세금
    var isTaxFree: Boolean = false,            // 비과세 여부

    // 감사
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 예금/적금 개설
     */
    fun activate() {
        require(status == DepositStatus.PENDING) { "대기 중인 상품만 활성화 가능합니다" }
        this.status = DepositStatus.ACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 입금 (적금)
     */
    fun deposit(amount: BigDecimal) {
        require(status == DepositStatus.ACTIVE) { "활성 상태에서만 입금 가능합니다" }
        this.currentBalance = currentBalance + amount
        this.totalDepositCount++
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 중도인출
     */
    fun earlyWithdraw(amount: BigDecimal) {
        require(status == DepositStatus.ACTIVE) { "활성 상태에서만 출금 가능합니다" }
        require(earlyWithdrawalCount < maxEarlyWithdrawals) { "중도인출 횟수 초과" }
        require(amount <= currentBalance) { "잔액 부족" }

        this.currentBalance = currentBalance - amount
        this.earlyWithdrawalCount++
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 만기 처리
     */
    fun mature(totalInterest: BigDecimal) {
        require(status == DepositStatus.ACTIVE) { "활성 상태만 만기 처리 가능합니다" }
        this.status = DepositStatus.MATURED
        this.accumulatedInterest = totalInterest
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 해지
     */
    fun terminate() {
        require(status == DepositStatus.ACTIVE) { "활성 상태만 해지 가능합니다" }
        this.status = DepositStatus.TERMINATED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 이자 지급
     */
    fun payInterest(interest: BigDecimal) {
        this.accumulatedInterest = accumulatedInterest + interest
        this.currentBalance = currentBalance + interest
        this.lastInterestPaymentDate = LocalDate.now()
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 주차 증가 (26주적금)
     */
    fun incrementWeek() {
        this.currentWeek++
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 일차 증가 (한달적금)
     */
    fun incrementDay() {
        this.currentDay++
        this.depositDaysCompleted++
        this.updatedAt = LocalDateTime.now()
    }
}
```

#### 2.1.2 DepositTransaction (입출금 내역)
예금/적금의 입출금 거래 내역

```kotlin
@Entity
class DepositTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var depositId: Long,
    var customerId: Long,

    @Enumerated(EnumType.STRING)
    var transactionType: DepositTransactionType,  // 거래 유형

    var amount: BigDecimal,                    // 거래 금액
    var balanceAfter: BigDecimal,              // 거래 후 잔액

    var description: String? = null,           // 거래 설명

    // 자동이체 정보
    var isAutoTransfer: Boolean = false,

    // 26주적금 전용
    var weekNumber: Int? = null,               // 주차

    // 한달적금 전용
    var dayNumber: Int? = null,                // 일차

    var transactionDate: LocalDateTime = LocalDateTime.now()
)
```

#### 2.1.3 InterestPayment (이자 지급 내역)
이자 지급 이력

```kotlin
@Entity
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var depositId: Long,
    var customerId: Long,

    var interestAmount: BigDecimal,            // 이자 금액
    var appliedRate: BigDecimal,               // 적용 금리
    var calculationPeriodStart: LocalDate,     // 계산 기간 시작
    var calculationPeriodEnd: LocalDate,       // 계산 기간 종료
    var principalBalance: BigDecimal,          // 계산 기준 원금

    var taxAmount: BigDecimal = BigDecimal.ZERO,  // 세금
    var netInterest: BigDecimal,               // 세후 이자

    var paymentDate: LocalDate,                // 지급일

    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

---

### 2.2 Enum 정의

```kotlin
enum class DepositProductType(val description: String) {
    FIXED_DEPOSIT("정기예금"),
    FREE_SAVINGS("자유적금"),
    COINBOX("저금통"),
    WEEKS_26_SAVINGS("26주적금"),
    M1_SAVINGS("한달적금"),
    CHILD_SAVINGS("우리아이적금")
}

enum class DepositStatus(val description: String) {
    PENDING("대기"),
    ACTIVE("활성"),
    MATURED("만기"),
    TERMINATED("해지"),
    SUSPENDED("정지")
}

enum class DepositTransactionType(val description: String) {
    DEPOSIT("입금"),
    WITHDRAWAL("출금"),
    INTEREST_PAYMENT("이자 지급"),
    EARLY_WITHDRAWAL("중도인출"),
    MATURITY("만기 지급"),
    TERMINATION("해지 지급"),
    AUTO_TRANSFER("자동이체"),
    SPARE_CHANGE("잔돈 모으기"),
    AI_SAVINGS("AI 자동저축"),
    CASHBACK("캐시백")
}

enum class PeriodUnit(val description: String) {
    DAY("일"),
    WEEK("주"),
    MONTH("월")
}
```

---

## 3. 서비스 레이어 설계

### 3.1 DataService (data 모듈)

#### DepositDataService
트랜잭션 처리를 담당하는 데이터 레이어

```kotlin
@Service
class DepositDataService(
    private val depositRepository: DepositRepository,
    private val depositTransactionRepository: DepositTransactionRepository,
    private val interestPaymentRepository: InterestPaymentRepository
) {

    @Transactional
    fun createDeposit(command: DepositCommand.Create): DepositResult {
        val deposit = command.toEntity()
        val savedDeposit = depositRepository.save(deposit)
        return DepositResult.from(savedDeposit)
    }

    @Transactional
    fun activateDeposit(depositId: Long): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow()
        deposit.activate()
        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun deposit(depositId: Long, amount: BigDecimal, description: String?): DepositTransactionResult {
        val deposit = depositRepository.findById(depositId).orElseThrow()
        deposit.deposit(amount)
        depositRepository.save(deposit)

        val transaction = DepositTransaction(
            depositId = depositId,
            customerId = deposit.customerId,
            transactionType = DepositTransactionType.DEPOSIT,
            amount = amount,
            balanceAfter = deposit.currentBalance,
            description = description
        )
        val savedTransaction = depositTransactionRepository.save(transaction)

        return DepositTransactionResult.from(savedTransaction)
    }

    @Transactional
    fun earlyWithdraw(depositId: Long, amount: BigDecimal): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow()
        deposit.earlyWithdraw(amount)

        // 거래 내역 생성
        depositTransactionRepository.save(
            DepositTransaction(
                depositId = depositId,
                customerId = deposit.customerId,
                transactionType = DepositTransactionType.EARLY_WITHDRAWAL,
                amount = amount,
                balanceAfter = deposit.currentBalance
            )
        )

        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun matureDeposit(depositId: Long, totalInterest: BigDecimal): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow()
        deposit.mature(totalInterest)
        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun payInterest(depositId: Long, interest: BigDecimal): InterestPaymentResult {
        val deposit = depositRepository.findById(depositId).orElseThrow()

        val tax = calculateTax(interest, deposit.isTaxFree)
        val netInterest = interest - tax

        deposit.payInterest(netInterest)
        depositRepository.save(deposit)

        val interestPayment = InterestPayment(
            depositId = depositId,
            customerId = deposit.customerId,
            interestAmount = interest,
            appliedRate = deposit.appliedRate,
            calculationPeriodStart = deposit.lastInterestPaymentDate ?: deposit.startDate,
            calculationPeriodEnd = LocalDate.now(),
            principalBalance = deposit.currentBalance,
            taxAmount = tax,
            netInterest = netInterest,
            paymentDate = LocalDate.now()
        )
        val saved = interestPaymentRepository.save(interestPayment)

        return InterestPaymentResult.from(saved)
    }

    private fun calculateTax(interest: BigDecimal, isTaxFree: Boolean): BigDecimal {
        if (isTaxFree) return BigDecimal.ZERO
        // 이자소득세 15.4% (소득세 14% + 지방소득세 1.4%)
        return interest.multiply(BigDecimal("0.154")).setScale(0, RoundingMode.DOWN)
    }
}
```

---

## 4. API 설계

### 4.1 예금/적금 가입 API

#### POST /api/deposits
예금/적금 가입

**Request:**
```json
{
  "customerId": 1,
  "accountId": 100,
  "productType": "FREE_SAVINGS",
  "principalAmount": 0,
  "contractPeriod": 12,
  "periodUnit": "MONTH",
  "minMonthlyPayment": 100000,
  "maxMonthlyPayment": 500000,
  "baseRate": 3.15,
  "autoTransferEnabled": true,
  "autoTransferDay": 25,
  "autoTransferAmount": 300000
}
```

### 4.2 입금 API

#### POST /api/deposits/{depositId}/deposit
입금

**Request:**
```json
{
  "amount": 100000,
  "description": "3월 정기납입"
}
```

### 4.3 조회 API

#### GET /api/deposits/{depositId}
예금/적금 상세 조회

#### GET /api/deposits/customer/{customerId}
고객의 예금/적금 목록

---

## 5. 비즈니스 로직

### 5.1 이자 계산

#### 정기예금 이자 계산
```kotlin
fun calculateFixedDepositInterest(
    principal: BigDecimal,
    annualRate: BigDecimal,
    days: Int
): BigDecimal {
    return principal
        .multiply(annualRate)
        .multiply(BigDecimal(days))
        .divide(BigDecimal("36500"), 0, RoundingMode.DOWN)
}
```

#### 자유적금 이자 계산
```kotlin
fun calculateFreeSavingsInterest(
    transactions: List<DepositTransaction>,
    annualRate: BigDecimal,
    maturityDate: LocalDate
): BigDecimal {
    return transactions.sumOf { transaction ->
        val days = ChronoUnit.DAYS.between(transaction.transactionDate.toLocalDate(), maturityDate)
        transaction.amount
            .multiply(annualRate)
            .multiply(BigDecimal(days))
            .divide(BigDecimal("36500"), 0, RoundingMode.DOWN)
    }
}
```

### 5.2 우대금리 적용

#### 자유적금 우대금리
```kotlin
fun calculateBonusRate(
    autoTransferCount: Int,
    totalPeriodMonths: Int
): BigDecimal {
    // 자동이체 6개월 이상 + 만기 시 +0.20%p
    return if (autoTransferCount >= totalPeriodMonths / 2) {
        BigDecimal("0.20")
    } else {
        BigDecimal.ZERO
    }
}
```

#### 26주적금 우대금리
```kotlin
fun calculate26WeeksBonusRate(currentWeek: Int): BigDecimal {
    return when {
        currentWeek >= 26 -> BigDecimal("3.00")  // +1.00%p (7주) + 2.00%p (26주)
        currentWeek >= 7 -> BigDecimal("1.00")   // +1.00%p (7주)
        else -> BigDecimal.ZERO
    }
}
```

---

## 6. 참고 자료

- 카카오뱅크 저금통: https://www.kakaobank.com/products/coinbox
- 카카오뱅크 정기예금: https://www.kakaobank.com/products/deposit
- 카카오뱅크 자유적금: https://www.kakaobank.com/products/savings
- 카카오뱅크 26주적금: https://www.kakaobank.com/products/26weeks
- 카카오뱅크 한달적금: https://www.kakaobank.com/products/m1savings
- 카카오뱅크 우리아이적금: https://www.kakaobank.com/products/childsavings
