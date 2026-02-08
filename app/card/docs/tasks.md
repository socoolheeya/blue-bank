# Card Service 구현 태스크

## 개요

이 문서는 architecture.md를 기반으로 Card Service를 단계별로 구현하는 태스크 목록입니다. Loan Service의 구현 패턴을 따르며, data 모듈과 app 모듈의 명확한 책임 분리를 유지합니다.

---

## Phase 1: 기본 인프라 및 도메인 모델 구축

### 1.1 프로젝트 구조 생성

**목표**: data/card-data와 app/card 모듈의 기본 구조 생성

**작업 내용**:
```
data/card-data/
├── src/main/kotlin/com/socoolheeya/bluebank/card/data/
│   ├── domain/
│   │   ├── entity/          # Entity 클래스
│   │   ├── command/         # Command 객체
│   │   └── result/          # Result 객체
│   ├── repository/          # JPA Repository
│   └── service/             # DataService (트랜잭션)
└── build.gradle.kts

app/card/
├── src/main/kotlin/com/socoolheeya/bluebank/card/
│   ├── controller/          # REST API Controller
│   ├── service/             # Business Logic Service
│   ├── dto/                 # DTO (Request/Response)
│   ├── client/              # Feign Client
│   └── CardApplication.kt
├── src/main/resources/
│   └── application.yml
└── build.gradle.kts
```

**체크리스트**:
- [ ] data/card-data 모듈 생성
- [ ] app/card 모듈 생성
- [ ] 패키지 구조 생성
- [ ] build.gradle.kts 의존성 설정
  - data 모듈: spring-boot-starter-data-jpa, kotlin-reflect
  - app 모듈: data/card-data 의존성, spring-boot-starter-web, spring-cloud-starter-openfeign

---

### 1.2 Enum 정의

**목표**: 카드 관련 모든 Enum 클래스 생성

**파일 위치**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/CardEnums.kt`

**작업 내용**:
```kotlin
package com.socoolheeya.bluebank.card.data.domain

object CardEnums {

    enum class CardType(val description: String) {
        DEBIT("체크카드"),
        CREDIT("신용카드")
    }

    enum class CardProductType(val description: String) {
        // 체크카드
        FRIENDS_CHECK("프렌즈 체크카드"),
        MOIM_CHECK("모임 체크카드"),

        // 신용카드
        JUPJUP_CREDIT("줍줍 신용카드"),

        // 카드 플랫폼 제휴카드
        SHINHAN_CREDIT("신한카드"),
        KB_CREDIT("KB국민카드"),
        SAMSUNG_CREDIT("삼성카드"),
        LOTTE_CREDIT("롯데카드"),
        HYUNDAI_CREDIT("현대카드")
    }

    enum class CardStatus(val description: String) {
        PENDING("발급 대기"),
        ISSUED("발급 완료"),
        ACTIVE("활성화"),
        SUSPENDED("정지"),
        LOST("분실"),
        EXPIRED("만료"),
        TERMINATED("해지")
    }

    enum class CardApplicationStatus(val description: String) {
        SUBMITTED("신청 완료"),
        UNDER_REVIEW("심사 중"),
        APPROVED("승인"),
        REJECTED("거절"),
        ISSUED("발급 완료")
    }

    enum class TransactionType(val description: String) {
        PURCHASE("일반 결제"),
        REFUND("환불"),
        CANCELLATION("취소"),
        CASH_ADVANCE("현금 서비스")
    }

    enum class TransactionStatus(val description: String) {
        PENDING("승인 대기"),
        APPROVED("승인 완료"),
        REJECTED("승인 거절"),
        CANCELLED("취소"),
        SETTLED("정산 완료")
    }

    enum class BenefitType(val description: String) {
        CASHBACK("캐시백"),
        DISCOUNT("할인"),
        POINTS("포인트 적립"),
        SUBSCRIPTION("구독 서비스"),
        FREE_SERVICE("무료 서비스")
    }

    enum class BenefitStatus(val description: String) {
        ACTIVE("진행 중"),
        EXPIRED("종료"),
        SUSPENDED("일시 중단")
    }

    enum class CashbackType(val description: String) {
        STANDARD("일반 캐시백"),
        RANDOM("랜덤 캐시백"),
        BONUS("보너스 캐시백")
    }

    enum class CashbackStatus(val description: String) {
        EARNED("적립"),
        PAID("지급 완료"),
        CANCELLED("취소")
    }

    enum class StatementStatus(val description: String) {
        PENDING("미납"),
        PAID("납부 완료"),
        OVERDUE("연체"),
        PARTIAL_PAID("부분 납부")
    }
}
```

**체크리스트**:
- [ ] CardEnums.kt 파일 생성
- [ ] 모든 Enum 정의 완료 (9개)
- [ ] description 필드 추가
- [ ] Enum 값 검증

---

### 1.3 Entity 클래스 구현

**목표**: 6개 핵심 Entity 클래스 구현

#### 1.3.1 Card Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/Card.kt`

