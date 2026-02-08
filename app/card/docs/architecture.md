# Card Service Architecture

## 1. 개요

Card Service는 블루뱅크의 다양한 카드 상품을 지원하는 마이크로서비스입니다. 체크카드, 신용카드, 모임체크카드, PLCC(제휴신용카드) 등의 카드 발급, 관리, 사용 내역 조회, 혜택 관리 등 카드의 전체 라이프사이클을 관리합니다.

### 1.1 지원 카드 상품

#### 체크카드 (Debit Card)
1. **프렌즈 체크카드 (Friends Check Card)**: 평일 0.2%, 주말/공휴일 0.4% 캐시백
   - 연회비 무료
   - 최소 사용금액 없음
   - 후불 교통카드 기능
   - 해외 사용 가능
   - 무료 ATM 출금
   - 다양한 카카오프렌즈 디자인

2. **모임 체크카드 (Moim Check Card)**: 모임통장 전용 체크카드
   - 연회비 무료, 발급 수수료 2,000원
   - 랜덤 캐시백 (300원 또는 3,000원)
   - 5만원 이상 결제 시 일 1회
   - MasterCard 제휴
   - 일 한도 600만원, 월 한도 2,000만원

#### 신용카드 (Credit Card)
3. **줍줍 신용카드 (PLCC - Shinhan Partnership)**: 신한카드 제휴
   - 국내외 결제 1% 캐시백 (월 최대 2만원)
   - 결제일 5일 이내 추가 1% 캐시백 (월 최대 2만원)
   - 월 30만원 이상 결제 시 카카오톡 이모티콘+ 1개월 무료
   - 연회비 18,000원

4. **카드 플랫폼 (Card Platform)**: 다양한 제휴 신용카드 비교 및 신청
   - 신한, 국민, 삼성, 롯데, 현대카드 등 주요 카드사 제휴
   - 혜택 비교 및 이벤트 참여
   - 앱 내 통합 관리

---

## 2. 도메인 모델

### 2.1 핵심 엔티티

#### 2.1.1 Card (카드)
카드의 핵심 정보를 담는 메인 엔티티

```kotlin
@Entity
class Card(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    var cardNumber: String,                    // 카드 번호 (암호화)
    var cardNumberMasked: String,              // 마스킹된 카드번호 (1234-****-****-5678)
    var customerId: Long,                      // 고객 ID
    var accountId: Long,                       // 연결된 계좌 ID

    // 카드 유형
    @Enumerated(EnumType.STRING)
    var cardType: CardType,                    // 카드 유형 (체크/신용)

    @Enumerated(EnumType.STRING)
    var productType: CardProductType,          // 상품 유형 (프렌즈/모임/줍줍 등)

    // 카드 정보
    var cardholderName: String,                // 카드소유자명
    var issueDate: LocalDate,                  // 발급일
    var expiryDate: LocalDate,                 // 유효기간
    var cvv: String,                           // CVV (암호화)

    // 카드 상태
    @Enumerated(EnumType.STRING)
    var status: CardStatus,                    // 카드 상태

    // 한도 정보
    var dailyLimit: BigDecimal,                // 일 한도
    var monthlyLimit: BigDecimal,              // 월 한도
    var dailyUsed: BigDecimal = BigDecimal.ZERO,      // 일 사용액
    var monthlyUsed: BigDecimal = BigDecimal.ZERO,    // 월 사용액

    // 카드 디자인
    var designCode: String,                    // 디자인 코드 (프렌즈캐릭터, 컬러 등)
    var customText: String? = null,            // 커스텀 텍스트 (선택)

    // 부가 서비스
    var hasTransitCard: Boolean = false,       // 후불 교통카드 기능
    var hasOverseasUsage: Boolean = true,      // 해외 사용 가능
    var isEnabled: Boolean = true,             // 카드 사용 활성화 (앱에서 토글)

    // 신용카드 전용
    var creditLimit: BigDecimal? = null,       // 신용한도
    var availableCredit: BigDecimal? = null,   // 가용한도
    var annualFee: BigDecimal = BigDecimal.ZERO,      // 연회비
    var nextPaymentDate: LocalDate? = null,    // 다음 결제일

    // 제휴사 정보 (PLCC)
    var partnerCompany: String? = null,        // 제휴사 (신한카드 등)
    var partnerCardId: String? = null,         // 제휴사 카드 ID

    // 모임카드 전용
    var moimAccountId: Long? = null,           // 모임통장 ID

    // 발급 정보
    var applicationId: Long? = null,           // 신청서 ID
    var issueFee: BigDecimal = BigDecimal.ZERO,       // 발급 수수료

    // 감사
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 비즈니스 로직
    fun activate() {
        require(status == CardStatus.ISSUED) { "발급된 카드만 활성화 가능합니다" }
        this.status = CardStatus.ACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    fun suspend(reason: String) {
        require(status == CardStatus.ACTIVE) { "활성화된 카드만 정지 가능합니다" }
        this.status = CardStatus.SUSPENDED
        this.updatedAt = LocalDateTime.now()
    }

    fun terminate() {
        require(status != CardStatus.TERMINATED) { "이미 해지된 카드입니다" }
        this.status = CardStatus.TERMINATED
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    fun enableUsage() {
        require(status == CardStatus.ACTIVE) { "활성화된 카드만 사용 설정 가능합니다" }
        this.isEnabled = true
        this.updatedAt = LocalDateTime.now()
    }

    fun disableUsage() {
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    fun validateDailyLimit(amount: BigDecimal): Boolean {
        return dailyUsed + amount <= dailyLimit
    }

    fun validateMonthlyLimit(amount: BigDecimal): Boolean {
        return monthlyUsed + amount <= monthlyLimit
    }

    fun addUsage(amount: BigDecimal) {
        this.dailyUsed = dailyUsed + amount
        this.monthlyUsed = monthlyUsed + amount
        this.updatedAt = LocalDateTime.now()
    }

    fun resetDailyUsage() {
        this.dailyUsed = BigDecimal.ZERO
        this.updatedAt = LocalDateTime.now()
    }

    fun resetMonthlyUsage() {
        this.monthlyUsed = BigDecimal.ZERO
        this.updatedAt = LocalDateTime.now()
    }
}
```

