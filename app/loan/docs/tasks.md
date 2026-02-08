# Loan Service 구현 태스크

## 개요

이 문서는 architecture.md를 기반으로 Loan Service를 단계별로 구현하는 태스크 목록입니다. Account Service의 구현 패턴을 따르며, data 모듈과 app 모듈의 명확한 책임 분리를 유지합니다.

---

## Phase 1: 기본 인프라 및 도메인 모델 구축

### 1.1 프로젝트 구조 생성

**목표**: data/loan-data와 app/loan 모듈의 기본 구조 생성

**작업 내용**:
```
data/loan-data/
├── src/main/kotlin/com/socoolheeya/bluebank/loan/data/
│   ├── domain/
│   │   ├── entity/          # Entity 클래스
│   │   ├── command/         # Command 객체
│   │   └── result/          # Result 객체
│   ├── repository/          # JPA Repository
│   └── service/             # DataService (트랜잭션)
└── build.gradle.kts

app/loan/
├── src/main/kotlin/com/socoolheeya/bluebank/loan/
│   ├── controller/          # REST API Controller
│   ├── service/             # Business Logic Service
│   ├── dto/                 # DTO (Request/Response)
│   └── LoanApplication.kt
├── src/main/resources/
│   └── application.yml
└── build.gradle.kts
```

**체크리스트**:
- [ ] data/loan-data 모듈 생성
- [ ] app/loan 모듈 생성
- [ ] 패키지 구조 생성
- [ ] build.gradle.kts 의존성 설정
  - data 모듈: spring-boot-starter-data-jpa, kotlin-reflect
  - app 모듈: data/loan-data 의존성, spring-boot-starter-web, spring-cloud-starter-openfeign

---

### 1.2 Enum 정의

**목표**: 대출 관련 모든 Enum 클래스 생성

**파일 위치**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/LoanEnums.kt`

**작업 내용**:
```kotlin
package com.socoolheeya.bluebank.loan.data.domain

object LoanEnums {

    enum class LoanType(val description: String) {
        CREDIT("신용대출"),
        SECURED("담보대출"),
        REFINANCE("대환대출")
    }

    enum class ProductType(val description: String) {
        // 신용대출
        EMERGENCY("비상금대출"),
        CREDIT_LINE("마이너스통장"),
        GENERAL_CREDIT("신용대출"),
        MID_CREDIT("중신용대출"),
        NEW_HOPE("새희망홀씨II"),
        TOGETHER("같이대출"),

        // 담보대출
        MORTGAGE("주택담보대출"),
        HF_MORTGAGE("HF 아낌e 보금자리론"),
        LEASE("전월세보증금대출"),
        AUTO_LOAN("자동차담보대출"),
        AUTO_LEASE("리스금융대출"),

        // 대환대출
        CREDIT_REFINANCE("신용대출 갈아타기"),
        MORTGAGE_REFINANCE("주택담보대출 갈아타기"),
        LEASE_REFINANCE("전월세보증금 갈아타기")
    }

    enum class LoanStatus(val description: String) {
        PENDING("심사 중"),
        APPROVED("승인"),
        ACTIVE("실행 중"),
        OVERDUE("연체"),
        SETTLED("완료"),
        REJECTED("거절"),
        CANCELLED("취소")
    }

    enum class ApplicationStatus(val description: String) {
        SUBMITTED("신청 완료"),
        UNDER_REVIEW("심사 중"),
        APPROVED("승인"),
        REJECTED("거절")
    }

    enum class RepaymentMethod(val description: String) {
        LUMP_SUM("만기일시상환"),
        EQUAL_PRINCIPAL("원금균등분할"),
        EQUAL_INSTALLMENT("원리금균등분할"),
        BALLOON("체증식분할상환")
    }

    enum class RepaymentType(val description: String) {
        SCHEDULED("정기 상환"),
        EARLY("중도 상환"),
        FINAL("만기 상환")
    }

    enum class RepaymentStatus(val description: String) {
        SCHEDULED("예정"),
        COMPLETED("완료"),
        OVERDUE("연체"),
        WAIVED("면제")
    }

    enum class RateType(val description: String) {
        FIXED("고정금리"),
        VARIABLE("변동금리")
    }

    enum class CollateralType(val description: String) {
        REAL_ESTATE("부동산"),
        VEHICLE("차량"),
        DEPOSIT("보증금")
    }

    enum class CollateralStatus(val description: String) {
        REGISTERED("등록 완료"),
        RELEASED("해제"),
        FORECLOSED("압류")
    }

    enum class InterestPaymentStatus(val description: String) {
        PENDING("미납"),
        PAID("납부 완료"),
        OVERDUE("연체")
    }
}
```

**체크리스트**:
- [ ] LoanEnums.kt 파일 생성
- [ ] 모든 Enum 정의 완료
- [ ] description 필드 추가
- [ ] Enum 값 검증

---

### 1.3 Entity 클래스 구현

**목표**: 6개 핵심 Entity 클래스 구현

#### 1.3.1 Loan Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/Loan.kt`

**작업 내용**:
```kotlin
package com.socoolheeya.bluebank.loan.data.domain.entity

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "loan")
class Loan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    @Column(unique = true, nullable = false)
    var loanNumber: String,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var accountId: Long,

    // 대출 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var loanType: LoanType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: ProductType,

    // 대출 금액 및 금리
    @Column(nullable = false, precision = 15, scale = 2)
    var principalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var outstandingBalance: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 3)
    var interestRate: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rateType: RateType,

    // 대출 기간
    @Column(nullable = false)
    var loanTerm: Int,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var maturityDate: LocalDate,

    // 상환 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var repaymentMethod: RepaymentMethod,

    @Column(precision = 15, scale = 2)
    var monthlyPayment: BigDecimal? = null,

    // 대출 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LoanStatus,

    // 담보 정보
    var collateralId: Long? = null,

    @Column(precision = 5, scale = 2)
    var loanToValueRatio: BigDecimal? = null,

    // 대환 정보
    var refinanceSourceLoanId: Long? = null,

    @Column(nullable = false)
    var isRefinanced: Boolean = false,

    // 우대 금리
    @Column(nullable = false, precision = 5, scale = 3)
    var preferentialRate: BigDecimal = BigDecimal.ZERO,

    var discountReason: String? = null,

    // 기타
    var creditScore: Int? = null,
    var approvedBy: String? = null,

    // 감사
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 대출 승인
     */
    fun approve(approver: String) {
        require(status == LoanStatus.PENDING) { "대출 심사 중인 상태만 승인 가능합니다" }
        this.status = LoanStatus.APPROVED
        this.approvedBy = approver
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 대출 실행
     */
    fun execute() {
        require(status == LoanStatus.APPROVED) { "승인된 대출만 실행 가능합니다" }
        this.status = LoanStatus.ACTIVE
        this.startDate = LocalDate.now()
        this.maturityDate = startDate.plusMonths(loanTerm.toLong())
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 대출 상환
     */
    fun repay(amount: BigDecimal) {
        require(status == LoanStatus.ACTIVE) { "실행 중인 대출만 상환 가능합니다" }
        require(amount <= outstandingBalance) { "상환 금액이 잔여 원금을 초과할 수 없습니다" }

        this.outstandingBalance = outstandingBalance.subtract(amount)
        this.updatedAt = LocalDateTime.now()

        if (outstandingBalance <= BigDecimal.ZERO) {
            this.status = LoanStatus.SETTLED
        }
    }

    /**
     * 대출 거절
     */
    fun reject(reason: String) {
        require(status == LoanStatus.PENDING) { "심사 중인 대출만 거절 가능합니다" }
        this.status = LoanStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 연체 처리
     */
    fun markAsOverdue() {
        require(status == LoanStatus.ACTIVE) { "실행 중인 대출만 연체 처리 가능합니다" }
        this.status = LoanStatus.OVERDUE
        this.updatedAt = LocalDateTime.now()
    }
}
```