**작업 내용**:
```kotlin
package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "card")
class Card(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    @Column(unique = true, nullable = false, length = 500)
    var cardNumber: String,  // 암호화

    @Column(unique = true, nullable = false)
    var cardNumberMasked: String,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var accountId: Long,

    // 카드 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var cardType: CardType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: CardProductType,

    // 카드 정보
    @Column(nullable = false)
    var cardholderName: String,

    @Column(nullable = false)
    var issueDate: LocalDate,

    @Column(nullable = false)
    var expiryDate: LocalDate,

    @Column(nullable = false, length = 500)
    var cvv: String,  // 암호화

    // 카드 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CardStatus,

    // 한도 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var dailyLimit: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var monthlyLimit: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var dailyUsed: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    var monthlyUsed: BigDecimal = BigDecimal.ZERO,

    // 카드 디자인
    @Column(nullable = false)
    var designCode: String,

    var customText: String? = null,

    // 부가 서비스
    @Column(nullable = false)
    var hasTransitCard: Boolean = false,

    @Column(nullable = false)
    var hasOverseasUsage: Boolean = true,

    @Column(nullable = false)
    var isEnabled: Boolean = true,

    // 신용카드 전용
    @Column(precision = 15, scale = 2)
    var creditLimit: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var availableCredit: BigDecimal? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    var annualFee: BigDecimal = BigDecimal.ZERO,

    var nextPaymentDate: LocalDate? = null,

    // 제휴사 정보 (PLCC)
    var partnerCompany: String? = null,
    var partnerCardId: String? = null,

    // 모임카드 전용
    var moimAccountId: Long? = null,

    // 발급 정보
    var applicationId: Long? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    var issueFee: BigDecimal = BigDecimal.ZERO,

    // 감사
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 카드 활성화
     */
    fun activate() {
        require(status == CardStatus.ISSUED) { "발급된 카드만 활성화 가능합니다" }
        this.status = CardStatus.ACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 카드 정지
     */
    fun suspend(reason: String) {
        require(status == CardStatus.ACTIVE) { "활성화된 카드만 정지 가능합니다" }
        this.status = CardStatus.SUSPENDED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 카드 해지
     */
    fun terminate() {
        require(status != CardStatus.TERMINATED) { "이미 해지된 카드입니다" }
        this.status = CardStatus.TERMINATED
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 카드 사용 활성화
     */
    fun enableUsage() {
        require(status == CardStatus.ACTIVE) { "활성화된 카드만 사용 설정 가능합니다" }
        this.isEnabled = true
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 카드 사용 비활성화
     */
    fun disableUsage() {
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 일 한도 검증
     */
    fun validateDailyLimit(amount: BigDecimal): Boolean {
        return dailyUsed + amount <= dailyLimit
    }

    /**
     * 월 한도 검증
     */
    fun validateMonthlyLimit(amount: BigDecimal): Boolean {
        return monthlyUsed + amount <= monthlyLimit
    }

    /**
     * 사용액 추가
     */
    fun addUsage(amount: BigDecimal) {
        this.dailyUsed = dailyUsed + amount
        this.monthlyUsed = monthlyUsed + amount
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 일일 사용액 초기화
     */
    fun resetDailyUsage() {
        this.dailyUsed = BigDecimal.ZERO
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 월간 사용액 초기화
     */
    fun resetMonthlyUsage() {
        this.monthlyUsed = BigDecimal.ZERO
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 분실 신고
     */
    fun reportLost() {
        this.status = CardStatus.LOST
        this.isEnabled = false
        this.updatedAt = LocalDateTime.now()
    }
}
```

**체크리스트**:
- [ ] Card.kt 파일 생성
- [ ] 모든 필드 정의 (@Column 애노테이션 포함)
- [ ] 비즈니스 로직 메서드 구현 (10개 메서드)
- [ ] JPA 애노테이션 설정
- [ ] BigDecimal precision/scale 설정

#### 1.3.2 CardApplication Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/CardApplication.kt`