#### 2.1.2 CardApplication (카드 신청)
카드 발급 신청 정보

```kotlin
@Entity
class CardApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var customerId: Long,
    var accountId: Long,                       // 연결할 계좌

    @Enumerated(EnumType.STRING)
    var cardType: CardType,

    @Enumerated(EnumType.STRING)
    var productType: CardProductType,

    // 신청자 정보
    var applicantName: String,
    var residentNumber: String,                // 주민등록번호 (암호화)
    var phoneNumber: String,
    var email: String? = null,
    var address: String,

    // 카드 옵션
    var designCode: String,
    var customText: String? = null,
    var requestTransitCard: Boolean = false,
    var requestOverseasUsage: Boolean = true,

    // 모임카드 전용
    var moimAccountId: Long? = null,

    // 신용카드 전용
    var annualIncome: BigDecimal? = null,
    var employmentType: String? = null,
    var companyName: String? = null,
    var creditScore: Int? = null,
    var requestedCreditLimit: BigDecimal? = null,

    // 심사 결과
    @Enumerated(EnumType.STRING)
    var status: CardApplicationStatus = CardApplicationStatus.SUBMITTED,

    var approvedCreditLimit: BigDecimal? = null,
    var rejectionReason: String? = null,

    var cardId: Long? = null,                  // 발급된 카드 ID

    var appliedAt: LocalDateTime = LocalDateTime.now(),
    var reviewedAt: LocalDateTime? = null
) {
    fun approve(creditLimit: BigDecimal?, cardId: Long) {
        this.status = CardApplicationStatus.APPROVED
        this.approvedCreditLimit = creditLimit
        this.cardId = cardId
        this.reviewedAt = LocalDateTime.now()
    }

    fun reject(reason: String) {
        this.status = CardApplicationStatus.REJECTED
        this.rejectionReason = reason
        this.reviewedAt = LocalDateTime.now()
    }
}
```

#### 2.1.3 CardTransaction (카드 거래)
카드 사용 내역

```kotlin
@Entity
class CardTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var cardId: Long,
    var customerId: Long,

    // 거래 정보
    var transactionId: String,                 // 거래 고유 ID
    var merchantName: String,                  // 가맹점명
    var merchantCategory: String,              // 가맹점 업종 코드 (MCC)

    @Enumerated(EnumType.STRING)
    var transactionType: TransactionType,      // 거래 유형 (승인/취소/환불)

    var amount: BigDecimal,                    // 거래 금액
    var currency: String = "KRW",              // 통화

    // 거래 일시
    var transactionDate: LocalDateTime,        // 거래 일시
    var approvalDate: LocalDateTime? = null,   // 승인 일시
    var settlementDate: LocalDate? = null,     // 정산 예정일

    // 거래 상태
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus,

    // 위치 정보
    var merchantCountry: String = "KR",        // 가맹점 국가
    var merchantCity: String? = null,          // 가맹점 도시
    var isOverseas: Boolean = false,           // 해외 거래 여부

    // 할부 정보
    var installmentMonths: Int = 0,            // 할부 개월 (0: 일시불)

    // 승인 정보
    var approvalNumber: String? = null,        // 승인 번호
    var isApproved: Boolean = false,

    // 연결 정보
    var originalTransactionId: String? = null, // 원거래 ID (취소/환불 시)

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun approve(approvalNumber: String) {
        require(!isApproved) { "이미 승인된 거래입니다" }
        this.isApproved = true
        this.approvalNumber = approvalNumber
        this.approvalDate = LocalDateTime.now()
        this.status = TransactionStatus.APPROVED
    }

    fun cancel() {
        require(isApproved) { "승인된 거래만 취소 가능합니다" }
        this.status = TransactionStatus.CANCELLED
    }
}
```

#### 2.1.4 CardBenefit (카드 혜택)
카드별 혜택 정보