**체크리스트**:
- [ ] Loan.kt 파일 생성
- [ ] 모든 필드 정의 (@Column 애노테이션 포함)
- [ ] 비즈니스 로직 메서드 구현 (approve, execute, repay, reject, markAsOverdue)
- [ ] JPA 애노테이션 설정
- [ ] BigDecimal precision/scale 설정

#### 1.3.2 LoanApplication Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/LoanApplication.kt`

**작업 내용**:
```kotlin
@Entity
@Table(name = "loan_application")
class LoanApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    var requestedAmount: BigDecimal,

    @Column(nullable = false)
    var requestedTerm: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: ProductType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var repaymentMethod: RepaymentMethod,

    // 고객 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var annualIncome: BigDecimal,

    @Column(nullable = false)
    var employmentType: String,

    @Column(nullable = false)
    var employmentPeriodMonths: Int,

    var companyName: String? = null,

    // 신용 정보
    @Column(nullable = false)
    var creditScore: Int,

    @Column(nullable = false)
    var existingLoanCount: Int = 0,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalExistingDebt: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var hasDelayHistory: Boolean = false,

    // 담보 정보
    var collateralType: String? = null,

    @Column(precision = 15, scale = 2)
    var collateralValue: BigDecimal? = null,

    var collateralAddress: String? = null,

    // 심사 결과
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApplicationStatus = ApplicationStatus.SUBMITTED,

    @Column(precision = 15, scale = 2)
    var approvedAmount: BigDecimal? = null,

    @Column(precision = 5, scale = 3)
    var approvedRate: BigDecimal? = null,

    @Column(length = 500)
    var rejectionReason: String? = null,

    var loanId: Long? = null,

    @Column(nullable = false)
    var appliedAt: LocalDateTime = LocalDateTime.now(),

    var reviewedAt: LocalDateTime? = null
) {

    fun approve(amount: BigDecimal, rate: BigDecimal, loanId: Long) {
        this.status = ApplicationStatus.APPROVED
        this.approvedAmount = amount
        this.approvedRate = rate
        this.loanId = loanId
        this.reviewedAt = LocalDateTime.now()
    }

    fun reject(reason: String) {
        this.status = ApplicationStatus.REJECTED
        this.rejectionReason = reason
        this.reviewedAt = LocalDateTime.now()
    }

    fun startReview() {
        require(status == ApplicationStatus.SUBMITTED) { "제출된 신청서만 심사 시작 가능합니다" }
        this.status = ApplicationStatus.UNDER_REVIEW
    }
}
```

**체크리스트**:
- [ ] LoanApplication.kt 생성
- [ ] 모든 필드 정의
- [ ] 비즈니스 로직 메서드 구현
- [ ] 유효성 검증 추가

#### 1.3.3 Repayment Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/Repayment.kt`

```kotlin
@Entity
@Table(name = "repayment")
class Repayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var repaymentType: RepaymentType,

    @Column(nullable = false, precision = 15, scale = 2)
    var principalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var interestAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var balanceAfter: BigDecimal,

    @Column(nullable = false)
    var scheduledDate: LocalDate,

    var actualDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RepaymentStatus = RepaymentStatus.SCHEDULED,

    @Column(nullable = false)
    var isOverdue: Boolean = false,

    @Column(nullable = false)
    var overdueDays: Int = 0,

    @Column(nullable = false, precision = 15, scale = 2)
    var penaltyAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun process() {
        require(status == RepaymentStatus.SCHEDULED) { "예정된 상환만 처리 가능합니다" }
        this.status = RepaymentStatus.COMPLETED
        this.actualDate = LocalDate.now()

        // 연체 체크
        if (LocalDate.now().isAfter(scheduledDate)) {
            this.isOverdue = true
            this.overdueDays = java.time.temporal.ChronoUnit.DAYS.between(scheduledDate, LocalDate.now()).toInt()
        }
    }

    fun markOverdue(days: Int, penalty: BigDecimal) {
        this.status = RepaymentStatus.OVERDUE
        this.isOverdue = true
        this.overdueDays = days
        this.penaltyAmount = penalty
    }
}
```

**체크리스트**:
- [ ] Repayment.kt 생성
- [ ] 상환 처리 로직 구현
- [ ] 연체 처리 로직 구현

#### 1.3.4 Collateral Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/Collateral.kt`

```kotlin
@Entity
@Table(name = "collateral")
class Collateral(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var collateralType: CollateralType,

    // 부동산 담보
    var address: String? = null,

    @Column(precision = 10, scale = 2)
    var area: BigDecimal? = null,

    var buildingType: String? = null,

    var completionYear: Int? = null,

    // 자동차 담보
    var vehicleModel: String? = null,

    var vehicleYear: Int? = null,

    var vehicleNumber: String? = null,

    // 평가 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var appraisedValue: BigDecimal,

    @Column(nullable = false)
    var appraisalDate: LocalDate,

    @Column(nullable = false)
    var appraisalInstitution: String,

    // 등기 정보
    var registrationNumber: String? = null,

    var registrationDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CollateralStatus = CollateralStatus.REGISTERED,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun release() {
        require(status == CollateralStatus.REGISTERED) { "등록된 담보만 해제 가능합니다" }
        this.status = CollateralStatus.RELEASED
    }

    fun foreclose() {
        this.status = CollateralStatus.FORECLOSED
    }
}
```

**체크리스트**:
- [ ] Collateral.kt 생성
- [ ] 담보 유형별 필드 정의
- [ ] 상태 변경 메서드 구현

#### 1.3.5 InterestPayment Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/InterestPayment.kt`

```kotlin
@Entity
@Table(name = "interest_payment")
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    var interestAmount: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 3)
    var interestRate: BigDecimal,

    @Column(nullable = false)
    var calculationPeriodStart: LocalDate,

    @Column(nullable = false)
    var calculationPeriodEnd: LocalDate,

    @Column(nullable = false, precision = 15, scale = 2)
    var principalBalance: BigDecimal,

    @Column(nullable = false)
    var dueDate: LocalDate,

    var paidDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InterestPaymentStatus = InterestPaymentStatus.PENDING,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun pay() {
        require(status == InterestPaymentStatus.PENDING) { "미납 상태의 이자만 납부 가능합니다" }
        this.status = InterestPaymentStatus.PAID
        this.paidDate = LocalDate.now()
    }

    fun markOverdue() {
        this.status = InterestPaymentStatus.OVERDUE
    }
}
```

**체크리스트**:
- [ ] InterestPayment.kt 생성
- [ ] 이자 납부 로직 구현

#### 1.3.6 CreditScoreHistory Entity

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/CreditScoreHistory.kt`

```kotlin
@Entity
@Table(name = "credit_score_history")
class CreditScoreHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var customerId: Long,

    var loanApplicationId: Long? = null,

    @Column(nullable = false)
    var creditScore: Int,

    @Column(nullable = false)
    var creditGrade: String,

    @Column(nullable = false)
    var scoringAgency: String,

    @Column(nullable = false)
    var scoredAt: LocalDateTime,

    // 세부 점수
    var paymentHistory: Int? = null,

    var creditUsage: Int? = null,

    @Column(precision = 15, scale = 2)
    var debtAmount: BigDecimal? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

**체크리스트**:
- [ ] CreditScoreHistory.kt 생성
- [ ] 신용점수 이력 필드 정의

---