**작업 내용**:
```kotlin
@Entity
@Table(name = "card_application")
class CardApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var accountId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var cardType: CardType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: CardProductType,

    // 신청자 정보
    @Column(nullable = false)
    var applicantName: String,

    @Column(nullable = false, length = 500)
    var residentNumber: String,  // 암호화

    @Column(nullable = false)
    var phoneNumber: String,

    var email: String? = null,

    @Column(nullable = false)
    var address: String,

    // 카드 옵션
    @Column(nullable = false)
    var designCode: String,

    var customText: String? = null,

    @Column(nullable = false)
    var requestTransitCard: Boolean = false,

    @Column(nullable = false)
    var requestOverseasUsage: Boolean = true,

    // 모임카드 전용
    var moimAccountId: Long? = null,

    // 신용카드 전용
    @Column(precision = 15, scale = 2)
    var annualIncome: BigDecimal? = null,

    var employmentType: String? = null,
    var companyName: String? = null,
    var creditScore: Int? = null,

    @Column(precision = 15, scale = 2)
    var requestedCreditLimit: BigDecimal? = null,

    // 심사 결과
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CardApplicationStatus = CardApplicationStatus.SUBMITTED,

    @Column(precision = 15, scale = 2)
    var approvedCreditLimit: BigDecimal? = null,

    @Column(length = 500)
    var rejectionReason: String? = null,

    var cardId: Long? = null,

    @Column(nullable = false)
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

    fun markAsIssued() {
        require(status == CardApplicationStatus.APPROVED) { "승인된 신청서만 발급 처리 가능합니다" }
        this.status = CardApplicationStatus.ISSUED
    }

    fun startReview() {
        require(status == CardApplicationStatus.SUBMITTED) { "제출된 신청서만 심사 시작 가능합니다" }
        this.status = CardApplicationStatus.UNDER_REVIEW
    }
}
```

**체크리스트**:
- [ ] CardApplication.kt 생성
- [ ] 모든 필드 정의
- [ ] 비즈니스 로직 메서드 구현 (4개)
- [ ] 유효성 검증 추가

#### 1.3.3 CardTransaction Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/CardTransaction.kt`

```kotlin
@Entity
@Table(name = "card_transaction")
class CardTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var cardId: Long,

    @Column(nullable = false)
    var customerId: Long,

    // 거래 정보
    @Column(unique = true, nullable = false)
    var transactionId: String,

    @Column(nullable = false)
    var merchantName: String,

    @Column(nullable = false)
    var merchantCategory: String,  // MCC

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var transactionType: TransactionType,

    @Column(nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String = "KRW",

    // 거래 일시
    @Column(nullable = false)
    var transactionDate: LocalDateTime,

    var approvalDate: LocalDateTime? = null,
    var settlementDate: LocalDate? = null,

    // 거래 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TransactionStatus,

    // 위치 정보
    @Column(nullable = false, length = 2)
    var merchantCountry: String = "KR",

    var merchantCity: String? = null,

    @Column(nullable = false)
    var isOverseas: Boolean = false,

    // 할부 정보
    @Column(nullable = false)
    var installmentMonths: Int = 0,

    // 승인 정보
    var approvalNumber: String? = null,

    @Column(nullable = false)
    var isApproved: Boolean = false,

    // 연결 정보
    var originalTransactionId: String? = null,

    @Column(nullable = false)
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

    fun settle(settlementDate: LocalDate) {
        require(status == TransactionStatus.APPROVED) { "승인된 거래만 정산 가능합니다" }
        this.status = TransactionStatus.SETTLED
        this.settlementDate = settlementDate
    }
}
```

**체크리스트**:
- [ ] CardTransaction.kt 생성
- [ ] 거래 처리 로직 구현
- [ ] 승인/취소/정산 메서드 구현

#### 1.3.4 CardBenefit Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/CardBenefit.kt`

```kotlin
@Entity
@Table(name = "card_benefit")
class CardBenefit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: CardProductType,

    // 혜택 정보
    @Column(nullable = false)
    var benefitName: String,

    @Column(nullable = false, length = 1000)
    var benefitDescription: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var benefitType: BenefitType,

    // 캐시백 혜택
    @Column(precision = 5, scale = 2)
    var cashbackRate: BigDecimal? = null,

    var cashbackCondition: String? = null,

    @Column(precision = 15, scale = 2)
    var maxCashbackPerMonth: BigDecimal? = null,

    // 할인 혜택
    @Column(precision = 5, scale = 2)
    var discountRate: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var discountAmount: BigDecimal? = null,

    // 적용 조건
    @Column(precision = 15, scale = 2)
    var minTransactionAmount: BigDecimal? = null,

    var applicableMerchants: String? = null,
    var applicableCountries: String? = null,

    // 혜택 기간
    @Column(nullable = false)
    var startDate: LocalDate,

    var endDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BenefitStatus = BenefitStatus.ACTIVE,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun activate() {
        this.status = BenefitStatus.ACTIVE
    }

    fun suspend() {
        this.status = BenefitStatus.SUSPENDED
    }

    fun expire() {
        this.status = BenefitStatus.EXPIRED
    }

    fun isActive(): Boolean {
        if (status != BenefitStatus.ACTIVE) return false
        val today = LocalDate.now()
        if (today.isBefore(startDate)) return false
        if (endDate != null && today.isAfter(endDate)) return false
        return true
    }
}
```

