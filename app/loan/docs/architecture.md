# Loan Service Architecture

## 1. 개요

Loan Service는 블루뱅크의 다양한 대출 상품을 지원하는 마이크로서비스입니다. 신용대출, 담보대출, 마이너스통장, 비상금대출 등 13개 대출 상품 유형을 지원하며, 대출 신청, 심사, 실행, 상환, 갈아타기(대환) 등의 전체 대출 라이프사이클을 관리합니다.

### 1.1 지원 대출 상품

#### 신용대출 (Credit Loan)
1. **비상금대출 (Emergency)**: 50만~300만원, 연 4.38~15%, 마이너스통장 형태
2. **마이너스통장대출 (Credit Line)**: 최대 2.4억원, 연 5.13~9.8%, 수시입출금
3. **신용대출 (General Credit)**: 최대 3억원, 연 4.485~7.395%
4. **중신용대출 (Mid Credit)**: 최대 1억원, 연 3.94~11.856%
5. **새희망홀씨II (New Hope)**: 최대 3,500만원, 연 5.14~10.5%
6. **같이대출 (Together)**: 최대 2억원, 연 4.453~6.761%

#### 담보대출 (Secured Loan)
7. **주택담보대출 (Mortgage)**: 최대 10억원, 연 4.334~6.262%
8. **HF 아낌e 보금자리론 (HF Mortgage)**: 최대 4.2억원, 고정금리 3.05~4.35%
9. **전월세보증금대출 (Lease)**: 최대 5억원, 연 3.42~6.5%
10. **자동차담보대출 (Auto)**: 최대 4,000만원, 연 4.057~8.063%
11. **리스금융대출 (Auto Lease)**: 리스 차량 담보

#### 대환대출 (Refinance)
12. **신용대출 갈아타기 (Credit Refinance)**: 최대 2개 통합
13. **주택담보대출 갈아타기 (Mortgage Refinance)**: 타행 대출 이전
14. **전월세보증금 갈아타기 (Lease Refinance)**: 전세대출 이전

---

## 2. 도메인 모델

### 2.1 핵심 엔티티

#### 2.1.1 Loan (대출)
대출의 핵심 정보를 담는 메인 엔티티

```kotlin
@Entity
class Loan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    var loanNumber: String,                    // 대출 번호 (고유 식별자)
    var customerId: Long,                      // 고객 ID
    var accountId: Long,                       // 연결된 계좌 ID

    // 대출 유형
    @Enumerated(EnumType.STRING)
    var loanType: LoanType,                    // 대출 유형 (신용/담보/대환)

    @Enumerated(EnumType.STRING)
    var productType: ProductType,              // 상품 유형 (비상금/마이너스/신용대출 등)

    // 대출 금액 및 금리
    var principalAmount: BigDecimal,           // 원금 (대출 금액)
    var outstandingBalance: BigDecimal,        // 잔여 원금
    var interestRate: BigDecimal,              // 연 금리 (%)
    var rateType: RateType,                    // 금리 유형 (변동/고정)

    // 대출 기간
    var loanTerm: Int,                         // 대출 기간 (개월)
    var startDate: LocalDate,                  // 대출 시작일
    var maturityDate: LocalDate,               // 만기일

    // 상환 정보
    @Enumerated(EnumType.STRING)
    var repaymentMethod: RepaymentMethod,      // 상환 방식
    var monthlyPayment: BigDecimal?,           // 월 상환액 (분할상환인 경우)

    // 대출 상태
    @Enumerated(EnumType.STRING)
    var status: LoanStatus,                    // 대출 상태

    // 담보 정보 (담보대출인 경우)
    var collateralId: Long? = null,            // 담보 ID
    var loanToValueRatio: BigDecimal? = null,  // LTV (담보인정비율)

    // 대환 정보
    var refinanceSourceLoanId: Long? = null,   // 대환 전 대출 ID
    var isRefinanced: Boolean = false,         // 대환 여부

    // 감면 및 우대
    var preferentialRate: BigDecimal = BigDecimal.ZERO,  // 우대 금리
    var discountReason: String? = null,                  // 감면 사유

    // 기타
    var creditScore: Int? = null,              // 신청 시 신용점수
    var approvedBy: String? = null,            // 승인자

    // 감사
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 비즈니스 로직
    fun approve(approver: String) {
        require(status == LoanStatus.PENDING) { "대출 심사 중인 상태만 승인 가능합니다" }
        this.status = LoanStatus.APPROVED
        this.approvedBy = approver
        this.updatedAt = LocalDateTime.now()
    }

    fun execute() {
        require(status == LoanStatus.APPROVED) { "승인된 대출만 실행 가능합니다" }
        this.status = LoanStatus.ACTIVE
        this.startDate = LocalDate.now()
        this.maturityDate = startDate.plusMonths(loanTerm.toLong())
        this.updatedAt = LocalDateTime.now()
    }

    fun repay(amount: BigDecimal) {
        require(status == LoanStatus.ACTIVE) { "실행 중인 대출만 상환 가능합니다" }
        require(amount <= outstandingBalance) { "상환 금액이 잔여 원금을 초과할 수 없습니다" }

        this.outstandingBalance = outstandingBalance.subtract(amount)
        this.updatedAt = LocalDateTime.now()

        if (outstandingBalance <= BigDecimal.ZERO) {
            this.status = LoanStatus.SETTLED
        }
    }

    fun reject(reason: String) {
        require(status == LoanStatus.PENDING) { "심사 중인 대출만 거절 가능합니다" }
        this.status = LoanStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }
}
```