### 1.4 Repository 인터페이스 생성

**목표**: 각 Entity에 대한 JPA Repository 생성

**파일 위치**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/repository/`

#### LoanRepository.kt
```kotlin
package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LoanRepository : JpaRepository<Loan, Long> {
    fun findByLoanNumber(loanNumber: String): Optional<Loan>
    fun findByCustomerId(customerId: Long): List<Loan>
    fun findByCustomerIdAndStatus(customerId: Long, status: LoanStatus): List<Loan>
    fun findByStatus(status: LoanStatus): List<Loan>
    fun existsByLoanNumber(loanNumber: String): Boolean
}
```

#### LoanApplicationRepository.kt
```kotlin
interface LoanApplicationRepository : JpaRepository<LoanApplication, Long> {
    fun findByCustomerId(customerId: Long): List<LoanApplication>
    fun findByCustomerIdAndStatus(customerId: Long, status: ApplicationStatus): List<LoanApplication>
    fun findByStatus(status: ApplicationStatus): List<LoanApplication>
    fun findByLoanId(loanId: Long): Optional<LoanApplication>
}
```

#### RepaymentRepository.kt
```kotlin
interface RepaymentRepository : JpaRepository<Repayment, Long> {
    fun findByLoanId(loanId: Long): List<Repayment>
    fun findByLoanIdAndStatus(loanId: Long, status: RepaymentStatus): List<Repayment>
    fun findByStatus(status: RepaymentStatus): List<Repayment>
    fun findByScheduledDateBetween(startDate: LocalDate, endDate: LocalDate): List<Repayment>
}
```

#### CollateralRepository.kt
```kotlin
interface CollateralRepository : JpaRepository<Collateral, Long> {
    fun findByLoanId(loanId: Long): Optional<Collateral>
    fun findByStatus(status: CollateralStatus): List<Collateral>
}
```

#### InterestPaymentRepository.kt
```kotlin
interface InterestPaymentRepository : JpaRepository<InterestPayment, Long> {
    fun findByLoanId(loanId: Long): List<InterestPayment>
    fun findByStatus(status: InterestPaymentStatus): List<InterestPayment>
    fun findByDueDateBefore(date: LocalDate): List<InterestPayment>
}
```

#### CreditScoreHistoryRepository.kt
```kotlin
interface CreditScoreHistoryRepository : JpaRepository<CreditScoreHistory, Long> {
    fun findByCustomerId(customerId: Long): List<CreditScoreHistory>
    fun findByCustomerIdOrderByScoredAtDesc(customerId: Long): List<CreditScoreHistory>
    fun findByLoanApplicationId(applicationId: Long): Optional<CreditScoreHistory>
}
```

**체크리스트**:
- [ ] 모든 Repository 인터페이스 생성
- [ ] 필요한 커스텀 쿼리 메서드 정의
- [ ] JpaRepository 상속 확인

---

## Phase 2: Command/Result 패턴 구현

### 2.1 Command 객체 생성

**목표**: 입력 데이터를 표현하는 Command 객체 생성

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/command/LoanCommand.kt`

```kotlin
package com.socoolheeya.bluebank.loan.data.domain.command

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.*
import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import java.math.BigDecimal
import java.time.LocalDate

sealed interface LoanCommand {

    data class Create(
        val loanNumber: String,
        val customerId: Long,
        val accountId: Long,
        val loanType: LoanType,
        val productType: ProductType,
        val principalAmount: BigDecimal,
        val interestRate: BigDecimal,
        val rateType: RateType,
        val loanTerm: Int,
        val repaymentMethod: RepaymentMethod,
        val monthlyPayment: BigDecimal? = null,
        val collateralId: Long? = null,
        val loanToValueRatio: BigDecimal? = null,
        val creditScore: Int? = null,
        val preferentialRate: BigDecimal = BigDecimal.ZERO,
        val discountReason: String? = null
    ) : LoanCommand {
        fun toEntity(): Loan {
            return Loan(
                loanNumber = loanNumber,
                customerId = customerId,
                accountId = accountId,
                loanType = loanType,
                productType = productType,
                principalAmount = principalAmount,
                outstandingBalance = principalAmount,
                interestRate = interestRate,
                rateType = rateType,
                loanTerm = loanTerm,
                startDate = LocalDate.now(),
                maturityDate = LocalDate.now().plusMonths(loanTerm.toLong()),
                repaymentMethod = repaymentMethod,
                monthlyPayment = monthlyPayment,
                status = LoanStatus.PENDING,
                collateralId = collateralId,
                loanToValueRatio = loanToValueRatio,
                creditScore = creditScore,
                preferentialRate = preferentialRate,
                discountReason = discountReason
            )
        }
    }

    data class Approve(
        val loanId: Long,
        val approver: String
    ) : LoanCommand

    data class Execute(
        val loanId: Long,
        val accountId: Long
    ) : LoanCommand

    data class Repay(
        val loanId: Long,
        val amount: BigDecimal,
        val repaymentType: RepaymentType
    ) : LoanCommand

    data class Reject(
        val loanId: Long,
        val reason: String
    ) : LoanCommand
}
```

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/command/LoanApplicationCommand.kt`

```kotlin
sealed interface LoanApplicationCommand {

    data class Submit(
        val customerId: Long,
        val requestedAmount: BigDecimal,
        val requestedTerm: Int,
        val productType: ProductType,
        val repaymentMethod: RepaymentMethod,
        val annualIncome: BigDecimal,
        val employmentType: String,
        val employmentPeriodMonths: Int,
        val companyName: String? = null,
        val creditScore: Int,
        val existingLoanCount: Int = 0,
        val totalExistingDebt: BigDecimal = BigDecimal.ZERO,
        val hasDelayHistory: Boolean = false,
        val collateralType: String? = null,
        val collateralValue: BigDecimal? = null,
        val collateralAddress: String? = null
    ) : LoanApplicationCommand {
        fun toEntity(): LoanApplication {
            return LoanApplication(
                customerId = customerId,
                requestedAmount = requestedAmount,
                requestedTerm = requestedTerm,
                productType = productType,
                repaymentMethod = repaymentMethod,
                annualIncome = annualIncome,
                employmentType = employmentType,
                employmentPeriodMonths = employmentPeriodMonths,
                companyName = companyName,
                creditScore = creditScore,
                existingLoanCount = existingLoanCount,
                totalExistingDebt = totalExistingDebt,
                hasDelayHistory = hasDelayHistory,
                collateralType = collateralType,
                collateralValue = collateralValue,
                collateralAddress = collateralAddress
            )
        }
    }
}
```

**체크리스트**:
- [ ] LoanCommand.kt 생성
- [ ] LoanApplicationCommand.kt 생성
- [ ] toEntity() 메서드 구현
- [ ] 모든 Command 타입 정의

---

### 2.2 Result 객체 생성

**목표**: 출력 데이터를 표현하는 Result 객체 생성

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/result/LoanResult.kt`