**체크리스트**:
- [ ] CardBenefit.kt 생성
- [ ] 혜택 유형별 필드 정의
- [ ] 상태 변경 메서드 구현

#### 1.3.5 CashbackHistory Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/CashbackHistory.kt`

```kotlin
@Entity
@Table(name = "cashback_history")
class CashbackHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var cardId: Long,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var transactionId: Long,

    // 캐시백 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var cashbackAmount: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 2)
    var cashbackRate: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var transactionAmount: BigDecimal,

    // 캐시백 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var cashbackType: CashbackType,

    // 지급 정보
    @Column(nullable = false)
    var earnedDate: LocalDate,

    var paymentDate: LocalDate? = null,
    var actualPaymentDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CashbackStatus = CashbackStatus.EARNED,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun markAsPaid() {
        require(status == CashbackStatus.EARNED) { "적립된 캐시백만 지급 가능합니다" }
        this.status = CashbackStatus.PAID
        this.actualPaymentDate = LocalDate.now()
    }

    fun cancel() {
        this.status = CashbackStatus.CANCELLED
    }
}
```

**체크리스트**:
- [ ] CashbackHistory.kt 생성
- [ ] 캐시백 지급 로직 구현

#### 1.3.6 CardStatement Entity

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/entity/CardStatement.kt`

```kotlin
@Entity
@Table(name = "card_statement")
class CardStatement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var cardId: Long,

    @Column(nullable = false)
    var customerId: Long,

    // 명세서 기간
    @Column(nullable = false)
    var statementYear: Int,

    @Column(nullable = false)
    var statementMonth: Int,

    @Column(nullable = false)
    var periodStart: LocalDate,

    @Column(nullable = false)
    var periodEnd: LocalDate,

    // 금액 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var totalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalCashback: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var annualFee: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    var netAmount: BigDecimal,

    // 결제 정보
    @Column(nullable = false)
    var paymentDueDate: LocalDate,

    @Column(nullable = false, precision = 15, scale = 2)
    var paidAmount: BigDecimal = BigDecimal.ZERO,

    var paidDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatementStatus = StatementStatus.PENDING,

    // 거래 건수
    @Column(nullable = false)
    var transactionCount: Int,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun markAsPaid(amount: BigDecimal) {
        require(status == StatementStatus.PENDING) { "미납 명세서만 납부 가능합니다" }
        this.paidAmount = amount
        this.paidDate = LocalDate.now()

        this.status = if (amount >= netAmount) {
            StatementStatus.PAID
        } else {
            StatementStatus.PARTIAL_PAID
        }
    }

    fun markOverdue() {
        require(status == StatementStatus.PENDING || status == StatementStatus.PARTIAL_PAID) {
            "미납 또는 부분납부 명세서만 연체 처리 가능합니다"
        }
        this.status = StatementStatus.OVERDUE
    }
}
```

**체크리스트**:
- [ ] CardStatement.kt 생성
- [ ] 명세서 결제 로직 구현
- [ ] 연체 처리 로직 구현

---

### 1.4 Repository 인터페이스 생성

**목표**: 각 Entity에 대한 JPA Repository 생성

**파일 위치**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/repository/`

#### CardRepository.kt
```kotlin
package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.Card
import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CardRepository : JpaRepository<Card, Long> {
    fun findByCardNumber(cardNumber: String): Optional<Card>
    fun findByCustomerId(customerId: Long): List<Card>
    fun findByCustomerIdAndStatus(customerId: Long, status: CardStatus): List<Card>
    fun findByStatus(status: CardStatus): List<Card>
    fun findByAccountId(accountId: Long): List<Card>
    fun existsByCardNumber(cardNumber: String): Boolean
}
```

#### CardApplicationRepository.kt
```kotlin
interface CardApplicationRepository : JpaRepository<CardApplication, Long> {
    fun findByCustomerId(customerId: Long): List<CardApplication>
    fun findByCustomerIdAndStatus(customerId: Long, status: CardApplicationStatus): List<CardApplication>
    fun findByStatus(status: CardApplicationStatus): List<CardApplication>
    fun findByCardId(cardId: Long): Optional<CardApplication>
}
```

#### CardTransactionRepository.kt
```kotlin
interface CardTransactionRepository : JpaRepository<CardTransaction, Long> {
    fun findByCardId(cardId: Long): List<CardTransaction>
    fun findByCardIdAndTransactionDateBetween(
        cardId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CardTransaction>
    fun findByCustomerId(customerId: Long): List<CardTransaction>
    fun findByTransactionId(transactionId: String): Optional<CardTransaction>
    fun findByStatus(status: TransactionStatus): List<CardTransaction>
}
```