#### 2.1.2 LoanApplication (대출 신청)
대출 신청 정보 및 심사 이력

```kotlin
@Entity
class LoanApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var customerId: Long,
    var requestedAmount: BigDecimal,           // 신청 금액
    var requestedTerm: Int,                    // 신청 기간 (개월)

    @Enumerated(EnumType.STRING)
    var productType: ProductType,

    @Enumerated(EnumType.STRING)
    var repaymentMethod: RepaymentMethod,

    // 고객 정보
    var annualIncome: BigDecimal,              // 연소득
    var employmentType: String,                // 고용 형태 (정규직/계약직/사업자)
    var employmentPeriodMonths: Int,           // 재직 기간 (개월)
    var companyName: String? = null,

    // 신용 정보
    var creditScore: Int,
    var existingLoanCount: Int = 0,            // 기존 대출 건수
    var totalExistingDebt: BigDecimal = BigDecimal.ZERO,  // 총 기존 부채
    var hasDelayHistory: Boolean = false,      // 연체 이력 여부

    // 담보 정보 (담보대출인 경우)
    var collateralType: String? = null,        // 담보 유형 (주택/자동차)
    var collateralValue: BigDecimal? = null,   // 담보 가치
    var collateralAddress: String? = null,

    // 심사 결과
    @Enumerated(EnumType.STRING)
    var status: ApplicationStatus = ApplicationStatus.SUBMITTED,

    var approvedAmount: BigDecimal? = null,    // 승인 금액
    var approvedRate: BigDecimal? = null,      // 승인 금리
    var rejectionReason: String? = null,

    var loanId: Long? = null,                  // 생성된 대출 ID

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
}
```

#### 2.1.3 Repayment (상환)
대출 상환 내역

```kotlin
@Entity
class Repayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var loanId: Long,

    @Enumerated(EnumType.STRING)
    var repaymentType: RepaymentType,          // 상환 유형 (정기/중도/만기)

    var principalAmount: BigDecimal,           // 원금 상환액
    var interestAmount: BigDecimal,            // 이자 상환액
    var totalAmount: BigDecimal,               // 총 상환액

    var balanceAfter: BigDecimal,              // 상환 후 잔액

    var scheduledDate: LocalDate,              // 예정일
    var actualDate: LocalDate? = null,         // 실제 상환일

    @Enumerated(EnumType.STRING)
    var status: RepaymentStatus = RepaymentStatus.SCHEDULED,

    var isOverdue: Boolean = false,            // 연체 여부
    var overduedays: Int = 0,                  // 연체 일수
    var penaltyAmount: BigDecimal = BigDecimal.ZERO,  // 연체료

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun process() {
        require(status == RepaymentStatus.SCHEDULED) { "예정된 상환만 처리 가능합니다" }
        this.status = RepaymentStatus.COMPLETED
        this.actualDate = LocalDate.now()

        // 연체 체크
        if (LocalDate.now().isAfter(scheduledDate)) {
            this.isOverdue = true
            this.overduedays = ChronoUnit.DAYS.between(scheduledDate, LocalDate.now()).toInt()
        }
    }
}
```