```kotlin
package com.socoolheeya.bluebank.loan.data.domain.result

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.*
import com.socoolheeya.bluebank.loan.data.domain.entity.Loan
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class LoanResult(
    val id: Long?,
    val loanNumber: String,
    val customerId: Long,
    val accountId: Long,
    val loanType: LoanType,
    val productType: ProductType,
    val principalAmount: BigDecimal,
    val outstandingBalance: BigDecimal,
    val interestRate: BigDecimal,
    val rateType: RateType,
    val loanTerm: Int,
    val startDate: LocalDate,
    val maturityDate: LocalDate,
    val repaymentMethod: RepaymentMethod,
    val monthlyPayment: BigDecimal?,
    val status: LoanStatus,
    val collateralId: Long?,
    val loanToValueRatio: BigDecimal?,
    val creditScore: Int?,
    val preferentialRate: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(loan: Loan): LoanResult {
            return LoanResult(
                id = loan.id,
                loanNumber = loan.loanNumber,
                customerId = loan.customerId,
                accountId = loan.accountId,
                loanType = loan.loanType,
                productType = loan.productType,
                principalAmount = loan.principalAmount,
                outstandingBalance = loan.outstandingBalance,
                interestRate = loan.interestRate,
                rateType = loan.rateType,
                loanTerm = loan.loanTerm,
                startDate = loan.startDate,
                maturityDate = loan.maturityDate,
                repaymentMethod = loan.repaymentMethod,
                monthlyPayment = loan.monthlyPayment,
                status = loan.status,
                collateralId = loan.collateralId,
                loanToValueRatio = loan.loanToValueRatio,
                creditScore = loan.creditScore,
                preferentialRate = loan.preferentialRate,
                createdAt = loan.createdAt,
                updatedAt = loan.updatedAt
            )
        }
    }
}
```

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/result/LoanApplicationResult.kt`

```kotlin
data class LoanApplicationResult(
    val id: Long?,
    val customerId: Long,
    val requestedAmount: BigDecimal,
    val requestedTerm: Int,
    val productType: ProductType,
    val repaymentMethod: RepaymentMethod,
    val annualIncome: BigDecimal,
    val creditScore: Int,
    val status: ApplicationStatus,
    val approvedAmount: BigDecimal?,
    val approvedRate: BigDecimal?,
    val rejectionReason: String?,
    val loanId: Long?,
    val appliedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?
) {
    companion object {
        fun from(application: LoanApplication): LoanApplicationResult {
            return LoanApplicationResult(
                id = application.id,
                customerId = application.customerId,
                requestedAmount = application.requestedAmount,
                requestedTerm = application.requestedTerm,
                productType = application.productType,
                repaymentMethod = application.repaymentMethod,
                annualIncome = application.annualIncome,
                creditScore = application.creditScore,
                status = application.status,
                approvedAmount = application.approvedAmount,
                approvedRate = application.approvedRate,
                rejectionReason = application.rejectionReason,
                loanId = application.loanId,
                appliedAt = application.appliedAt,
                reviewedAt = application.reviewedAt
            )
        }
    }
}
```

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/domain/result/RepaymentResult.kt`

```kotlin
data class RepaymentResult(
    val id: Long?,
    val loanId: Long,
    val repaymentType: RepaymentType,
    val principalAmount: BigDecimal,
    val interestAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val balanceAfter: BigDecimal,
    val scheduledDate: LocalDate,
    val actualDate: LocalDate?,
    val status: RepaymentStatus,
    val isOverdue: Boolean,
    val overdueDays: Int,
    val penaltyAmount: BigDecimal
) {
    companion object {
        fun from(repayment: Repayment): RepaymentResult {
            return RepaymentResult(
                id = repayment.id,
                loanId = repayment.loanId,
                repaymentType = repayment.repaymentType,
                principalAmount = repayment.principalAmount,
                interestAmount = repayment.interestAmount,
                totalAmount = repayment.totalAmount,
                balanceAfter = repayment.balanceAfter,
                scheduledDate = repayment.scheduledDate,
                actualDate = repayment.actualDate,
                status = repayment.status,
                isOverdue = repayment.isOverdue,
                overdueDays = repayment.overdueDays,
                penaltyAmount = repayment.penaltyAmount
            )
        }
    }
}
```

**체크리스트**:
- [ ] LoanResult.kt 생성
- [ ] LoanApplicationResult.kt 생성
- [ ] RepaymentResult.kt 생성
- [ ] from() companion 메서드 구현

---

## Phase 3: DataService 구현 (트랜잭션 레이어)

### 3.1 LoanDataService

**목표**: Loan 관련 트랜잭션 처리

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/service/LoanDataService.kt`

```kotlin
package com.socoolheeya.bluebank.loan.data.service