```kotlin
@Entity
class CardBenefit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    var productType: CardProductType,

    // 혜택 정보
    var benefitName: String,                   // 혜택명
    var benefitDescription: String,            // 혜택 설명

    @Enumerated(EnumType.STRING)
    var benefitType: BenefitType,              // 혜택 유형

    // 캐시백 혜택
    var cashbackRate: BigDecimal? = null,      // 캐시백 비율 (%)
    var cashbackCondition: String? = null,     // 캐시백 조건 (평일/주말 등)
    var maxCashbackPerMonth: BigDecimal? = null,  // 월 최대 캐시백 금액

    // 할인 혜택
    var discountRate: BigDecimal? = null,      // 할인율 (%)
    var discountAmount: BigDecimal? = null,    // 고정 할인액

    // 적용 조건
    var minTransactionAmount: BigDecimal? = null,  // 최소 거래 금액
    var applicableMerchants: String? = null,   // 적용 가맹점 (MCC 코드)
    var applicableCountries: String? = null,   // 적용 국가

    // 혜택 기간
    var startDate: LocalDate,
    var endDate: LocalDate? = null,            // null: 무기한

    @Enumerated(EnumType.STRING)
    var status: BenefitStatus = BenefitStatus.ACTIVE,

    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### 2.1.5 CashbackHistory (캐시백 내역)
캐시백 적립 및 지급 내역

```kotlin
@Entity
class CashbackHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var cardId: Long,
    var customerId: Long,
    var transactionId: Long,                   // 연결된 거래 ID

    // 캐시백 정보
    var cashbackAmount: BigDecimal,            // 캐시백 금액
    var cashbackRate: BigDecimal,              // 적용된 캐시백 비율
    var transactionAmount: BigDecimal,         // 거래 금액

    // 캐시백 유형
    @Enumerated(EnumType.STRING)
    var cashbackType: CashbackType,            // 캐시백 유형 (일반/랜덤)

    // 지급 정보
    var earnedDate: LocalDate,                 // 적립일
    var paymentDate: LocalDate? = null,        // 지급 예정일 (다음달 10일)
    var actualPaymentDate: LocalDate? = null,  // 실제 지급일

    @Enumerated(EnumType.STRING)
    var status: CashbackStatus = CashbackStatus.EARNED,

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun markAsPaid() {
        require(status == CashbackStatus.EARNED) { "적립된 캐시백만 지급 가능합니다" }
        this.status = CashbackStatus.PAID
        this.actualPaymentDate = LocalDate.now()
    }
}
```

#### 2.1.6 CardStatement (카드 명세서)
신용카드 월별 명세서

```kotlin
@Entity
class CardStatement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var cardId: Long,
    var customerId: Long,

    // 명세서 기간
    var statementYear: Int,
    var statementMonth: Int,
    var periodStart: LocalDate,
    var periodEnd: LocalDate,

    // 금액 정보
    var totalAmount: BigDecimal,               // 총 사용 금액
    var totalCashback: BigDecimal,             // 총 캐시백
    var annualFee: BigDecimal = BigDecimal.ZERO,      // 연회비
    var netAmount: BigDecimal,                 // 실제 청구 금액

    // 결제 정보
    var paymentDueDate: LocalDate,             // 결제 예정일
    var paidAmount: BigDecimal = BigDecimal.ZERO,     // 납부 금액
    var paidDate: LocalDate? = null,           // 납부일

    @Enumerated(EnumType.STRING)
    var status: StatementStatus = StatementStatus.PENDING,

    // 거래 건수
    var transactionCount: Int,

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun markAsPaid(amount: BigDecimal) {
        require(status == StatementStatus.PENDING) { "미납 명세서만 납부 가능합니다" }
        this.paidAmount = amount
        this.paidDate = LocalDate.now()
        this.status = StatementStatus.PAID
    }
}
```

---

### 2.2 Enum 정의

```kotlin
enum class CardType {
    DEBIT,         // 체크카드
    CREDIT         // 신용카드
}

enum class CardProductType {
    // 체크카드
    FRIENDS_CHECK,          // 프렌즈 체크카드
    MOIM_CHECK,             // 모임 체크카드

    // 신용카드
    JUPJUP_CREDIT,          // 줍줍 신용카드 (PLCC)

    // 카드 플랫폼 제휴카드
    SHINHAN_CREDIT,         // 신한카드
    KB_CREDIT,              // KB국민카드
    SAMSUNG_CREDIT,         // 삼성카드
    LOTTE_CREDIT,           // 롯데카드
    HYUNDAI_CREDIT          // 현대카드
}

enum class CardStatus {
    PENDING,        // 발급 대기
    ISSUED,         // 발급 완료 (활성화 전)
    ACTIVE,         // 활성화
    SUSPENDED,      // 정지
    LOST,           // 분실
    EXPIRED,        // 만료
    TERMINATED      // 해지
}

enum class CardApplicationStatus {
    SUBMITTED,      // 신청 완료
    UNDER_REVIEW,   // 심사 중
    APPROVED,       // 승인
    REJECTED,       // 거절
    ISSUED          // 발급 완료
}

enum class TransactionType {
    PURCHASE,       // 일반 결제
    REFUND,         // 환불
    CANCELLATION,   // 취소
    CASH_ADVANCE    // 현금 서비스
}

enum class TransactionStatus {
    PENDING,        // 승인 대기
    APPROVED,       // 승인 완료
    REJECTED,       // 승인 거절
    CANCELLED,      // 취소
    SETTLED         // 정산 완료
}

enum class BenefitType {
    CASHBACK,       // 캐시백
    DISCOUNT,       // 할인
    POINTS,         // 포인트 적립
    SUBSCRIPTION,   // 구독 서비스 (이모티콘+ 등)
    FREE_SERVICE    // 무료 서비스 (ATM 출금 등)
}

enum class BenefitStatus {
    ACTIVE,         // 진행 중
    EXPIRED,        // 종료
    SUSPENDED       // 일시 중단
}

enum class CashbackType {
    STANDARD,       // 일반 캐시백 (0.2%, 0.4%, 1% 등)
    RANDOM,         // 랜덤 캐시백 (300원/3,000원)
    BONUS           // 보너스 캐시백
}

enum class CashbackStatus {
    EARNED,         // 적립
    PAID,           // 지급 완료
    CANCELLED       // 취소 (거래 취소 시)
}