#### 2.1.4 Collateral (담보)
담보대출의 담보 정보

```kotlin
@Entity
class Collateral(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var loanId: Long,

    @Enumerated(EnumType.STRING)
    var collateralType: CollateralType,        // 담보 유형

    // 부동산 담보
    var address: String? = null,
    var area: BigDecimal? = null,              // 면적 (㎡)
    var buildingType: String? = null,          // 건물 유형 (아파트/연립/단독)
    var completionYear: Int? = null,           // 준공 연도

    // 자동차 담보
    var vehicleModel: String? = null,
    var vehicleYear: Int? = null,
    var vehicleNumber: String? = null,

    // 평가 정보
    var appraisedValue: BigDecimal,            // 감정 가액
    var appraisalDate: LocalDate,              // 감정일
    var appraisalInstitution: String,          // 감정 기관

    // 등기 정보
    var registrationNumber: String? = null,    // 등기 번호
    var registrationDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    var status: CollateralStatus = CollateralStatus.REGISTERED,

    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### 2.1.5 InterestPayment (이자 납부)
이자 납부 내역

```kotlin
@Entity
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var loanId: Long,

    var interestAmount: BigDecimal,            // 이자 금액
    var interestRate: BigDecimal,              // 적용 이율

    var calculationPeriodStart: LocalDate,     // 계산 기간 시작
    var calculationPeriodEnd: LocalDate,       // 계산 기간 종료

    var principalBalance: BigDecimal,          // 원금 잔액

    var dueDate: LocalDate,                    // 납부 예정일
    var paidDate: LocalDate? = null,           // 납부일

    @Enumerated(EnumType.STRING)
    var status: InterestPaymentStatus = InterestPaymentStatus.PENDING,

    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### 2.1.6 CreditScoreHistory (신용점수 이력)
대출 신청 시점의 신용점수 기록

```kotlin
@Entity
class CreditScoreHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var customerId: Long,
    var loanApplicationId: Long? = null,

    var creditScore: Int,                      // 신용점수 (0-1000)
    var creditGrade: String,                   // 신용등급 (1-10등급)

    var scoringAgency: String,                 // 평가기관 (KCB/NICE)
    var scoredAt: LocalDateTime,               // 평가 시점

    // 세부 점수
    var paymentHistory: Int? = null,           // 상환이력 점수
    var creditUsage: Int? = null,              // 신용사용 점수
    var debtAmount: BigDecimal? = null,        // 총 부채액

    var createdAt: LocalDateTime = LocalDateTime.now()
)
```

---

### 2.2 Enum 정의