#### CardBenefitRepository.kt
```kotlin
interface CardBenefitRepository : JpaRepository<CardBenefit, Long> {
    fun findByProductType(productType: CardProductType): List<CardBenefit>
    fun findByProductTypeAndStatus(productType: CardProductType, status: BenefitStatus): List<CardBenefit>
    fun findByStatus(status: BenefitStatus): List<CardBenefit>
}
```

#### CashbackHistoryRepository.kt
```kotlin
interface CashbackHistoryRepository : JpaRepository<CashbackHistory, Long> {
    fun findByCardId(cardId: Long): List<CashbackHistory>
    fun findByCustomerId(customerId: Long): List<CashbackHistory>
    fun findByTransactionId(transactionId: Long): List<CashbackHistory>
    fun findByStatus(status: CashbackStatus): List<CashbackHistory>
    fun findByStatusAndPaymentDate(status: CashbackStatus, paymentDate: LocalDate): List<CashbackHistory>
    fun countByCardIdAndEarnedDate(cardId: Long, earnedDate: LocalDate): Int
}
```

#### CardStatementRepository.kt
```kotlin
interface CardStatementRepository : JpaRepository<CardStatement, Long> {
    fun findByCardId(cardId: Long): List<CardStatement>
    fun findByCardIdAndStatementYearAndStatementMonth(
        cardId: Long,
        year: Int,
        month: Int
    ): Optional<CardStatement>
    fun findByStatus(status: StatementStatus): List<CardStatement>
    fun findByPaymentDueDateBeforeAndStatus(dueDate: LocalDate, status: StatementStatus): List<CardStatement>
}
```

**체크리스트**:
- [ ] 모든 Repository 인터페이스 생성 (6개)
- [ ] 필요한 커스텀 쿼리 메서드 정의
- [ ] JpaRepository 상속 확인

---

## Phase 2: Command/Result 패턴 구현

### 2.1 Command 객체 생성

**목표**: 입력 데이터를 표현하는 Command 객체 생성

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/command/CardCommand.kt`

```kotlin
package com.socoolheeya.bluebank.card.data.domain.command

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.Card
import java.math.BigDecimal
import java.time.LocalDate

sealed interface CardCommand {

    data class Create(
        val cardNumber: String,
        val cardNumberMasked: String,
        val customerId: Long,
        val accountId: Long,
        val cardType: CardType,
        val productType: CardProductType,
        val cardholderName: String,
        val expiryDate: LocalDate,
        val cvv: String,
        val dailyLimit: BigDecimal,
        val monthlyLimit: BigDecimal,
        val designCode: String,
        val customText: String? = null,
        val hasTransitCard: Boolean = false,
        val hasOverseasUsage: Boolean = true,
        val creditLimit: BigDecimal? = null,
        val annualFee: BigDecimal = BigDecimal.ZERO,
        val partnerCompany: String? = null,
        val moimAccountId: Long? = null,
        val applicationId: Long? = null,
        val issueFee: BigDecimal = BigDecimal.ZERO
    ) : CardCommand {
        fun toEntity(): Card {
            return Card(
                cardNumber = cardNumber,
                cardNumberMasked = cardNumberMasked,
                customerId = customerId,
                accountId = accountId,
                cardType = cardType,
                productType = productType,
                cardholderName = cardholderName,
                issueDate = LocalDate.now(),
                expiryDate = expiryDate,
                cvv = cvv,
                status = CardStatus.ISSUED,
                dailyLimit = dailyLimit,
                monthlyLimit = monthlyLimit,
                designCode = designCode,
                customText = customText,
                hasTransitCard = hasTransitCard,
                hasOverseasUsage = hasOverseasUsage,
                creditLimit = creditLimit,
                availableCredit = creditLimit,
                annualFee = annualFee,
                partnerCompany = partnerCompany,
                moimAccountId = moimAccountId,
                applicationId = applicationId,
                issueFee = issueFee
            )
        }
    }

    data class Activate(val cardId: Long) : CardCommand

    data class Suspend(val cardId: Long, val reason: String) : CardCommand

    data class Terminate(val cardId: Long) : CardCommand

    data class ToggleUsage(val cardId: Long, val enabled: Boolean) : CardCommand
}
```

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/command/CardApplicationCommand.kt`