enum class StatementStatus {
    PENDING,        // 미납
    PAID,           // 납부 완료
    OVERDUE,        // 연체
    PARTIAL_PAID    // 부분 납부
}
```

---

## 3. 서비스 레이어 설계

### 3.1 DataService (data 모듈)

#### CardDataService
트랜잭션 처리를 담당하는 데이터 레이어

```kotlin
@Service
class CardDataService(
    private val cardRepository: CardRepository,
    private val cardApplicationRepository: CardApplicationRepository,
    private val cardTransactionRepository: CardTransactionRepository,
    private val cashbackHistoryRepository: CashbackHistoryRepository,
    private val cardStatementRepository: CardStatementRepository
) {

    @Transactional
    fun createCard(command: CardCommand.Create): CardResult {
        val card = command.toEntity()
        val savedCard = cardRepository.save(card)
        return CardResult.from(savedCard)
    }

    @Transactional
    fun activateCard(cardId: Long): CardResult {
        val card = cardRepository.findById(cardId).orElseThrow()
        card.activate()
        return CardResult.from(cardRepository.save(card))
    }

    @Transactional
    fun suspendCard(cardId: Long, reason: String): CardResult {
        val card = cardRepository.findById(cardId).orElseThrow()
        card.suspend(reason)
        return CardResult.from(cardRepository.save(card))
    }

    @Transactional
    fun terminateCard(cardId: Long): CardResult {
        val card = cardRepository.findById(cardId).orElseThrow()
        card.terminate()
        return CardResult.from(cardRepository.save(card))
    }

    @Transactional
    fun toggleCardUsage(cardId: Long, enabled: Boolean): CardResult {
        val card = cardRepository.findById(cardId).orElseThrow()
        if (enabled) card.enableUsage() else card.disableUsage()
        return CardResult.from(cardRepository.save(card))
    }

    @Transactional(readOnly = true)
    fun getCard(cardId: Long): CardResult? {
        return cardRepository.findById(cardId)
            .map { CardResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getCardsByCustomerId(customerId: Long): List<CardResult> {
        return cardRepository.findByCustomerId(customerId)
            .map { CardResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getActiveCardsByCustomerId(customerId: Long): List<CardResult> {
        return cardRepository.findByCustomerIdAndStatus(customerId, CardStatus.ACTIVE)
            .map { CardResult.from(it) }
    }
}
```

#### CardTransactionDataService
카드 거래 관련 트랜잭션

```kotlin
@Service
class CardTransactionDataService(
    private val cardTransactionRepository: CardTransactionRepository,
    private val cardRepository: CardRepository,
    private val cashbackHistoryRepository: CashbackHistoryRepository,
    private val cardBenefitRepository: CardBenefitRepository
) {

    @Transactional
    fun createTransaction(command: TransactionCommand.Create): TransactionResult {
        // 1. 카드 조회
        val card = cardRepository.findById(command.cardId).orElseThrow()

        // 2. 카드 상태 및 한도 검증
        require(card.status == CardStatus.ACTIVE) { "활성화된 카드만 사용 가능합니다" }
        require(card.isEnabled) { "카드 사용이 비활성화되어 있습니다" }
        require(card.validateDailyLimit(command.amount)) { "일 한도를 초과합니다" }
        require(card.validateMonthlyLimit(command.amount)) { "월 한도를 초과합니다" }

        // 3. 거래 생성
        val transaction = command.toEntity()
        val savedTransaction = cardTransactionRepository.save(transaction)

        // 4. 카드 사용액 누적
        card.addUsage(command.amount)
        cardRepository.save(card)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun approveTransaction(transactionId: Long, approvalNumber: String): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId).orElseThrow()
        transaction.approve(approvalNumber)

        val savedTransaction = cardTransactionRepository.save(transaction)

        // 캐시백 계산 및 적립
        calculateAndEarnCashback(savedTransaction)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun cancelTransaction(transactionId: Long): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId).orElseThrow()
        transaction.cancel()

        // 카드 사용액 차감
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        card.addUsage(transaction.amount.negate())
        cardRepository.save(card)

        // 캐시백 취소
        cancelCashback(transactionId)

        return TransactionResult.from(cardTransactionRepository.save(transaction))
    }

    @Transactional(readOnly = true)
    fun getTransactionsByCardId(
        cardId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionResult> {
        return cardTransactionRepository
            .findByCardIdAndTransactionDateBetween(
                cardId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            )
            .map { TransactionResult.from(it) }
    }

    private fun calculateAndEarnCashback(transaction: CardTransaction) {
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        val benefits = cardBenefitRepository
            .findByProductTypeAndStatus(card.productType, BenefitStatus.ACTIVE)

        benefits.filter { it.benefitType == BenefitType.CASHBACK }
            .forEach { benefit ->
                val cashbackAmount = calculateCashbackAmount(transaction, benefit)
                if (cashbackAmount > BigDecimal.ZERO) {
                    val cashback = CashbackHistory(
                        cardId = card.id!!,
                        customerId = card.customerId,
                        transactionId = transaction.id!!,
                        cashbackAmount = cashbackAmount,
                        cashbackRate = benefit.cashbackRate ?: BigDecimal.ZERO,
                        transactionAmount = transaction.amount,
                        cashbackType = determineCashbackType(benefit),
                        earnedDate = LocalDate.now(),
                        paymentDate = LocalDate.now().withDayOfMonth(10).plusMonths(1)
                    )
                    cashbackHistoryRepository.save(cashback)
                }
            }
    }

    private fun calculateCashbackAmount(
        transaction: CardTransaction,
        benefit: CardBenefit
    ): BigDecimal {
        // 혜택 조건 검증
        if (benefit.minTransactionAmount != null &&
            transaction.amount < benefit.minTransactionAmount!!
        ) {
            return BigDecimal.ZERO
        }

        // 랜덤 캐시백 처리 (모임체크카드)
        if (benefit.benefitName.contains("랜덤")) {
            return if (transaction.amount >= BigDecimal("50000")) {
                if (Math.random() < 0.1) BigDecimal("3000") else BigDecimal("300")
            } else {
                BigDecimal.ZERO
            }
        }

        // 일반 캐시백 계산
        val cashbackRate = benefit.cashbackRate ?: return BigDecimal.ZERO
        return transaction.amount
            .multiply(cashbackRate)
            .divide(BigDecimal("100"), 0, RoundingMode.DOWN)
    }

    private fun cancelCashback(transactionId: Long) {
        cashbackHistoryRepository.findByTransactionId(transactionId)
            .forEach { it.status = CashbackStatus.CANCELLED }
    }

    private fun determineCashbackType(benefit: CardBenefit): CashbackType {
        return when {
            benefit.benefitName.contains("랜덤") -> CashbackType.RANDOM
            benefit.benefitName.contains("보너스") -> CashbackType.BONUS
            else -> CashbackType.STANDARD
        }
    }
}
```

### 3.2 Service (app 모듈)

#### CardService
비즈니스 로직 처리

```kotlin
@Service
class CardService(
    private val cardDataService: CardDataService,
    private val cardApplicationDataService: CardApplicationDataService,
    private val accountClient: AccountClient,  // Feign Client
    private val creditScoreService: CreditScoreService
) {

    fun applyForCard(request: CardApplicationRequest): CardApplicationResponse {
        // 1. 계좌 검증
        val account = accountClient.validateAccount(request.accountId)
        require(account.isValid) { "유효하지 않은 계좌입니다" }

        // 2. 모임체크카드인 경우 모임통장 검증
        if (request.productType == CardProductType.MOIM_CHECK) {
            require(request.moimAccountId != null) { "모임통장 ID가 필요합니다" }
            val moimAccount = accountClient.getMoimAccount(request.moimAccountId)
            require(moimAccount.isActive) { "활성화된 모임통장이 필요합니다" }
        }

        // 3. 신용카드인 경우 신용점수 조회
        var creditScore: Int? = null
        if (request.cardType == CardType.CREDIT) {
            val creditInfo = creditScoreService.getCreditScore(request.customerId)
            creditScore = creditInfo.score
            validateCreditCardEligibility(request, creditScore)
        }

        // 4. 신청서 제출
        val command = CardApplicationCommand.Submit(
            customerId = request.customerId,
            accountId = request.accountId,
            cardType = request.cardType,
            productType = request.productType,
            applicantName = request.applicantName,
            designCode = request.designCode,
            moimAccountId = request.moimAccountId,
            creditScore = creditScore
        )

        val result = cardApplicationDataService.submitApplication(command)

        return CardApplicationResponse.from(result)
    }

    fun issueCard(applicationId: Long): CardResponse {
        // 1. 신청서 조회
        val application = cardApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다")

        require(application.status == CardApplicationStatus.APPROVED) {
            "승인된 신청서만 발급 가능합니다"
        }

        // 2. 카드 생성
        val cardNumber = generateCardNumber()
        val cvv = generateCVV()
        val expiryDate = LocalDate.now().plusYears(5)

        val command = CardCommand.Create(
            cardNumber = cardNumber,
            customerId = application.customerId,
            accountId = application.accountId,
            cardType = application.cardType,
            productType = application.productType,
            cardholderName = application.applicantName,
            expiryDate = expiryDate,
            cvv = cvv,
            designCode = application.designCode,
            customText = application.customText,
            hasTransitCard = application.requestTransitCard,
            moimAccountId = application.moimAccountId,
            applicationId = applicationId
        )

        val cardResult = cardDataService.createCard(command)

        // 3. 신청서 상태 업데이트
        cardApplicationDataService.markAsIssued(applicationId, cardResult.id)

        return CardResponse.from(cardResult)
    }

    fun activateCard(cardId: Long, customerId: Long): CardResponse {
        // 본인 확인
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다")

        require(card.customerId == customerId) { "본인의 카드만 활성화 가능합니다" }

        val result = cardDataService.activateCard(cardId)
        return CardResponse.from(result)
    }

    fun toggleCardUsage(cardId: Long, enabled: Boolean): CardResponse {
        val result = cardDataService.toggleCardUsage(cardId, enabled)
        return CardResponse.from(result)
    }

    fun reportLostCard(cardId: Long, customerId: Long): CardResponse {
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다")

        require(card.customerId == customerId) { "본인의 카드만 분실 신고 가능합니다" }

        val result = cardDataService.suspendCard(cardId, "분실")
        return CardResponse.from(result)
    }

    fun terminateCard(cardId: Long, customerId: Long): CardResponse {
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다")

        require(card.customerId == customerId) { "본인의 카드만 해지 가능합니다" }

        // 신용카드인 경우 미납 내역 확인
        if (card.cardType == CardType.CREDIT) {
            // 미납 내역 확인 로직
        }

        val result = cardDataService.terminateCard(cardId)
        return CardResponse.from(result)
    }

    private fun validateCreditCardEligibility(
        request: CardApplicationRequest,
        creditScore: Int
    ) {
        // 최소 신용점수 검증
        val minScore = getMinimumCreditScore(request.productType)
        require(creditScore >= minScore) {
            "최소 신용점수 미달: 필요 ${minScore}점, 현재 ${creditScore}점"
        }

        // 연소득 검증
        if (request.annualIncome != null) {
            val minIncome = getMinimumIncome(request.productType)
            require(request.annualIncome >= minIncome) {
                "최소 연소득 미달: 필요 ${minIncome}원"
            }
        }
    }

    private fun generateCardNumber(): String {
        // 실제로는 카드사 BIN + 순번 + 체크디지트
        return "5234-${Random.nextInt(1000, 9999)}-${Random.nextInt(1000, 9999)}-${Random.nextInt(1000, 9999)}"
    }

    private fun generateCVV(): String {
        return Random.nextInt(100, 999).toString()
    }

    private fun getMinimumCreditScore(productType: CardProductType): Int {
        return when (productType) {
            CardProductType.JUPJUP_CREDIT -> 600
            else -> 500
        }
    }

    private fun getMinimumIncome(productType: CardProductType): BigDecimal {
        return when (productType) {
            CardProductType.JUPJUP_CREDIT -> BigDecimal("20000000")
            else -> BigDecimal("10000000")
        }
    }
}
```

---

## 4. API 설계

### 4.1 카드 신청 API

#### POST /api/cards/applications
카드 발급 신청

**Request:**
```json
{
  "customerId": 1,
  "accountId": 100,
  "cardType": "DEBIT",
  "productType": "FRIENDS_CHECK",
  "applicantName": "홍길동",
  "residentNumber": "900101-1234567",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "designCode": "RYAN",
  "requestTransitCard": true,
  "requestOverseasUsage": true
}
```

**Response:**
```json
{
  "applicationId": 1001,
  "status": "SUBMITTED",
  "productType": "FRIENDS_CHECK",
  "message": "신청이 완료되었습니다. 카드는 3-5일 내 배송됩니다.",
  "appliedAt": "2026-02-08T10:30:00"
}
```

#### GET /api/cards/applications/{applicationId}
신청 내역 조회

#### GET /api/cards/applications/customer/{customerId}
고객의 신청 내역 목록

### 4.2 카드 관리 API

#### GET /api/cards/{cardId}
카드 상세 조회

**Response:**
```json
{
  "cardId": 1,
  "cardNumberMasked": "5234-****-****-5678",
  "cardholderName": "홍길동",
  "productType": "FRIENDS_CHECK",
  "status": "ACTIVE",
  "expiryDate": "2031-02-28",
  "dailyLimit": 6000000,
  "monthlyLimit": 20000000,
  "dailyUsed": 150000,
  "monthlyUsed": 3200000,
  "hasTransitCard": true,
  "isEnabled": true
}
```

#### GET /api/cards/customer/{customerId}
고객의 카드 목록

#### POST /api/cards/{cardId}/activate
카드 활성화 (발급 후 첫 사용 전)

#### PUT /api/cards/{cardId}/toggle
카드 사용 ON/OFF

**Request:**
```json
{
  "enabled": false
}
```

#### POST /api/cards/{cardId}/report-lost
분실 신고

#### POST /api/cards/{cardId}/terminate
카드 해지

### 4.3 카드 거래 API

#### POST /api/cards/{cardId}/transactions
카드 결제 (PG 연동)

**Request:**
```json
{
  "merchantName": "스타벅스 강남점",
  "merchantCategory": "5814",
  "amount": 15000,
  "transactionType": "PURCHASE",
  "merchantCountry": "KR"
}
```

#### GET /api/cards/{cardId}/transactions
카드 거래 내역 조회

**Query Parameters:**
- `startDate`: 조회 시작일 (YYYY-MM-DD)
- `endDate`: 조회 종료일 (YYYY-MM-DD)

**Response:**
```json
{
  "transactions": [
    {
      "transactionId": "TXN123456",
      "merchantName": "스타벅스 강남점",
      "amount": 15000,
      "transactionDate": "2026-02-08T14:30:00",
      "status": "APPROVED",
      "cashbackAmount": 30
    }
  ],
  "totalCount": 45,
  "totalAmount": 850000
}
```

#### POST /api/cards/transactions/{transactionId}/cancel
거래 취소

### 4.4 캐시백 API

#### GET /api/cards/{cardId}/cashback
캐시백 내역 조회

**Response:**
```json
{
  "totalEarned": 25600,
  "totalPaid": 18000,
  "pending": 7600,
  "nextPaymentDate": "2026-03-10",
  "history": [
    {
      "earnedDate": "2026-02-05",
      "amount": 300,
      "transactionAmount": 150000,
      "merchantName": "CU편의점",
      "status": "EARNED"
    }
  ]
}
```

#### GET /api/cards/{cardId}/cashback/summary
월별 캐시백 요약

### 4.5 명세서 API (신용카드)

#### GET /api/cards/{cardId}/statements
명세서 목록

#### GET /api/cards/{cardId}/statements/{year}/{month}
월별 명세서 조회

**Response:**
```json
{
  "statementId": 123,
  "year": 2026,
  "month": 1,
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "totalAmount": 850000,
  "totalCashback": 8500,
  "annualFee": 18000,
  "netAmount": 859500,
  "paymentDueDate": "2026-02-15",
  "status": "PENDING",
  "transactionCount": 45
}
```

#### POST /api/cards/statements/{statementId}/pay
명세서 결제

---

## 5. 비즈니스 로직

### 5.1 캐시백 계산 로직

#### 프렌즈 체크카드
```kotlin
fun calculateFriendsCheckCashback(
    transactionAmount: BigDecimal,
    transactionDate: LocalDateTime
): BigDecimal {
    val dayOfWeek = transactionDate.dayOfWeek

    val cashbackRate = when (dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> BigDecimal("0.4")  // 주말 0.4%
        else -> {
            // 공휴일 체크 로직
            if (isHoliday(transactionDate.toLocalDate())) {
                BigDecimal("0.4")
            } else {
                BigDecimal("0.2")  // 평일 0.2%
            }
        }
    }

    return transactionAmount
        .multiply(cashbackRate)
        .divide(BigDecimal("100"), 0, RoundingMode.DOWN)
}
```

#### 모임 체크카드 랜덤 캐시백
```kotlin
fun calculateMoimCheckCashback(
    transactionAmount: BigDecimal,
    cardId: Long,
    merchantName: String,
    transactionDate: LocalDate
): BigDecimal {
    // 5만원 이상 결제만 해당
    if (transactionAmount < BigDecimal("50000")) {
        return BigDecimal.ZERO
    }

    // 동일 가맹점 일 1회 제한 체크
    val todayTransactionCount = cashbackHistoryRepository
        .countByCardIdAndMerchantNameAndEarnedDate(cardId, merchantName, transactionDate)

    if (todayTransactionCount > 0) {
        return BigDecimal.ZERO
    }

    // 랜덤 캐시백: 90% 확률로 300원, 10% 확률로 3,000원
    return if (Random.nextDouble() < 0.1) {
        BigDecimal("3000")
    } else {
        BigDecimal("300")
    }
}
```

#### 줍줍 신용카드 캐시백
```kotlin
fun calculateJupjupCreditCashback(
    transactionAmount: BigDecimal,
    paymentDate: LocalDate,
    dueDate: LocalDate,
    currentMonthCashback: BigDecimal
): Pair<BigDecimal, BigDecimal> {
    // 기본 1% 캐시백 (월 최대 2만원)
    val basicCashback = transactionAmount
        .multiply(BigDecimal("1"))
        .divide(BigDecimal("100"), 0, RoundingMode.DOWN)
        .min(BigDecimal("20000").subtract(currentMonthCashback))

    // 결제일 5일 이내 추가 1% 캐시백 (월 최대 2만원)
    val bonusCashback = if (ChronoUnit.DAYS.between(dueDate, paymentDate) <= 5) {
        transactionAmount
            .multiply(BigDecimal("1"))
            .divide(BigDecimal("100"), 0, RoundingMode.DOWN)
            .min(BigDecimal("20000").subtract(currentMonthCashback))
    } else {
        BigDecimal.ZERO
    }

    return Pair(basicCashback, bonusCashback)
}
```

### 5.2 카드 한도 관리

#### 일일 한도 초기화 (스케줄러)
```kotlin
@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
fun resetDailyLimits() {
    val allActiveCards = cardRepository.findByStatus(CardStatus.ACTIVE)
    allActiveCards.forEach { card ->
        card.resetDailyUsage()
    }
    cardRepository.saveAll(allActiveCards)
}
```

#### 월간 한도 초기화 (스케줄러)
```kotlin
@Scheduled(cron = "0 0 0 1 * *")  // 매월 1일 자정
fun resetMonthlyLimits() {
    val allActiveCards = cardRepository.findByStatus(CardStatus.ACTIVE)
    allActiveCards.forEach { card ->
        card.resetMonthlyUsage()
    }
    cardRepository.saveAll(allActiveCards)
}
```

### 5.3 캐시백 지급 (스케줄러)

```kotlin
@Scheduled(cron = "0 0 9 10 * *")  // 매월 10일 오전 9시
fun payCashback() {
    val today = LocalDate.now()
    val pendingCashbacks = cashbackHistoryRepository
        .findByStatusAndPaymentDate(CashbackStatus.EARNED, today)

    pendingCashbacks.groupBy { it.cardId }
        .forEach { (cardId, cashbacks) ->
            val card = cardRepository.findById(cardId).orElse(null) ?: return@forEach

            val totalCashback = cashbacks.sumOf { it.cashbackAmount }

            // 계좌에 캐시백 입금
            accountClient.deposit(card.accountId, totalCashback)

            // 캐시백 지급 완료 처리
            cashbacks.forEach { it.markAsPaid() }
            cashbackHistoryRepository.saveAll(cashbacks)
        }
}
```

---

## 6. 데이터 흐름

### 6.1 카드 신청 → 발급 흐름

```
[Customer]
    ↓ 신청
[CardService.applyForCard]
    ↓ 계좌 검증
[AccountClient (Feign)]
    ↓ 신용카드인 경우 신용점수 조회
[CreditScoreService]
    ↓ 신청서 생성
[CardApplicationDataService]
    ↓ 저장
[CardApplicationRepository]
    ↓ 심사 (체크카드: 자동, 신용카드: 수동)
[심사 시스템]
    ↓ 승인
[CardApplicationDataService.approve]
    ↓ 카드 발급
[CardService.issueCard]
    ↓ Card 생성
[CardRepository]
    ↓ 배송
[물류 시스템]
    ↓ 고객 수령 후 활성화
[CardService.activateCard]
    ↓ 완료
[Card.status = ACTIVE]
```

### 6.2 카드 결제 흐름

```
[Customer]
    ↓ 결제 요청
[PG사 / 가맹점 단말기]
    ↓ 결제 승인 요청
[CardTransactionDataService.createTransaction]
    ↓ 카드 상태 검증
[Card.validateDailyLimit, validateMonthlyLimit]
    ↓ 거래 생성
[CardTransactionRepository]
    ↓ 승인
[CardTransactionDataService.approveTransaction]
    ↓ 캐시백 계산 및 적립
[calculateAndEarnCashback]
    ↓ CashbackHistory 생성
[CashbackHistoryRepository]
    ↓ 카드 사용액 누적
[Card.addUsage()]
    ↓ 완료
[TransactionStatus = APPROVED]
```

### 6.3 캐시백 지급 흐름

```
[스케줄러 (매월 10일)]
    ↓ 지급 대상 조회
[CashbackHistoryRepository.findByPaymentDate]
    ↓ 카드별 합산
[groupBy cardId]
    ↓ 계좌 입금
[AccountClient.deposit]
    ↓ 지급 완료 처리
[CashbackHistory.markAsPaid()]
```

---

## 7. 외부 연동

### 7.1 Account Service 연동
Feign Client를 통한 계좌 서비스 호출

```kotlin
@FeignClient(name = "account-service")
interface AccountClient {
    @GetMapping("/internal/accounts/{accountId}/validate")
    fun validateAccount(@PathVariable accountId: Long): AccountValidationDto

    @GetMapping("/internal/accounts/moim/{moimAccountId}")
    fun getMoimAccount(@PathVariable moimAccountId: Long): MoimAccountDto

    @PostMapping("/internal/accounts/{accountId}/deposit")
    fun deposit(@PathVariable accountId: Long, @RequestBody amount: BigDecimal)

    @PostMapping("/internal/accounts/{accountId}/withdraw")
    fun withdraw(@PathVariable accountId: Long, @RequestBody amount: BigDecimal)
}
```

### 7.2 신용평가 기관 연동
NICE, KCB 등 신용평가기관 API 호출 (신용카드 발급 시)

```kotlin
@Service
class CreditScoreService(
    private val niceCreditClient: NiceCreditClient,
    private val kcbCreditClient: KcbCreditClient
) {
    fun getCreditScore(customerId: Long): CreditScoreResult {
        // NICE 또는 KCB 신용평가 조회
        // 결과 캐싱 (1시간)
    }
}
```

### 7.3 PG사 연동
카드 결제 승인 처리

```kotlin
@Service
class PaymentGatewayService(
    private val pgClient: PaymentGatewayClient
) {
    fun requestApproval(
        cardNumber: String,
        amount: BigDecimal,
        merchantInfo: MerchantInfo
    ): ApprovalResult {
        // PG사 승인 요청
    }
}
```

### 7.4 카드사 연동 (PLCC, 플랫폼 카드)
제휴 카드사 API 연동 (신한, 국민, 삼성 등)

```kotlin
@Service
class PartnerCardService(
    private val shinhanCardClient: ShinhanCardClient,
    private val kbCardClient: KBCardClient
) {
    fun applyPartnerCard(request: PartnerCardApplicationRequest): PartnerCardResult {
        // 제휴사 카드 신청 전달
    }
}
```

---

## 8. 예외 처리

### 8.1 카드 신청 단계 예외
- `InsufficientCreditScoreException`: 신용점수 미달 (신용카드)
- `InvalidAccountException`: 유효하지 않은 계좌
- `MoimAccountRequiredException`: 모임통장 미연결 (모임체크카드)
- `DuplicateCardApplicationException`: 중복 신청

### 8.2 카드 사용 단계 예외
- `CardNotActiveException`: 비활성화된 카드 사용 시도
- `CardDisabledException`: 사용 중지된 카드
- `DailyLimitExceededException`: 일 한도 초과
- `MonthlyLimitExceededException`: 월 한도 초과
- `CardExpiredException`: 만료된 카드

### 8.3 거래 단계 예외
- `InsufficientBalanceException`: 잔액 부족 (체크카드)
- `CreditLimitExceededException`: 신용한도 초과 (신용카드)
- `TransactionApprovalFailedException`: 승인 실패
- `InvalidMerchantException`: 유효하지 않은 가맹점

---

## 9. 성능 최적화

### 9.1 인덱스 설계
```sql
-- Card 테이블
CREATE INDEX idx_card_customer ON card(customer_id);
CREATE INDEX idx_card_status ON card(status);
CREATE INDEX idx_card_number ON card(card_number);
CREATE INDEX idx_card_account ON card(account_id);

-- CardTransaction 테이블
CREATE INDEX idx_transaction_card ON card_transaction(card_id);
CREATE INDEX idx_transaction_date ON card_transaction(transaction_date);
CREATE INDEX idx_transaction_status ON card_transaction(status);
CREATE INDEX idx_transaction_customer ON card_transaction(customer_id);

-- CashbackHistory 테이블
CREATE INDEX idx_cashback_card ON cashback_history(card_id);
CREATE INDEX idx_cashback_payment_date ON cashback_history(payment_date, status);
CREATE INDEX idx_cashback_transaction ON cashback_history(transaction_id);

-- CardStatement 테이블
CREATE INDEX idx_statement_card ON card_statement(card_id);
CREATE INDEX idx_statement_period ON card_statement(statement_year, statement_month);
```

### 9.2 캐싱 전략
- 신용점수: 1시간 캐싱
- 카드 혜택 정보: 1일 캐싱
- 카드 상품 정보: 영구 캐싱 (수동 무효화)
- 공휴일 정보: 연 단위 캐싱

---

## 10. 보안

### 10.1 데이터 암호화
- 카드번호: AES-256 암호화
- CVV: AES-256 암호화
- 주민등록번호: AES-256 암호화
- 카드번호 조회 시: 마스킹 처리 (1234-****-****-5678)

### 10.2 접근 제어
- 본인 카드 정보만 조회 가능
- 관리자 권한: 전체 카드 조회 및 관리
- 카드 활성화/정지: 본인 인증 필수
- PG 연동: API Key + HTTPS

### 10.3 이상 거래 탐지
- 단기간 고액 결제 알림
- 해외 거래 알림 (처음 사용 시)
- 중복 결제 방지 (동일 금액/가맹점 1분 내)

---

## 11. 모니터링

### 11.1 주요 지표
- 카드 발급 건수 (일/주/월)
- 카드 사용 건수 및 금액
- 평균 거래 금액
- 캐시백 지급 총액
- 카드 해지율

### 11.2 알림
- 카드 결제 승인 알림 (푸시)
- 캐시백 적립 알림
- 캐시백 지급 알림 (매월 10일)
- 카드 유효기간 만료 1개월 전 알림
- 이상 거래 탐지 알림

---

## 12. 참고 자료

- 카카오뱅크 체크카드: https://www.kakaobank.com/products/checkcard
- 카카오뱅크 모임체크카드: https://www.kakaobank.com/products/moimcheckcard
- 카카오뱅크 카드플랫폼: https://www.kakaobank.com/products/cardplatform
- 카카오뱅크 PLCC: https://kakaobank.com/products/plcc
- 여신전문금융업법: 카드업 규제
- 개인정보보호법: 카드정보 보호