```kotlin
enum class LoanType {
    CREDIT,        // 신용대출
    SECURED,       // 담보대출
    REFINANCE      // 대환대출
}

enum class ProductType {
    // 신용대출
    EMERGENCY,              // 비상금대출
    CREDIT_LINE,            // 마이너스통장
    GENERAL_CREDIT,         // 신용대출
    MID_CREDIT,             // 중신용대출
    NEW_HOPE,               // 새희망홀씨II
    TOGETHER,               // 같이대출

    // 담보대출
    MORTGAGE,               // 주택담보대출
    HF_MORTGAGE,            // HF 아낌e 보금자리론
    LEASE,                  // 전월세보증금대출
    AUTO_LOAN,              // 자동차담보대출
    AUTO_LEASE,             // 리스금융대출

    // 대환대출
    CREDIT_REFINANCE,       // 신용대출 갈아타기
    MORTGAGE_REFINANCE,     // 주택담보대출 갈아타기
    LEASE_REFINANCE         // 전월세보증금 갈아타기
}

enum class LoanStatus {
    PENDING,        // 심사 중
    APPROVED,       // 승인 (실행 전)
    ACTIVE,         // 실행 중
    OVERDUE,        // 연체
    SETTLED,        // 완료 (전액 상환)
    REJECTED,       // 거절
    CANCELLED       // 취소
}

enum class ApplicationStatus {
    SUBMITTED,      // 신청 완료
    UNDER_REVIEW,   // 심사 중
    APPROVED,       // 승인
    REJECTED        // 거절
}

enum class RepaymentMethod {
    LUMP_SUM,                // 만기일시상환
    EQUAL_PRINCIPAL,         // 원금균등분할
    EQUAL_INSTALLMENT,       // 원리금균등분할
    BALLOON                  // 체증식분할상환
}

enum class RepaymentType {
    SCHEDULED,      // 정기 상환
    EARLY,          // 중도 상환
    FINAL           // 만기 상환
}

enum class RepaymentStatus {
    SCHEDULED,      // 예정
    COMPLETED,      // 완료
    OVERDUE,        // 연체
    WAIVED          // 면제
}

enum class RateType {
    FIXED,          // 고정금리
    VARIABLE        // 변동금리
}

enum class CollateralType {
    REAL_ESTATE,    // 부동산
    VEHICLE,        // 차량
    DEPOSIT         // 보증금
}

enum class CollateralStatus {
    REGISTERED,     // 등록 완료
    RELEASED,       // 해제
    FORECLOSED      // 압류
}

enum class InterestPaymentStatus {
    PENDING,        // 미납
    PAID,           // 납부 완료
    OVERDUE         // 연체
}
```

---

## 3. 서비스 레이어 설계

### 3.1 DataService (data 모듈)

#### LoanDataService
트랜잭션 처리를 담당하는 데이터 레이어

```kotlin
@Service
class LoanDataService(
    private val loanRepository: LoanRepository,
    private val loanApplicationRepository: LoanApplicationRepository,
    private val repaymentRepository: RepaymentRepository,
    private val collateralRepository: CollateralRepository
) {

    @Transactional
    fun createLoan(command: LoanCommand.Create): LoanResult {
        val loan = command.toEntity()
        val savedLoan = loanRepository.save(loan)
        return LoanResult.from(savedLoan)
    }

    @Transactional
    fun approveLoan(loanId: Long, approver: String): LoanResult {
        val loan = loanRepository.findById(loanId).orElseThrow()
        loan.approve(approver)
        return LoanResult.from(loanRepository.save(loan))
    }

    @Transactional
    fun executeLoan(loanId: Long, accountId: Long): LoanResult {
        val loan = loanRepository.findById(loanId).orElseThrow()
        loan.execute()

        // 계좌에 대출금 입금 처리 (Account Service 호출)

        return LoanResult.from(loanRepository.save(loan))
    }

    @Transactional
    fun repayLoan(loanId: Long, amount: BigDecimal): RepaymentResult {
        val loan = loanRepository.findById(loanId).orElseThrow()
        loan.repay(amount)
        loanRepository.save(loan)

        // 상환 내역 기록
        val repayment = Repayment(
            loanId = loanId,
            repaymentType = RepaymentType.EARLY,
            principalAmount = amount,
            interestAmount = BigDecimal.ZERO, // 계산 필요
            totalAmount = amount,
            balanceAfter = loan.outstandingBalance,
            scheduledDate = LocalDate.now()
        )
        repayment.process()
        val savedRepayment = repaymentRepository.save(repayment)

        return RepaymentResult.from(savedRepayment)
    }

    @Transactional(readOnly = true)
    fun getLoan(loanId: Long): LoanResult? {
        return loanRepository.findById(loanId)
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

#### LoanApplicationDataService
대출 신청 관련 트랜잭션

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
        approvedRate: BigDecimal
    ): LoanApplicationResult {
        val application = loanApplicationRepository.findById(applicationId).orElseThrow()

        // 대출 생성
        val loan = Loan(
            loanNumber = generateLoanNumber(),
            customerId = application.customerId,
            accountId = 0L, // 계좌 연결은 실행 단계에서
            loanType = determineLoanType(application.productType),
            productType = application.productType,
            principalAmount = approvedAmount,
            outstandingBalance = approvedAmount,
            interestRate = approvedRate,
            rateType = RateType.VARIABLE,
            loanTerm = application.requestedTerm,
            startDate = LocalDate.now(),
            maturityDate = LocalDate.now().plusMonths(application.requestedTerm.toLong()),
            repaymentMethod = application.repaymentMethod,
            status = LoanStatus.APPROVED,
            creditScore = application.creditScore
        )
        val savedLoan = loanRepository.save(loan)

        application.approve(approvedAmount, approvedRate, savedLoan.id!!)
        val updatedApplication = loanApplicationRepository.save(application)

        return LoanApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun rejectApplication(applicationId: Long, reason: String): LoanApplicationResult {
        val application = loanApplicationRepository.findById(applicationId).orElseThrow()
        application.reject(reason)
        val updatedApplication = loanApplicationRepository.save(application)
        return LoanApplicationResult.from(updatedApplication)
    }
}
```