```kotlin
sealed interface CardApplicationCommand {

    data class Submit(
        val customerId: Long,
        val accountId: Long,
        val cardType: CardType,
        val productType: CardProductType,
        val applicantName: String,
        val residentNumber: String,
        val phoneNumber: String,
        val email: String? = null,
        val address: String,
        val designCode: String,
        val customText: String? = null,
        val requestTransitCard: Boolean = false,
        val requestOverseasUsage: Boolean = true,
        val moimAccountId: Long? = null,
        val annualIncome: BigDecimal? = null,
        val employmentType: String? = null,
        val companyName: String? = null,
        val creditScore: Int? = null,
        val requestedCreditLimit: BigDecimal? = null
    ) : CardApplicationCommand {
        fun toEntity(): CardApplication {
            return CardApplication(
                customerId = customerId,
                accountId = accountId,
                cardType = cardType,
                productType = productType,
                applicantName = applicantName,
                residentNumber = residentNumber,
                phoneNumber = phoneNumber,
                email = email,
                address = address,
                designCode = designCode,
                customText = customText,
                requestTransitCard = requestTransitCard,
                requestOverseasUsage = requestOverseasUsage,
                moimAccountId = moimAccountId,
                annualIncome = annualIncome,
                employmentType = employmentType,
                companyName = companyName,
                creditScore = creditScore,
                requestedCreditLimit = requestedCreditLimit
            )
        }
    }
}
```

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/command/TransactionCommand.kt`

```kotlin
sealed interface TransactionCommand {

    data class Create(
        val cardId: Long,
        val customerId: Long,
        val transactionId: String,
        val merchantName: String,
        val merchantCategory: String,
        val transactionType: TransactionType,
        val amount: BigDecimal,
        val currency: String = "KRW",
        val merchantCountry: String = "KR",
        val merchantCity: String? = null,
        val isOverseas: Boolean = false,
        val installmentMonths: Int = 0
    ) : TransactionCommand {
        fun toEntity(): CardTransaction {
            return CardTransaction(
                cardId = cardId,
                customerId = customerId,
                transactionId = transactionId,
                merchantName = merchantName,
                merchantCategory = merchantCategory,
                transactionType = transactionType,
                amount = amount,
                currency = currency,
                transactionDate = LocalDateTime.now(),
                status = TransactionStatus.PENDING,
                merchantCountry = merchantCountry,
                merchantCity = merchantCity,
                isOverseas = isOverseas,
                installmentMonths = installmentMonths
            )
        }
    }
}
```

**체크리스트**:
- [ ] CardCommand.kt 생성
- [ ] CardApplicationCommand.kt 생성
- [ ] TransactionCommand.kt 생성
- [ ] toEntity() 메서드 구현
- [ ] 모든 Command 타입 정의

---

### 2.2 Result 객체 생성

**목표**: 출력 데이터를 표현하는 Result 객체 생성

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/result/CardResult.kt`

```kotlin
package com.socoolheeya.bluebank.card.data.domain.result

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.Card
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CardResult(
    val id: Long?,
    val cardNumber: String,
    val cardNumberMasked: String,
    val customerId: Long,
    val accountId: Long,
    val cardType: CardType,
    val productType: CardProductType,
    val cardholderName: String,
    val issueDate: LocalDate,
    val expiryDate: LocalDate,
    val status: CardStatus,
    val dailyLimit: BigDecimal,
    val monthlyLimit: BigDecimal,
    val dailyUsed: BigDecimal,
    val monthlyUsed: BigDecimal,
    val designCode: String,
    val customText: String?,
    val hasTransitCard: Boolean,
    val hasOverseasUsage: Boolean,
    val isEnabled: Boolean,
    val creditLimit: BigDecimal?,
    val availableCredit: BigDecimal?,
    val annualFee: BigDecimal,
    val nextPaymentDate: LocalDate?,
    val partnerCompany: String?,
    val moimAccountId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(card: Card): CardResult {
            return CardResult(
                id = card.id,
                cardNumber = card.cardNumber,
                cardNumberMasked = card.cardNumberMasked,
                customerId = card.customerId,
                accountId = card.accountId,
                cardType = card.cardType,
                productType = card.productType,
                cardholderName = card.cardholderName,
                issueDate = card.issueDate,
                expiryDate = card.expiryDate,
                status = card.status,
                dailyLimit = card.dailyLimit,
                monthlyLimit = card.monthlyLimit,
                dailyUsed = card.dailyUsed,
                monthlyUsed = card.monthlyUsed,
                designCode = card.designCode,
                customText = card.customText,
                hasTransitCard = card.hasTransitCard,
                hasOverseasUsage = card.hasOverseasUsage,
                isEnabled = card.isEnabled,
                creditLimit = card.creditLimit,
                availableCredit = card.availableCredit,
                annualFee = card.annualFee,
                nextPaymentDate = card.nextPaymentDate,
                partnerCompany = card.partnerCompany,
                moimAccountId = card.moimAccountId,
                createdAt = card.createdAt,
                updatedAt = card.updatedAt
            )
        }
    }
}
```

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/result/CardApplicationResult.kt`

```kotlin
data class CardApplicationResult(
    val id: Long?,
    val customerId: Long,
    val accountId: Long,
    val cardType: CardType,
    val productType: CardProductType,
    val applicantName: String,
    val phoneNumber: String,
    val email: String?,
    val designCode: String,
    val customText: String?,
    val requestTransitCard: Boolean,
    val status: CardApplicationStatus,
    val approvedCreditLimit: BigDecimal?,
    val rejectionReason: String?,
    val cardId: Long?,
    val appliedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?
) {
    companion object {
        fun from(application: CardApplication): CardApplicationResult {
            return CardApplicationResult(
                id = application.id,
                customerId = application.customerId,
                accountId = application.accountId,
                cardType = application.cardType,
                productType = application.productType,
                applicantName = application.applicantName,
                phoneNumber = application.phoneNumber,
                email = application.email,
                designCode = application.designCode,
                customText = application.customText,
                requestTransitCard = application.requestTransitCard,
                status = application.status,
                approvedCreditLimit = application.approvedCreditLimit,
                rejectionReason = application.rejectionReason,
                cardId = application.cardId,
                appliedAt = application.appliedAt,
                reviewedAt = application.reviewedAt
            )
        }
    }
}
```

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/result/TransactionResult.kt`