import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class LoanDataService(
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun createLoan(command: LoanCommand.Create): LoanResult {
        // 1. 대출 번호 중복 확인
        if (loanRepository.existsByLoanNumber(command.loanNumber)) {
            throw IllegalArgumentException("이미 존재하는 대출 번호입니다: ${command.loanNumber}")
        }

        // 2. Entity 생성 및 저장
        val loan = command.toEntity()
        val savedLoan = loanRepository.save(loan)

        return LoanResult.from(savedLoan)
    }

    @Transactional
    fun approveLoan(command: LoanCommand.Approve): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.approve(command.approver)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun executeLoan(command: LoanCommand.Execute): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.execute()
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun repayLoan(command: LoanCommand.Repay): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.repay(command.amount)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun rejectLoan(command: LoanCommand.Reject): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.reject(command.reason)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional(readOnly = true)
    fun getLoan(loanId: Long): LoanResult? {
        return loanRepository.findById(loanId)
            .map { LoanResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getLoanByNumber(loanNumber: String): LoanResult? {
        return loanRepository.findByLoanNumber(loanNumber)
            .map { LoanResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getLoansByCustomerId(customerId: Long): List<LoanResult> {
        return loanRepository.findByCustomerId(customerId)
            .map { LoanResult.from(it) }
    }
}
```

**체크리스트**:
- [ ] LoanDataService.kt 생성
- [ ] @Transactional 애노테이션 추가
- [ ] CRUD 메서드 구현
- [ ] 예외 처리 추가

---

### 3.2 LoanApplicationDataService

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/service/LoanApplicationDataService.kt`

```kotlin
@Service
class LoanApplicationDataService(
    private val loanApplicationRepository: LoanApplicationRepository,
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun submitApplication(command: LoanApplicationCommand.Submit): LoanApplicationResult {
        val application = command.toEntity()
        val savedApplication = loanApplicationRepository.save(application)
        return LoanApplicationResult.from(savedApplication)
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        approvedAmount: BigDecimal,
        approvedRate: BigDecimal,
        loanCommand: LoanCommand.Create
    ): LoanApplicationResult {
        val application = loanApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        // 1. Loan 생성
        val loan = loanCommand.toEntity()
        val savedLoan = loanRepository.save(loan)

        // 2. Application 승인 처리
        application.approve(approvedAmount, approvedRate, savedLoan.id!!)
        val updatedApplication = loanApplicationRepository.save(application)

        return LoanApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun rejectApplication(applicationId: Long, reason: String): LoanApplicationResult {
        val application = loanApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.reject(reason)
        val updatedApplication = loanApplicationRepository.save(application)

        return LoanApplicationResult.from(updatedApplication)
    }

    @Transactional(readOnly = true)
    fun getApplication(applicationId: Long): LoanApplicationResult? {
        return loanApplicationRepository.findById(applicationId)
            .map { LoanApplicationResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getApplicationsByCustomerId(customerId: Long): List<LoanApplicationResult> {
        return loanApplicationRepository.findByCustomerId(customerId)
            .map { LoanApplicationResult.from(it) }
    }
}
```

**체크리스트**:
- [ ] LoanApplicationDataService.kt 생성
- [ ] 신청서 제출/승인/거절 로직 구현
- [ ] Loan 생성 로직 통합

---

### 3.3 RepaymentDataService

**파일**: `data/loan-data/src/main/kotlin/com/socoolheeya/bluebank/loan/data/service/RepaymentDataService.kt`

```kotlin
@Service
class RepaymentDataService(
    private val repaymentRepository: RepaymentRepository,
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun createRepayment(
        loanId: Long,
        repaymentType: RepaymentType,
        principalAmount: BigDecimal,
        interestAmount: BigDecimal,
        scheduledDate: LocalDate
    ): RepaymentResult {
        val loan = loanRepository.findById(loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: $loanId") }

        val repayment = Repayment(
            loanId = loanId,
            repaymentType = repaymentType,
            principalAmount = principalAmount,
            interestAmount = interestAmount,
            totalAmount = principalAmount.add(interestAmount),
            balanceAfter = loan.outstandingBalance.subtract(principalAmount),
            scheduledDate = scheduledDate
        )

        val savedRepayment = repaymentRepository.save(repayment)
        return RepaymentResult.from(savedRepayment)
    }

    @Transactional
    fun processRepayment(repaymentId: Long): RepaymentResult {
        val repayment = repaymentRepository.findById(repaymentId)
            .orElseThrow { NoSuchElementException("상환 내역을 찾을 수 없습니다: $repaymentId") }

        repayment.process()
        val updatedRepayment = repaymentRepository.save(repayment)

        return RepaymentResult.from(updatedRepayment)
    }

    @Transactional(readOnly = true)
    fun getRepaymentsByLoanId(loanId: Long): List<RepaymentResult> {
        return repaymentRepository.findByLoanId(loanId)
            .map { RepaymentResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getScheduledRepayments(startDate: LocalDate, endDate: LocalDate): List<RepaymentResult> {
        return repaymentRepository.findByScheduledDateBetween(startDate, endDate)
            .map { RepaymentResult.from(it) }
    }
}
```

**체크리스트**:
- [ ] RepaymentDataService.kt 생성
- [ ] 상환 생성 및 처리 로직 구현
- [ ] 스케줄 조회 기능 구현

---

## Phase 4: App Service 구현 (비즈니스 로직)

### 4.1 LoanService

**목표**: 대출 비즈니스 로직 구현

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/service/LoanService.kt`

```kotlin
package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.account.adapter.AccountClient
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.service.LoanDataService
import com.socoolheeya.bluebank.loan.dto.LoanDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class LoanService(
    private val loanDataService: LoanDataService,
    private val accountClient: AccountClient
) {

    fun getLoan(loanId: Long): LoanDto.Response {
        val result = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        return LoanDto.Response.from(result)
    }

    fun getLoansByCustomerId(customerId: Long): List<LoanDto.Response> {
        val results = loanDataService.getLoansByCustomerId(customerId)
        return results.map { LoanDto.Response.from(it) }
    }

    fun executeLoan(loanId: Long): LoanDto.Response {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        // 2. 계좌에 대출금 입금
        accountClient.deposit(
            accountId = loan.accountId,
            amount = loan.principalAmount,
            description = "대출금 입금 - ${loan.loanNumber}"
        )

        // 3. 대출 실행 처리
        val command = LoanCommand.Execute(
            loanId = loanId,
            accountId = loan.accountId
        )
        val result = loanDataService.executeLoan(command)

        return LoanDto.Response.from(result)
    }

    fun repayLoan(loanId: Long, amount: BigDecimal): LoanDto.Response {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        // 2. 계좌에서 출금
        accountClient.withdraw(
            accountId = loan.accountId,
            amount = amount,
            description = "대출 상환 - ${loan.loanNumber}"
        )

        // 3. 대출 상환 처리
        val command = LoanCommand.Repay(
            loanId = loanId,
            amount = amount,
            repaymentType = RepaymentType.EARLY
        )
        val result = loanDataService.repayLoan(command)

        return LoanDto.Response.from(result)
    }
}
```

**체크리스트**:
- [ ] LoanService.kt 생성
- [ ] AccountClient 연동
- [ ] 대출 실행 로직 구현
- [ ] 대출 상환 로직 구현

---

### 4.2 LoanApplicationService

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/service/LoanApplicationService.kt`

```kotlin
@Service
class LoanApplicationService(
    private val loanApplicationDataService: LoanApplicationDataService,
    private val loanDataService: LoanDataService,
    private val accountClient: AccountClient,
    private val creditScoreService: CreditScoreService
) {

    fun applyForLoan(request: LoanApplicationDto.Request): LoanApplicationDto.Response {
        // 1. 신용점수 조회
        val creditScore = creditScoreService.getCreditScore(request.customerId)

        // 2. 계좌 검증
        val accountValidation = accountClient.validateAccount(request.accountId)
        require(accountValidation.isValid) { "유효하지 않은 계좌입니다" }

        // 3. 대출 가능 여부 검증
        validateLoanEligibility(request, creditScore.score)

        // 4. 신청서 제출
        val command = LoanApplicationCommand.Submit(
            customerId = request.customerId,
            requestedAmount = request.amount,
            requestedTerm = request.term,
            productType = request.productType,
            repaymentMethod = request.repaymentMethod,
            annualIncome = request.annualIncome,
            employmentType = request.employmentType,
            employmentPeriodMonths = request.employmentPeriodMonths,
            companyName = request.companyName,
            creditScore = creditScore.score,
            existingLoanCount = request.existingLoanCount,
            totalExistingDebt = request.totalExistingDebt,
            hasDelayHistory = request.hasDelayHistory
        )

        val result = loanApplicationDataService.submitApplication(command)

        return LoanApplicationDto.Response.from(result)
    }

    fun approveApplication(applicationId: Long, approvedAmount: BigDecimal, approvedRate: BigDecimal): LoanApplicationDto.Response {
        // 신청서 조회
        val application = loanApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId")

        // Loan 생성을 위한 Command
        val loanNumber = generateLoanNumber()
        val loanCommand = LoanCommand.Create(
            loanNumber = loanNumber,
            customerId = application.customerId,
            accountId = 0L, // 실행 시 설정
            loanType = determineLoanType(application.productType),
            productType = application.productType,
            principalAmount = approvedAmount,
            interestRate = approvedRate,
            rateType = RateType.VARIABLE,
            loanTerm = application.requestedTerm,
            repaymentMethod = application.repaymentMethod,
            creditScore = application.creditScore
        )

        val result = loanApplicationDataService.approveApplication(
            applicationId = applicationId,
            approvedAmount = approvedAmount,
            approvedRate = approvedRate,
            loanCommand = loanCommand
        )

        return LoanApplicationDto.Response.from(result)
    }

    fun rejectApplication(applicationId: Long, reason: String): LoanApplicationDto.Response {
        val result = loanApplicationDataService.rejectApplication(applicationId, reason)
        return LoanApplicationDto.Response.from(result)
    }

    private fun validateLoanEligibility(request: LoanApplicationDto.Request, creditScore: Int) {
        // 신용점수 기준 검증
        val minScore = getMinimumCreditScore(request.productType)
        require(creditScore >= minScore) {
            "최소 신용점수 미달: 필요 ${minScore}점, 현재 ${creditScore}점"
        }

        // 소득 기준 검증
        val minIncome = getMinimumIncome(request.productType)
        require(request.annualIncome >= minIncome) {
            "최소 연소득 미달: 필요 ${minIncome}원"
        }

        // DSR 검증
        val dsr = calculateDSR(request.annualIncome, request.totalExistingDebt, request.amount)
        require(dsr <= BigDecimal("40")) {
            "DSR 한도 초과: 현재 ${dsr}%, 최대 40%"
        }
    }

    private fun calculateDSR(
        annualIncome: BigDecimal,
        existingDebt: BigDecimal,
        newLoanAmount: BigDecimal
    ): BigDecimal {
        // 간단한 DSR 계산 (실제로는 더 복잡)
        val totalDebt = existingDebt.add(newLoanAmount)
        return totalDebt.divide(annualIncome, 2, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
    }

    private fun getMinimumCreditScore(productType: ProductType): Int {
        return when (productType) {
            ProductType.EMERGENCY -> 600
            ProductType.CREDIT_LINE -> 700
            ProductType.GENERAL_CREDIT -> 700
            ProductType.MID_CREDIT -> 500
            ProductType.HF_MORTGAGE -> 271
            else -> 600
        }
    }

    private fun getMinimumIncome(productType: ProductType): BigDecimal {
        return when (productType) {
            ProductType.EMERGENCY -> BigDecimal("12000000")
            ProductType.CREDIT_LINE -> BigDecimal("30000000")
            ProductType.GENERAL_CREDIT -> BigDecimal("25000000")
            else -> BigDecimal("15000000")
        }
    }

    private fun determineLoanType(productType: ProductType): LoanType {
        return when (productType) {
            ProductType.EMERGENCY, ProductType.CREDIT_LINE, ProductType.GENERAL_CREDIT,
            ProductType.MID_CREDIT, ProductType.NEW_HOPE, ProductType.TOGETHER -> LoanType.CREDIT

            ProductType.MORTGAGE, ProductType.HF_MORTGAGE, ProductType.LEASE,
            ProductType.AUTO_LOAN, ProductType.AUTO_LEASE -> LoanType.SECURED

            ProductType.CREDIT_REFINANCE, ProductType.MORTGAGE_REFINANCE,
            ProductType.LEASE_REFINANCE -> LoanType.REFINANCE
        }
    }

    private fun generateLoanNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "LN$timestamp$random"
    }
}
```

**체크리스트**:
- [ ] LoanApplicationService.kt 생성
- [ ] 신청서 제출 로직 구현
- [ ] 심사 로직 구현 (DSR 계산, 신용점수 검증)
- [ ] 승인/거절 로직 구현
- [ ] CreditScoreService 연동

---

### 4.3 CreditScoreService (Mock)

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/service/CreditScoreService.kt`

```kotlin
@Service
class CreditScoreService {

    data class CreditScoreResult(
        val score: Int,
        val grade: String,
        val agency: String
    )

    fun getCreditScore(customerId: Long): CreditScoreResult {
        // TODO: 실제 신용평가 기관 API 연동
        // 현재는 Mock 데이터 반환
        return CreditScoreResult(
            score = 750,
            grade = "3등급",
            agency = "NICE"
        )
    }
}
```

**체크리스트**:
- [ ] CreditScoreService.kt 생성
- [ ] Mock 구현
- [ ] 추후 실제 API 연동 준비

---

## Phase 5: DTO 및 Controller 구현

### 5.1 DTO 클래스

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/dto/LoanDto.kt`

```kotlin
package com.socoolheeya.bluebank.loan.dto

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.*
import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object LoanDto {

    data class Response(
        val id: Long?,
        val loanNumber: String,
        val customerId: Long,
        val accountId: Long,
        val loanType: String,
        val productType: String,
        val principalAmount: BigDecimal,
        val outstandingBalance: BigDecimal,
        val interestRate: BigDecimal,
        val rateType: String,
        val loanTerm: Int,
        val startDate: LocalDate,
        val maturityDate: LocalDate,
        val repaymentMethod: String,
        val monthlyPayment: BigDecimal?,
        val status: String,
        val creditScore: Int?,
        val createdAt: LocalDateTime
    ) {
        companion object {
            fun from(result: LoanResult): Response {
                return Response(
                    id = result.id,
                    loanNumber = result.loanNumber,
                    customerId = result.customerId,
                    accountId = result.accountId,
                    loanType = result.loanType.description,
                    productType = result.productType.description,
                    principalAmount = result.principalAmount,
                    outstandingBalance = result.outstandingBalance,
                    interestRate = result.interestRate,
                    rateType = result.rateType.description,
                    loanTerm = result.loanTerm,
                    startDate = result.startDate,
                    maturityDate = result.maturityDate,
                    repaymentMethod = result.repaymentMethod.description,
                    monthlyPayment = result.monthlyPayment,
                    status = result.status.description,
                    creditScore = result.creditScore,
                    createdAt = result.createdAt
                )
            }
        }
    }

    data class RepayRequest(
        val amount: BigDecimal
    )
}
```

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/dto/LoanApplicationDto.kt`

```kotlin
object LoanApplicationDto {

    data class Request(
        val customerId: Long,
        val accountId: Long,
        val productType: ProductType,
        val amount: BigDecimal,
        val term: Int,
        val repaymentMethod: RepaymentMethod,
        val annualIncome: BigDecimal,
        val employmentType: String,
        val employmentPeriodMonths: Int,
        val companyName: String? = null,
        val existingLoanCount: Int = 0,
        val totalExistingDebt: BigDecimal = BigDecimal.ZERO,
        val hasDelayHistory: Boolean = false
    )

    data class Response(
        val id: Long?,
        val customerId: Long,
        val requestedAmount: BigDecimal,
        val requestedTerm: Int,
        val productType: String,
        val status: String,
        val approvedAmount: BigDecimal?,
        val approvedRate: BigDecimal?,
        val rejectionReason: String?,
        val loanId: Long?,
        val appliedAt: LocalDateTime
    ) {
        companion object {
            fun from(result: LoanApplicationResult): Response {
                return Response(
                    id = result.id,
                    customerId = result.customerId,
                    requestedAmount = result.requestedAmount,
                    requestedTerm = result.requestedTerm,
                    productType = result.productType.description,
                    status = result.status.description,
                    approvedAmount = result.approvedAmount,
                    approvedRate = result.approvedRate,
                    rejectionReason = result.rejectionReason,
                    loanId = result.loanId,
                    appliedAt = result.appliedAt
                )
            }
        }
    }

    data class ApproveRequest(
        val approvedAmount: BigDecimal,
        val approvedRate: BigDecimal
    )

    data class RejectRequest(
        val reason: String
    )
}
```

**체크리스트**:
- [ ] LoanDto.kt 생성
- [ ] LoanApplicationDto.kt 생성
- [ ] from() 메서드 구현

---

### 5.2 Controller 구현

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/controller/LoanController.kt`

```kotlin
package com.socoolheeya.bluebank.loan.controller

import com.socoolheeya.bluebank.loan.dto.LoanDto
import com.socoolheeya.bluebank.loan.service.LoanService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/loans")
class LoanController(
    private val loanService: LoanService
) {

    @GetMapping("/{loanId}")
    fun getLoan(@PathVariable loanId: Long): ResponseEntity<LoanDto.Response> {
        val loan = loanService.getLoan(loanId)
        return ResponseEntity.ok(loan)
    }

    @GetMapping("/customer/{customerId}")
    fun getLoansByCustomer(@PathVariable customerId: Long): ResponseEntity<List<LoanDto.Response>> {
        val loans = loanService.getLoansByCustomerId(customerId)
        return ResponseEntity.ok(loans)
    }

    @PostMapping("/{loanId}/execute")
    fun executeLoan(@PathVariable loanId: Long): ResponseEntity<LoanDto.Response> {
        val loan = loanService.executeLoan(loanId)
        return ResponseEntity.ok(loan)
    }

    @PostMapping("/{loanId}/repay")
    fun repayLoan(
        @PathVariable loanId: Long,
        @RequestBody request: LoanDto.RepayRequest
    ): ResponseEntity<LoanDto.Response> {
        val loan = loanService.repayLoan(loanId, request.amount)
        return ResponseEntity.ok(loan)
    }
}
```

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/controller/LoanApplicationController.kt`

```kotlin
@RestController
@RequestMapping("/api/loans/applications")
class LoanApplicationController(
    private val loanApplicationService: LoanApplicationService
) {

    @PostMapping
    fun applyForLoan(
        @RequestBody request: LoanApplicationDto.Request
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.applyForLoan(request)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/{applicationId}")
    fun getApplication(@PathVariable applicationId: Long): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.getApplication(applicationId)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/customer/{customerId}")
    fun getApplicationsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<LoanApplicationDto.Response>> {
        val applications = loanApplicationService.getApplicationsByCustomerId(customerId)
        return ResponseEntity.ok(applications)
    }

    @PostMapping("/{applicationId}/approve")
    fun approveApplication(
        @PathVariable applicationId: Long,
        @RequestBody request: LoanApplicationDto.ApproveRequest
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.approveApplication(
            applicationId,
            request.approvedAmount,
            request.approvedRate
        )
        return ResponseEntity.ok(application)
    }

    @PostMapping("/{applicationId}/reject")
    fun rejectApplication(
        @PathVariable applicationId: Long,
        @RequestBody request: LoanApplicationDto.RejectRequest
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.rejectApplication(applicationId, request.reason)
        return ResponseEntity.ok(application)
    }
}
```

**체크리스트**:
- [ ] LoanController.kt 생성
- [ ] LoanApplicationController.kt 생성
- [ ] REST API 엔드포인트 구현
- [ ] @RequestMapping 설정

---

## Phase 6: 설정 및 통합

### 6.1 Application 클래스

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/LoanApplication.kt`

```kotlin
package com.socoolheeya.bluebank.loan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = [
    "com.socoolheeya.bluebank"
])
@ConfigurationPropertiesScan(basePackages = [
    "com.socoolheeya.bluebank"
])
@EnableFeignClients(basePackages = [
    "com.socoolheeya.bluebank.account.adapter"
])
class LoanApplication

fun main(args: Array<String>) {
    runApplication<LoanApplication>(*args)
}
```

**체크리스트**:
- [ ] LoanApplication.kt 생성
- [ ] @EnableFeignClients 설정
- [ ] scanBasePackages 설정

---

### 6.2 application.yml

**파일**: `app/loan/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: loan

server:
  port: 8082

# Feign Client 설정
feign:
  account:
    url: http://localhost:8080
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: basic
      account-service:
        connectTimeout: 3000
        readTimeout: 5000

# Logging 설정
logging:
  level:
    com.socoolheeya.bluebank.loan: DEBUG
    com.socoolheeya.bluebank.account.adapter: DEBUG
```

**체크리스트**:
- [ ] application.yml 생성
- [ ] Feign Client 설정
- [ ] 포트 설정 (8082)

---

### 6.3 build.gradle.kts

**data/loan-data/build.gradle.kts**:
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // H2 for testing
    runtimeOnly("com.h2database:h2")
}
```

**app/loan/build.gradle.kts**:
```kotlin
dependencies {
    implementation(project(":data:loan-data"))
    implementation(project(":app:account"))  // AccountClient

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
```

**체크리스트**:
- [ ] data/loan-data build.gradle.kts 설정
- [ ] app/loan build.gradle.kts 설정
- [ ] 의존성 확인

---

## Phase 7: 테스트

### 7.1 Entity 테스트

**파일**: `data/loan-data/src/test/kotlin/com/socoolheeya/bluebank/loan/data/domain/entity/LoanTest.kt`

```kotlin
class LoanTest {

    @Test
    fun `대출 승인 테스트`() {
        val loan = createTestLoan()

        loan.approve("ADMIN")

        assertEquals(LoanStatus.APPROVED, loan.status)
        assertEquals("ADMIN", loan.approvedBy)
    }

    @Test
    fun `대출 실행 테스트`() {
        val loan = createTestLoan()
        loan.approve("ADMIN")

        loan.execute()

        assertEquals(LoanStatus.ACTIVE, loan.status)
    }

    @Test
    fun `대출 상환 테스트`() {
        val loan = createTestLoan()
        loan.approve("ADMIN")
        loan.execute()

        loan.repay(BigDecimal("1000000"))

        assertEquals(BigDecimal("29000000"), loan.outstandingBalance)
    }

    private fun createTestLoan(): Loan {
        return Loan(
            loanNumber = "LN001",
            customerId = 1L,
            accountId = 100L,
            loanType = LoanType.CREDIT,
            productType = ProductType.GENERAL_CREDIT,
            principalAmount = BigDecimal("30000000"),
            outstandingBalance = BigDecimal("30000000"),
            interestRate = BigDecimal("5.5"),
            rateType = RateType.VARIABLE,
            loanTerm = 36,
            startDate = LocalDate.now(),
            maturityDate = LocalDate.now().plusMonths(36),
            repaymentMethod = RepaymentMethod.EQUAL_INSTALLMENT,
            status = LoanStatus.PENDING
        )
    }
}
```

**체크리스트**:
- [ ] Entity 단위 테스트 작성
- [ ] 비즈니스 로직 메서드 테스트
- [ ] 예외 케이스 테스트

---

### 7.2 DataService 테스트

```kotlin
@SpringBootTest
@Transactional
class LoanDataServiceTest {

    @Autowired
    lateinit var loanDataService: LoanDataService

    @Test
    fun `대출 생성 테스트`() {
        val command = LoanCommand.Create(...)

        val result = loanDataService.createLoan(command)

        assertNotNull(result.id)
        assertEquals(command.loanNumber, result.loanNumber)
    }
}
```

**체크리스트**:
- [ ] DataService 통합 테스트 작성
- [ ] 트랜잭션 롤백 확인

---

### 7.3 Service 테스트

```kotlin
@SpringBootTest
class LoanApplicationServiceTest {

    @Autowired
    lateinit var loanApplicationService: LoanApplicationService

    @MockBean
    lateinit var accountClient: AccountClient

    @MockBean
    lateinit var creditScoreService: CreditScoreService

    @Test
    fun `대출 신청 테스트`() {
        // given
        `when`(creditScoreService.getCreditScore(any())).thenReturn(
            CreditScoreService.CreditScoreResult(750, "3등급", "NICE")
        )
        `when`(accountClient.validateAccount(any())).thenReturn(
            AccountValidationDto(100L, true, true, true, null)
        )

        val request = LoanApplicationDto.Request(...)

        // when
        val response = loanApplicationService.applyForLoan(request)

        // then
        assertNotNull(response.id)
        assertEquals(ApplicationStatus.SUBMITTED.description, response.status)
    }
}
```

**체크리스트**:
- [ ] Service 단위 테스트 작성
- [ ] Mock 객체 활용
- [ ] 비즈니스 로직 검증

---

### 7.4 Controller 테스트

```kotlin
@WebMvcTest(LoanController::class)
class LoanControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var loanService: LoanService

    @Test
    fun `대출 조회 API 테스트`() {
        val loanId = 1L
        val loanDto = LoanDto.Response(...)

        `when`(loanService.getLoan(loanId)).thenReturn(loanDto)

        mockMvc.perform(get("/api/loans/{loanId}", loanId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(loanId))
    }
}
```

**체크리스트**:
- [ ] Controller API 테스트 작성
- [ ] MockMvc 활용
- [ ] HTTP 응답 검증

---

## Phase 8: 고급 기능 구현

### 8.1 이자 계산 서비스

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/service/InterestCalculationService.kt`

```kotlin
@Service
class InterestCalculationService {

    /**
     * 원리금균등분할 월 상환액 계산
     */
    fun calculateEqualInstallment(
        principal: BigDecimal,
        annualRate: BigDecimal,
        termMonths: Int
    ): BigDecimal {
        val monthlyRate = annualRate.divide(BigDecimal("1200"), 10, RoundingMode.HALF_UP)

        val numerator = principal.multiply(monthlyRate)
        val denominator = BigDecimal.ONE.subtract(
            BigDecimal.ONE.add(monthlyRate).pow(-termMonths)
        )

        return numerator.divide(denominator, 0, RoundingMode.UP)
    }

    /**
     * 원금균등분할 월 상환액 계산
     */
    fun calculateEqualPrincipal(
        principal: BigDecimal,
        annualRate: BigDecimal,
        termMonths: Int,
        currentMonth: Int
    ): Pair<BigDecimal, BigDecimal> {
        val monthlyPrincipal = principal.divide(BigDecimal(termMonths), 0, RoundingMode.UP)
        val remainingPrincipal = principal.subtract(
            monthlyPrincipal.multiply(BigDecimal(currentMonth - 1))
        )
        val monthlyInterest = remainingPrincipal
            .multiply(annualRate)
            .divide(BigDecimal("1200"), 0, RoundingMode.UP)

        return Pair(monthlyPrincipal, monthlyInterest)
    }
}
```

**체크리스트**:
- [ ] InterestCalculationService.kt 생성
- [ ] 상환 방식별 계산 로직 구현
- [ ] 테스트 코드 작성

---

### 8.2 상환 스케줄 생성 서비스

```kotlin
@Service
class RepaymentScheduleService(
    private val repaymentDataService: RepaymentDataService,
    private val interestCalculationService: InterestCalculationService
) {

    fun generateRepaymentSchedule(loanId: Long, loan: LoanResult): List<RepaymentResult> {
        return when (loan.repaymentMethod) {
            RepaymentMethod.EQUAL_INSTALLMENT -> generateEqualInstallmentSchedule(loanId, loan)
            RepaymentMethod.EQUAL_PRINCIPAL -> generateEqualPrincipalSchedule(loanId, loan)
            RepaymentMethod.LUMP_SUM -> generateLumpSumSchedule(loanId, loan)
            else -> emptyList()
        }
    }

    private fun generateEqualInstallmentSchedule(loanId: Long, loan: LoanResult): List<RepaymentResult> {
        val results = mutableListOf<RepaymentResult>()
        val monthlyPayment = interestCalculationService.calculateEqualInstallment(
            loan.principalAmount,
            loan.interestRate,
            loan.loanTerm
        )

        var remainingBalance = loan.principalAmount

        for (month in 1..loan.loanTerm) {
            val scheduledDate = loan.startDate.plusMonths(month.toLong())
            val interestAmount = remainingBalance
                .multiply(loan.interestRate)
                .divide(BigDecimal("1200"), 2, RoundingMode.HALF_UP)
            val principalAmount = monthlyPayment.subtract(interestAmount)

            remainingBalance = remainingBalance.subtract(principalAmount)

            val result = repaymentDataService.createRepayment(
                loanId = loanId,
                repaymentType = if (month == loan.loanTerm) RepaymentType.FINAL else RepaymentType.SCHEDULED,
                principalAmount = principalAmount,
                interestAmount = interestAmount,
                scheduledDate = scheduledDate
            )

            results.add(result)
        }

        return results
    }

    // 다른 상환 방식 구현...
}
```

**체크리스트**:
- [ ] RepaymentScheduleService.kt 생성
- [ ] 상환 스케줄 생성 로직 구현
- [ ] 상환 방식별 로직 구현

---

### 8.3 배치 작업 (연체 처리)

```kotlin
@Component
class OverdueCheckBatchJob(
    private val repaymentDataService: RepaymentDataService,
    private val loanDataService: LoanDataService
) {

    @Scheduled(cron = "0 0 1 * * *")  // 매일 새벽 1시
    fun checkOverdueRepayments() {
        val today = LocalDate.now()

        // 오늘 이전에 예정되었으나 완료되지 않은 상환 조회
        val overdueRepayments = repaymentDataService.getScheduledRepayments(
            startDate = LocalDate.now().minusYears(1),
            endDate = today.minusDays(1)
        ).filter { it.status == RepaymentStatus.SCHEDULED }

        overdueRepayments.forEach { repayment ->
            // 연체 일수 계산
            val overdueDays = ChronoUnit.DAYS.between(repayment.scheduledDate, today).toInt()

            // 연체료 계산
            val penaltyRate = BigDecimal("0.03")  // 3% 연체료
            val penaltyAmount = repayment.totalAmount
                .multiply(penaltyRate)
                .multiply(BigDecimal(overdueDays))
                .divide(BigDecimal("365"), 2, RoundingMode.HALF_UP)

            // 상환 내역 연체 처리
            repaymentDataService.markOverdue(repayment.id!!, overdueDays, penaltyAmount)

            // 대출 상태 변경
            loanDataService.markAsOverdue(repayment.loanId)
        }

        println("연체 체크 완료: ${overdueRepayments.size}건")
    }
}
```

**체크리스트**:
- [ ] OverdueCheckBatchJob.kt 생성
- [ ] @Scheduled 배치 작업 구현
- [ ] 연체 처리 로직 구현

---

## Phase 9: 문서화 및 배포 준비

### 9.1 API 문서화

**체크리스트**:
- [ ] Swagger/OpenAPI 설정
- [ ] API 문서 작성
- [ ] 예제 Request/Response 추가

---

### 9.2 에러 처리

**파일**: `app/loan/src/main/kotlin/com/socoolheeya/bluebank/loan/exception/LoanExceptionHandler.kt`

```kotlin
@RestControllerAdvice
class LoanExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Not Found"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Bad Request"))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(e.message ?: "Conflict"))
    }

    data class ErrorResponse(
        val message: String,
        val timestamp: LocalDateTime = LocalDateTime.now()
    )
}
```

**체크리스트**:
- [ ] 전역 예외 처리 구현
- [ ] 커스텀 예외 클래스 생성
- [ ] 에러 응답 표준화

---

## 완료 체크리스트

### Phase 1: 기본 인프라
- [ ] 프로젝트 구조 생성
- [ ] Enum 정의
- [ ] Entity 클래스 6개 구현
- [ ] Repository 인터페이스 6개 생성

### Phase 2: Command/Result 패턴
- [ ] Command 객체 생성
- [ ] Result 객체 생성

### Phase 3: DataService
- [ ] LoanDataService 구현
- [ ] LoanApplicationDataService 구현
- [ ] RepaymentDataService 구현

### Phase 4: App Service
- [ ] LoanService 구현
- [ ] LoanApplicationService 구현
- [ ] CreditScoreService Mock 구현

### Phase 5: DTO 및 Controller
- [ ] DTO 클래스 생성
- [ ] Controller 구현

### Phase 6: 설정 및 통합
- [ ] Application 클래스 설정
- [ ] application.yml 설정
- [ ] build.gradle.kts 설정

### Phase 7: 테스트
- [ ] Entity 테스트
- [ ] DataService 테스트
- [ ] Service 테스트
- [ ] Controller 테스트

### Phase 8: 고급 기능
- [ ] 이자 계산 서비스
- [ ] 상환 스케줄 생성
- [ ] 배치 작업 (연체 처리)

### Phase 9: 문서화 및 배포
- [ ] API 문서화
- [ ] 에러 처리
- [ ] 로깅 설정

---

## 우선순위

1. **최우선 (MVP)**: Phase 1-5
2. **중요**: Phase 6-7
3. **선택**: Phase 8-9

---

## 참고사항

- Account Service의 구현 패턴을 따라야 함
- data 모듈은 app 모듈을 의존하지 않음
- 모든 트랜잭션은 DataService에서 처리
- app 모듈은 비즈니스 로직만 담당