### 3.2 Service (app 모듈)

#### LoanService
비즈니스 로직 처리

```kotlin
@Service
class LoanService(
    private val loanDataService: LoanDataService,
    private val accountClient: AccountClient,  // Feign Client
    private val creditScoreService: CreditScoreService
) {

    fun applyForLoan(request: LoanApplicationRequest): LoanApplicationResponse {
        // 1. 신용점수 조회
        val creditScore = creditScoreService.getCreditScore(request.customerId)

        // 2. 계좌 검증
        val account = accountClient.validateAccount(request.accountId)
        require(account.isValid) { "유효하지 않은 계좌입니다" }

        // 3. 대출 가능 여부 사전 검증
        validateLoanEligibility(request, creditScore)

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
            creditScore = creditScore.score
        )

        val result = loanApplicationDataService.submitApplication(command)

        return LoanApplicationResponse.from(result)
    }

    fun executeLoan(loanId: Long): LoanResponse {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다")

        require(loan.status == LoanStatus.APPROVED) { "승인된 대출만 실행 가능합니다" }

        // 2. 계좌에 대출금 입금
        accountClient.deposit(loan.accountId, loan.principalAmount)

        // 3. 대출 실행 처리
        val result = loanDataService.executeLoan(loanId, loan.accountId)

        return LoanResponse.from(result)
    }

    fun repayLoan(loanId: Long, amount: BigDecimal): RepaymentResponse {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다")

        // 2. 계좌에서 출금
        accountClient.withdraw(loan.accountId, amount)

        // 3. 대출 상환 처리
        val result = loanDataService.repayLoan(loanId, amount)

        return RepaymentResponse.from(result)
    }

    private fun validateLoanEligibility(
        request: LoanApplicationRequest,
        creditScore: CreditScoreResult
    ) {
        // 신용점수 기준 검증
        val minScore = getMinimumCreditScore(request.productType)
        require(creditScore.score >= minScore) {
            "최소 신용점수 미달: 필요 ${minScore}점, 현재 ${creditScore.score}점"
        }

        // 소득 기준 검증
        val minIncome = getMinimumIncome(request.productType)
        require(request.annualIncome >= minIncome) {
            "최소 연소득 미달: 필요 ${minIncome}원"
        }
    }
}
```

---

## 4. API 설계

### 4.1 대출 신청 API

#### POST /api/loans/applications
대출 신청

**Request:**
```json
{
  "customerId": 1,
  "accountId": 100,
  "productType": "GENERAL_CREDIT",
  "amount": 30000000,
  "term": 36,
  "repaymentMethod": "EQUAL_INSTALLMENT",
  "annualIncome": 50000000,
  "employmentType": "정규직",
  "employmentPeriodMonths": 24,
  "companyName": "카카오"
}
```