```kotlin
data class TransactionResult(
    val id: Long?,
    val cardId: Long,
    val customerId: Long,
    val transactionId: String,
    val merchantName: String,
    val merchantCategory: String,
    val transactionType: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val transactionDate: LocalDateTime,
    val approvalDate: LocalDateTime?,
    val settlementDate: LocalDate?,
    val status: TransactionStatus,
    val merchantCountry: String,
    val merchantCity: String?,
    val isOverseas: Boolean,
    val installmentMonths: Int,
    val approvalNumber: String?,
    val isApproved: Boolean
) {
    companion object {
        fun from(transaction: CardTransaction): TransactionResult {
            return TransactionResult(
                id = transaction.id,
                cardId = transaction.cardId,
                customerId = transaction.customerId,
                transactionId = transaction.transactionId,
                merchantName = transaction.merchantName,
                merchantCategory = transaction.merchantCategory,
                transactionType = transaction.transactionType,
                amount = transaction.amount,
                currency = transaction.currency,
                transactionDate = transaction.transactionDate,
                approvalDate = transaction.approvalDate,
                settlementDate = transaction.settlementDate,
                status = transaction.status,
                merchantCountry = transaction.merchantCountry,
                merchantCity = transaction.merchantCity,
                isOverseas = transaction.isOverseas,
                installmentMonths = transaction.installmentMonths,
                approvalNumber = transaction.approvalNumber,
                isApproved = transaction.isApproved
            )
        }
    }
}
```

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/domain/result/CashbackResult.kt`

```kotlin
data class CashbackResult(
    val id: Long?,
    val cardId: Long,
    val customerId: Long,
    val transactionId: Long,
    val cashbackAmount: BigDecimal,
    val cashbackRate: BigDecimal,
    val transactionAmount: BigDecimal,
    val cashbackType: CashbackType,
    val earnedDate: LocalDate,
    val paymentDate: LocalDate?,
    val actualPaymentDate: LocalDate?,
    val status: CashbackStatus
) {
    companion object {
        fun from(cashback: CashbackHistory): CashbackResult {
            return CashbackResult(
                id = cashback.id,
                cardId = cashback.cardId,
                customerId = cashback.customerId,
                transactionId = cashback.transactionId,
                cashbackAmount = cashback.cashbackAmount,
                cashbackRate = cashback.cashbackRate,
                transactionAmount = cashback.transactionAmount,
                cashbackType = cashback.cashbackType,
                earnedDate = cashback.earnedDate,
                paymentDate = cashback.paymentDate,
                actualPaymentDate = cashback.actualPaymentDate,
                status = cashback.status
            )
        }
    }
}
```

**체크리스트**:
- [ ] CardResult.kt 생성
- [ ] CardApplicationResult.kt 생성
- [ ] TransactionResult.kt 생성
- [ ] CashbackResult.kt 생성
- [ ] from() companion 메서드 구현

---

## Phase 3: DataService 구현 (트랜잭션 레이어)

### 3.1 CardDataService

**목표**: Card 관련 트랜잭션 처리

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/service/CardDataService.kt`