**Response:**
```json
{
  "applicationId": 1001,
  "status": "SUBMITTED",
  "requestedAmount": 30000000,
  "expectedRate": 5.5,
  "message": "신청이 완료되었습니다. 심사는 최대 1영업일 소요됩니다.",
  "appliedAt": "2026-02-08T10:30:00"
}
```

#### GET /api/loans/applications/{applicationId}
신청 내역 조회

#### GET /api/loans/applications/customer/{customerId}
고객의 신청 내역 목록

### 4.2 대출 관리 API

#### GET /api/loans/{loanId}
대출 상세 조회

#### GET /api/loans/customer/{customerId}
고객의 대출 목록

#### POST /api/loans/{loanId}/execute
대출 실행 (승인 후)

#### POST /api/loans/{loanId}/repay
대출 상환

**Request:**
```json
{
  "amount": 5000000,
  "repaymentType": "EARLY"
}
```

### 4.3 상환 스케줄 API

#### GET /api/loans/{loanId}/repayments
상환 내역 조회

#### GET /api/loans/{loanId}/repayments/schedule
상환 스케줄 조회

---

## 5. 비즈니스 로직

### 5.1 대출 심사 로직

#### 신용점수 기준
| 상품 | 최소 신용점수 |
|------|--------------|
| 비상금대출 | 600점 |
| 마이너스통장 | 700점 |
| 신용대출 | 700점 |
| 중신용대출 | 500점 |
| HF 보금자리론 | 271점 |

#### DSR (총부채원리금상환비율) 계산
```kotlin
fun calculateDSR(
    annualIncome: BigDecimal,
    existingDebtPayment: BigDecimal,
    newLoanPayment: BigDecimal
): BigDecimal {
    val totalPayment = existingDebtPayment + newLoanPayment
    return totalPayment.divide(annualIncome, 4, RoundingMode.HALF_UP)
           .multiply(BigDecimal(100))
}

// DSR 한도: 일반적으로 40%
```

### 5.2 금리 결정 로직

```kotlin
fun determineInterestRate(
    creditScore: Int,
    loanAmount: BigDecimal,
    loanTerm: Int,
    productType: ProductType
): BigDecimal {
    val baseRate = getBaseRate()  // 기준금리 (COFIX, 금융채 등)

    // 신용점수에 따른 가산금리
    val creditSpread = when {
        creditScore >= 900 -> BigDecimal("0.5")
        creditScore >= 800 -> BigDecimal("1.0")
        creditScore >= 700 -> BigDecimal("2.0")
        creditScore >= 600 -> BigDecimal("3.5")
        else -> BigDecimal("5.0")
    }

    // 대출금액에 따른 가산금리
    val amountSpread = when {
        loanAmount >= BigDecimal("100000000") -> BigDecimal("-0.2")  // 우대
        loanAmount >= BigDecimal("50000000") -> BigDecimal("0.0")
        else -> BigDecimal("0.3")
    }

    // 우대금리 적용
    val discount = calculatePreferentialRate(productType)

    return baseRate + creditSpread + amountSpread - discount
}
```

### 5.3 월 상환액 계산

#### 원리금균등분할
```kotlin
fun calculateEqualInstallment(
    principal: BigDecimal,
    annualRate: BigDecimal,
    termMonths: Int
): BigDecimal {
    val monthlyRate = annualRate.divide(BigDecimal(1200), 10, RoundingMode.HALF_UP)

    val numerator = principal.multiply(monthlyRate)
    val denominator = BigDecimal.ONE.subtract(
        (BigDecimal.ONE.add(monthlyRate))
            .pow(-termMonths)
    )

    return numerator.divide(denominator, 0, RoundingMode.UP)
}
```

#### 원금균등분할
```kotlin
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
        .divide(BigDecimal(1200), 0, RoundingMode.UP)

    return Pair(monthlyPrincipal, monthlyInterest)
}
```

---

## 6. 데이터 흐름

### 6.1 대출 신청 → 실행 흐름

```
[Customer]
    ↓ 신청
[LoanService.applyForLoan]
    ↓ 신용점수 조회
[CreditScoreService]
    ↓ 계좌 검증
[AccountClient (Feign)]
    ↓ 신청서 생성
[LoanApplicationDataService]
    ↓ 저장
[LoanApplicationRepository]
    ↓ 심사 (자동/수동)
[심사 시스템]
    ↓ 승인
[LoanApplicationDataService.approve]
    ↓ Loan 생성
[LoanRepository]
    ↓ 대출 실행
[LoanService.executeLoan]
    ↓ 계좌 입금
[AccountClient.deposit]
    ↓ 완료
[Loan.status = ACTIVE]
```

### 6.2 대출 상환 흐름

```
[Customer]
    ↓ 상환 요청
[LoanService.repayLoan]
    ↓ 대출 조회
[LoanDataService.getLoan]
    ↓ 계좌 출금
[AccountClient.withdraw]
    ↓ 상환 처리
[LoanDataService.repayLoan]
    ↓ Repayment 생성
[RepaymentRepository]
    ↓ Loan 잔액 차감
[Loan.repay()]
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

    @PostMapping("/internal/accounts/{accountId}/deposit")
    fun deposit(@PathVariable accountId: Long, @RequestBody amount: BigDecimal)

    @PostMapping("/internal/accounts/{accountId}/withdraw")
    fun withdraw(@PathVariable accountId: Long, @RequestBody amount: BigDecimal)
}
```

### 7.2 신용평가 기관 연동
NICE, KCB 등 신용평가기관 API 호출

### 7.3 담보 평가 시스템 연동
KB시세, 한국감정원 등을 통한 부동산 시세 조회

---

## 8. 예외 처리

### 8.1 대출 신청 단계 예외
- `InsufficientCreditScoreException`: 신용점수 미달
- `ExcessiveDebtRatioException`: DSR 초과
- `InvalidEmploymentException`: 재직 기간 미달
- `DuplicateApplicationException`: 중복 신청

### 8.2 대출 실행 단계 예외
- `AccountNotFoundException`: 계좌 없음
- `InsufficientAccountBalanceException`: 계좌 잔액 부족 (상환 시)
- `LoanNotApprovedException`: 미승인 대출 실행 시도

### 8.3 상환 단계 예외
- `ExcessiveRepaymentException`: 잔액 초과 상환
- `OverdueRepaymentException`: 연체 상환

---

## 9. 성능 최적화

### 9.1 인덱스 설계
```sql
-- Loan 테이블
CREATE INDEX idx_loan_customer ON loan(customer_id);
CREATE INDEX idx_loan_status ON loan(status);
CREATE INDEX idx_loan_number ON loan(loan_number);

-- LoanApplication 테이블
CREATE INDEX idx_application_customer ON loan_application(customer_id);
CREATE INDEX idx_application_status ON loan_application(status);

-- Repayment 테이블
CREATE INDEX idx_repayment_loan ON repayment(loan_id);
CREATE INDEX idx_repayment_scheduled_date ON repayment(scheduled_date);
```

### 9.2 캐싱 전략
- 신용점수: 1시간 캐싱
- 기준금리: 1일 캐싱
- 대출 상품 정보: 영구 캐싱 (수동 무효화)

---

## 10. 보안

### 10.1 데이터 암호화
- 주민등록번호: AES-256 암호화
- 계좌번호: 부분 마스킹 (123-****-****-56)
- 주소: 상세주소 마스킹

### 10.2 접근 제어
- 본인 대출 정보만 조회 가능
- 관리자 권한: 전체 대출 조회 및 승인/거절
- 심사자 권한: 신청서 조회 및 심사 처리

---

## 11. 모니터링

### 11.1 주요 지표
- 대출 신청 건수 (일/주/월)
- 승인율
- 평균 대출 금액
- 연체율
- 중도상환율

### 11.2 알림
- 연체 발생 시 즉시 알림
- 대출 승인/거절 알림
- 상환일 3일 전 리마인더

---

## 12. 참고 자료

- 블루뱅크 대출 상품: https://www.kakaobank.com/products
- DSR 규제: 금융감독원 고시
- 신용정보법: 개인신용정보 보호