```kotlin
package com.socoolheeya.bluebank.card.data.service

import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardDataService(
    private val cardRepository: CardRepository
) {

    @Transactional
    fun createCard(command: CardCommand.Create): CardResult {
        // 1. 카드 번호 중복 확인
        if (cardRepository.existsByCardNumber(command.cardNumber)) {
            throw IllegalArgumentException("이미 존재하는 카드 번호입니다")
        }

        // 2. Entity 생성 및 저장
        val card = command.toEntity()
        val savedCard = cardRepository.save(card)

        return CardResult.from(savedCard)
    }

    @Transactional
    fun activateCard(command: CardCommand.Activate): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.activate()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun suspendCard(command: CardCommand.Suspend): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.suspend(command.reason)
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun terminateCard(command: CardCommand.Terminate): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.terminate()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun toggleCardUsage(command: CardCommand.ToggleUsage): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        if (command.enabled) card.enableUsage() else card.disableUsage()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
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

    @Transactional
    fun resetDailyLimits() {
        val activeCards = cardRepository.findByStatus(CardStatus.ACTIVE)
        activeCards.forEach { it.resetDailyUsage() }
        cardRepository.saveAll(activeCards)
    }

    @Transactional
    fun resetMonthlyLimits() {
        val activeCards = cardRepository.findByStatus(CardStatus.ACTIVE)
        activeCards.forEach { it.resetMonthlyUsage() }
        cardRepository.saveAll(activeCards)
    }
}
```

**체크리스트**:
- [ ] CardDataService.kt 생성
- [ ] @Transactional 애노테이션 추가
- [ ] CRUD 메서드 구현
- [ ] 한도 초기화 메서드 구현
- [ ] 예외 처리 추가

---

### 3.2 CardApplicationDataService

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/service/CardApplicationDataService.kt`

```kotlin
@Service
class CardApplicationDataService(
    private val cardApplicationRepository: CardApplicationRepository,
    private val cardRepository: CardRepository
) {

    @Transactional
    fun submitApplication(command: CardApplicationCommand.Submit): CardApplicationResult {
        val application = command.toEntity()
        val savedApplication = cardApplicationRepository.save(application)
        return CardApplicationResult.from(savedApplication)
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        creditLimit: BigDecimal?,
        cardCommand: CardCommand.Create
    ): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        // 1. Card 생성
        val card = cardCommand.toEntity()
        val savedCard = cardRepository.save(card)

        // 2. Application 승인 처리
        application.approve(creditLimit, savedCard.id!!)
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun rejectApplication(applicationId: Long, reason: String): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.reject(reason)
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun markAsIssued(applicationId: Long): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.markAsIssued()
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional(readOnly = true)
    fun getApplication(applicationId: Long): CardApplicationResult? {
        return cardApplicationRepository.findById(applicationId)
            .map { CardApplicationResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getApplicationsByCustomerId(customerId: Long): List<CardApplicationResult> {
        return cardApplicationRepository.findByCustomerId(customerId)
            .map { CardApplicationResult.from(it) }
    }
}
```

**체크리스트**:
- [ ] CardApplicationDataService.kt 생성
- [ ] 신청서 제출/승인/거절 로직 구현
- [ ] Card 생성 로직 통합

---

### 3.3 CardTransactionDataService

**파일**: `data/card-data/src/main/kotlin/com/socoolheeya/bluebank/card/data/service/CardTransactionDataService.kt`

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
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        // 2. 카드 상태 및 한도 검증
        require(card.status == CardStatus.ACTIVE) { "활성화된 카드만 사용 가능합니다" }
        require(card.isEnabled) { "카드 사용이 비활성화되어 있습니다" }
        require(card.validateDailyLimit(command.amount)) { "일 한도를 초과합니다" }
        require(card.validateMonthlyLimit(command.amount)) { "월 한도를 초과합니다" }

        // 3. 거래 생성
        val transaction = command.toEntity()
        val savedTransaction = cardTransactionRepository.save(transaction)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun approveTransaction(transactionId: Long, approvalNumber: String): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("거래를 찾을 수 없습니다: $transactionId") }

        // 승인 처리
        transaction.approve(approvalNumber)
        val savedTransaction = cardTransactionRepository.save(transaction)

        // 카드 사용액 누적
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        card.addUsage(transaction.amount)
        cardRepository.save(card)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun cancelTransaction(transactionId: Long): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("거래를 찾을 수 없습니다: $transactionId") }

        transaction.cancel()

        // 카드 사용액 차감
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        card.addUsage(transaction.amount.negate())
        cardRepository.save(card)

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
}
```

**체크리스트**:
- [ ] CardTransactionDataService.kt 생성
- [ ] 거래 생성 및 승인 로직 구현
- [ ] 한도 검증 로직 구현

---

(계속...)

## 우선순위

1. **최우선 (MVP)**: Phase 1-5 (Entity, Repository, DataService, DTO, Controller)
2. **중요**: Phase 6-7 (설정, 테스트)
3. **선택**: Phase 8-9 (고급 기능, 배치 작업)

---

## 참고사항

- Loan Service의 구현 패턴을 따라야 함
- data 모듈은 app 모듈을 의존하지 않음
- 모든 트랜잭션은 DataService에서 처리
- app 모듈은 비즈니스 로직만 담당
- 카드번호, CVV, 주민번호는 암호화 필수