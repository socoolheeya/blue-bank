# Account Service 구현 작업 목록

## 문서 개요

이 문서는 architecture.md에 정의된 계좌 시스템을 실제로 구현하기 위한 상세 작업 목록입니다.
작업은 우선순위에 따라 Phase로 구분되어 있으며, 각 작업의 예상 소요 시간과 의존성을 명시합니다.

**기준 날짜**: 2026-02-08
**전체 예상 기간**: 8-10주
**개발자**: 1-2명 기준

---

## Phase 1: 기본 인프라 구축 (1주)

### 1.1 Entity 클래스 수정 및 추가
**우선순위**: 🔴 High
**예상 시간**: 2일

#### 작업 내용

**기존 Entity 수정**

- [ ] **Account.kt** 수정
  - [ ] `productType: ProductType` 필드 추가
  - [ ] `interestRate: BigDecimal` 필드 추가 (기존 adaptedRate 대체)
  - [ ] `parentAccountId: Long?` 필드 추가 (우리아이통장용)
  - [ ] `linkedAccountId: Long?` 필드 추가 (세이프박스, 기록통장용)
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue(strategy = GenerationType.IDENTITY)` 추가
  - [ ] 비즈니스 메서드 추가: `updateName(name: String)`, `close()`, `activate()`

- [ ] **Balance.kt** 수정
  - [ ] `interestAccumulated: BigDecimal` 필드 추가
  - [ ] `version: Long` 필드 추가 with `@Version` (낙관적 락)
  - [ ] 비즈니스 메서드 추가: `deposit(amount: BigDecimal)`, `withdraw(amount: BigDecimal)`, `addHold(amount: BigDecimal)`, `releaseHold(amount: BigDecimal)`
  - [ ] `calculateAvailableBalance()` 메서드 추가

- [ ] **AccountHolder.kt** 수정
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue` 추가
  - [ ] `relationshipType: String?` 필드 추가
  - [ ] `joinedAt: LocalDateTime` 필드 추가

- [ ] **AccountLimit.kt** 수정
  - [ ] `monthlyDepositLimit: BigDecimal?` 필드 추가
  - [ ] `updatedAt: LocalDateTime` 필드 추가

- [ ] **Hold.kt** 수정
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue` 추가
  - [ ] `createdAt: LocalDateTime` 필드 추가 (기존 누락)
  - [ ] `releasedAt: LocalDateTime?` 필드 추가 (기존 누락)
  - [ ] 비즈니스 메서드 추가: `release()`, `convert()`, `isExpired()`

- [ ] **LedgerEntry.kt** 수정
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue` 추가
  - [ ] `description: String` 필드 추가 (기존 누락)
  - [ ] `memo: String?` 필드 추가 (우리아이통장, 기록통장용)
  - [ ] `sectionId: Long?` 필드 추가 (기록통장용)
  - [ ] `transactionId: String?` 필드 추가 (기존 누락)
  - [ ] 필드명 통일: `type` → `entryType`, `balanceAfter` → `balance`, `occurredAt` → `createdAt`

- [ ] **AccountStatusHistory.kt** 수정
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue` 추가
  - [ ] `changedBy: String` 필드 추가 (기존 누락)
  - [ ] 필드명 통일: `fromStatus` → `previousStatus`, `toStatus` → `newStatus`

- [ ] **LimitUsage.kt** 수정
  - [ ] `id` 필드를 nullable로 수정 (`Long?`)
  - [ ] `@GeneratedValue` 추가

**신규 Entity 생성**

- [ ] **AccountSection.kt** 생성 (기록통장용)
  ```kotlin
  @Entity
  class AccountSection(
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      var id: Long? = null,
      var accountId: Long,
      var name: String,
      var description: String? = null,
      var coverImageUrl: String? = null,
      var targetAmount: BigDecimal? = null,
      var currentAmount: BigDecimal = BigDecimal.ZERO,
      var displayOrder: Int = 0,
      var createdAt: LocalDateTime = LocalDateTime.now()
  )
  ```

- [ ] **SavingRule.kt** 생성 (기록통장용)
  ```kotlin
  @Entity
  class SavingRule(
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      var id: Long? = null,
      var sectionId: Long,
      var name: String,
      var amount: BigDecimal,
      @Enumerated(EnumType.STRING)
      var frequency: RuleFrequency,
      var dayOfWeek: Int? = null,
      var dayOfMonth: Int? = null,
      var isActive: Boolean = true,
      var createdAt: LocalDateTime = LocalDateTime.now()
  )
  ```

- [ ] **GroupMeeting.kt** 생성 (모임통장용)
  ```kotlin
  @Entity
  class GroupMeeting(
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      var id: Long? = null,
      var accountId: Long,
      var name: String,
      var description: String? = null,
      var monthlyFee: BigDecimal? = null,
      var maxMembers: Int = 100,
      var currentMembers: Int = 1,
      var coverImageUrl: String? = null,
      var createdAt: LocalDateTime = LocalDateTime.now()
  )
  ```

- [ ] **InterestPayment.kt** 생성
  ```kotlin
  @Entity
  class InterestPayment(
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      var id: Long? = null,
      var accountId: Long,
      var amount: BigDecimal,
      var interestRate: BigDecimal,
      var calculationPeriodStart: LocalDate,
      var calculationPeriodEnd: LocalDate,
      var paidAt: LocalDateTime = LocalDateTime.now()
  )
  ```

**Enum 수정 및 추가**

- [ ] **AccountEnums.kt** 수정
  - [ ] `ProductType` enum 추가
    ```kotlin
    enum class ProductType {
        BASIC_CHECKING,
        GROUP_MEETING,
        CHILD_ACCOUNT,
        RECORD_BOOK,
        SAFEBOX
    }
    ```
  - [ ] `RuleFrequency` enum 추가
    ```kotlin
    enum class RuleFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
    ```
  - [ ] `HolderRole` enum에 추가
    ```kotlin
    PARENT,
    CHILD,
    MEETING_OWNER,
    MEETING_MEMBER
    ```
  - [ ] `AccountType`에 `SAFEBOX` 추가

---

### 1.2 Repository 인터페이스 생성
**우선순위**: 🔴 High
**예상 시간**: 1일

#### 작업 내용

- [ ] **BalanceRepository.kt** 생성
  ```kotlin
  @Repository
  interface BalanceRepository : JpaRepository<Balance, Long> {
      fun findByAccountId(accountId: Long): Balance?

      @Lock(LockModeType.PESSIMISTIC_WRITE)
      @Query("SELECT b FROM Balance b WHERE b.accountId = :accountId")
      fun findByAccountIdWithLock(accountId: Long): Balance?
  }
  ```

- [ ] **AccountHolderRepository.kt** 생성
  ```kotlin
  @Repository
  interface AccountHolderRepository : JpaRepository<AccountHolder, Long> {
      fun findByAccountId(accountId: Long): List<AccountHolder>
      fun findByCustomerId(customerId: Long): List<AccountHolder>
      fun findByAccountIdAndRole(accountId: Long, role: HolderRole): List<AccountHolder>
  }
  ```

- [ ] **AccountLimitRepository.kt** 생성
  ```kotlin
  @Repository
  interface AccountLimitRepository : JpaRepository<AccountLimit, Long> {
      fun findByAccountId(accountId: Long): AccountLimit?
  }
  ```

- [ ] **HoldRepository.kt** 생성
  ```kotlin
  @Repository
  interface HoldRepository : JpaRepository<Hold, Long> {
      fun findByAccountIdAndStatus(accountId: Long, status: HoldStatus): List<Hold>
      fun findByAccountId(accountId: Long): List<Hold>

      @Query("SELECT SUM(h.amount) FROM Hold h WHERE h.accountId = :accountId AND h.status = 'ACTIVE'")
      fun sumActiveHoldsByAccountId(accountId: Long): BigDecimal?
  }
  ```

- [ ] **LedgerEntryRepository.kt** 생성
  ```kotlin
  @Repository
  interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {
      fun findByAccountIdOrderByOccurredAtDesc(accountId: Long, pageable: Pageable): Page<LedgerEntry>
      fun findByAccountIdAndSectionId(accountId: Long, sectionId: Long, pageable: Pageable): Page<LedgerEntry>

      @Query("SELECT l FROM LedgerEntry l WHERE l.accountId = :accountId AND l.occurredAt BETWEEN :start AND :end")
      fun findByAccountIdAndPeriod(accountId: Long, start: LocalDateTime, end: LocalDateTime): List<LedgerEntry>
  }
  ```

- [ ] **AccountStatusHistoryRepository.kt** 생성
  ```kotlin
  @Repository
  interface AccountStatusHistoryRepository : JpaRepository<AccountStatusHistory, Long> {
      fun findByAccountIdOrderByChangedAtDesc(accountId: Long): List<AccountStatusHistory>
  }
  ```

- [ ] **LimitUsageRepository.kt** 생성
  ```kotlin
  @Repository
  interface LimitUsageRepository : JpaRepository<LimitUsage, Long> {
      fun findByAccountIdAndUsageDate(accountId: Long, usageDate: LocalDate): LimitUsage?

      @Query("SELECT SUM(l.dailyUsage) FROM LimitUsage l WHERE l.accountId = :accountId AND l.usageDate = :date")
      fun sumDailyUsageByAccountIdAndDate(accountId: Long, date: LocalDate): BigDecimal?
  }
  ```

- [ ] **AccountSectionRepository.kt** 생성
  ```kotlin
  @Repository
  interface AccountSectionRepository : JpaRepository<AccountSection, Long> {
      fun findByAccountIdOrderByDisplayOrder(accountId: Long): List<AccountSection>
      fun countByAccountId(accountId: Long): Long
  }
  ```

- [ ] **SavingRuleRepository.kt** 생성
  ```kotlin
  @Repository
  interface SavingRuleRepository : JpaRepository<SavingRule, Long> {
      fun findBySectionId(sectionId: Long): List<SavingRule>
      fun findBySectionIdAndIsActive(sectionId: Long, isActive: Boolean): List<SavingRule>
  }
  ```

- [ ] **GroupMeetingRepository.kt** 생성
  ```kotlin
  @Repository
  interface GroupMeetingRepository : JpaRepository<GroupMeeting, Long> {
      fun findByAccountId(accountId: Long): GroupMeeting?
  }
  ```

- [ ] **InterestPaymentRepository.kt** 생성
  ```kotlin
  @Repository
  interface InterestPaymentRepository : JpaRepository<InterestPayment, Long> {
      fun findByAccountIdOrderByPaidAtDesc(accountId: Long): List<InterestPayment>

      @Query("SELECT i FROM InterestPayment i WHERE i.accountId = :accountId AND i.paidAt BETWEEN :start AND :end")
      fun findByAccountIdAndPeriod(accountId: Long, start: LocalDateTime, end: LocalDateTime): List<InterestPayment>
  }
  ```

- [ ] **AccountRepository.kt** 수정
  - [ ] 추가 쿼리 메서드 정의
    ```kotlin
    fun findByStatus(status: AccountStatus): List<Account>
    fun findByProductType(productType: ProductType): List<Account>
    fun findByParentAccountId(parentAccountId: Long): List<Account>
    fun findByLinkedAccountId(linkedAccountId: Long): List<Account>

    @Query("SELECT a FROM Account a JOIN AccountHolder ah ON a.id = ah.accountId WHERE ah.customerId = :customerId")
    fun findByCustomerId(customerId: Long): List<Account>
    ```

---

### 1.3 테스트 환경 설정
**우선순위**: 🟡 Medium
**예상 시간**: 0.5일

#### 작업 내용

- [ ] **application-test.yml** 생성
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:testdb;MODE=PostgreSQL
      driver-class-name: org.h2.Driver
      username: sa
      password:

    jpa:
      hibernate:
        ddl-auto: create-drop
      show-sql: true
      properties:
        hibernate:
          format_sql: true
          dialect: org.hibernate.dialect.H2Dialect

    main:
      allow-bean-definition-overriding: true

  logging:
    level:
      org.hibernate.SQL: DEBUG
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  ```

- [ ] 통합 테스트 기반 클래스 생성
  ```kotlin
  @SpringBootTest
  @ActiveProfiles("test")
  @Transactional
  abstract class IntegrationTestBase {
      @Autowired
      protected lateinit var entityManager: EntityManager

      @BeforeEach
      fun setup() {
          entityManager.clear()
      }
  }
  ```

---

## Phase 2: 핵심 비즈니스 로직 구현 (2-3주)

### 2.1 계좌 개설 로직 구현
**우선순위**: 🔴 High
**예상 시간**: 3일

#### 작업 내용

**Command 객체 업데이트**

- [ ] **AccountCommand.kt** 수정
  ```kotlin
  sealed interface AccountCommand {
      data class Create(
          val accountNumber: String,
          val name: String?,
          val accountType: AccountType,
          val productType: ProductType,
          val interestRate: BigDecimal,
          val customerId: Long,
          val parentAccountId: Long? = null,
          val linkedAccountId: Long? = null
      ) : AccountCommand

      // 추가 커맨드들...
  }
  ```

**Result 객체 업데이트**

- [ ] **AccountResult.kt** 수정
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
      val balance: BalanceInfo? = null,
      val sections: List<SectionInfo>? = null
  ) {
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

      companion object {
          fun from(account: Account, balance: Balance? = null, sections: List<AccountSection>? = null): AccountResult
      }
  }
  ```

**DataService 구현**

- [ ] **AccountDataService.kt** 확장
  ```kotlin
  @Service
  class AccountDataService(
      private val accountRepository: AccountRepository,
      private val balanceRepository: BalanceRepository,
      private val accountHolderRepository: AccountHolderRepository,
      private val accountLimitRepository: AccountLimitRepository,
      private val accountStatusHistoryRepository: AccountStatusHistoryRepository
  ) {
      @Transactional
      fun createAccount(command: AccountCommand.Create): AccountResult {
          // 1. 계좌번호 중복 확인
          require(accountRepository.findByAccountNumber(command.accountNumber) == null) {
              "Account number already exists"
          }

          // 2. Account Entity 생성
          val account = command.toEntity()
          val savedAccount = accountRepository.save(account)

          // 3. 초기 Balance 생성
          val balance = Balance(
              accountId = savedAccount.id!!,
              ledgerBalance = BigDecimal.ZERO,
              availableBalance = BigDecimal.ZERO,
              holdBalance = BigDecimal.ZERO,
              interestAccumulated = BigDecimal.ZERO,
              updatedAt = LocalDateTime.now()
          )
          balanceRepository.save(balance)

          // 4. AccountLimit 기본값 설정
          val limit = createDefaultLimit(savedAccount.id!!, command.productType)
          accountLimitRepository.save(limit)

          // 5. AccountHolder 연결
          val holder = AccountHolder(
              accountId = savedAccount.id!!,
              customerId = command.customerId,
              role = HolderRole.PRIMARY,
              joinedAt = LocalDateTime.now()
          )
          accountHolderRepository.save(holder)

          // 6. AccountStatusHistory 기록
          val history = AccountStatusHistory(
              accountId = savedAccount.id!!,
              previousStatus = null,
              newStatus = AccountStatus.ACTIVE,
              reason = "계좌 개설",
              changedAt = LocalDateTime.now(),
              changedBy = "SYSTEM"
          )
          accountStatusHistoryRepository.save(history)

          return AccountResult.from(savedAccount, balance)
      }

      private fun createDefaultLimit(accountId: Long, productType: ProductType): AccountLimit {
          return when (productType) {
              ProductType.BASIC_CHECKING -> AccountLimit(
                  accountId = accountId,
                  dailyTransferLimit = BigDecimal("10000000"),
                  singleTransferLimit = BigDecimal("5000000"),
                  monthlyDepositLimit = null,
                  updatedAt = LocalDateTime.now()
              )
              ProductType.RECORD_BOOK -> AccountLimit(
                  accountId = accountId,
                  dailyTransferLimit = BigDecimal.ZERO,
                  singleTransferLimit = BigDecimal.ZERO,
                  monthlyDepositLimit = BigDecimal("10000000"), // 섹션당
                  updatedAt = LocalDateTime.now()
              )
              // 기타 상품별 한도 설정
              else -> AccountLimit(
                  accountId = accountId,
                  dailyTransferLimit = BigDecimal.ZERO,
                  singleTransferLimit = BigDecimal.ZERO,
                  monthlyDepositLimit = null,
                  updatedAt = LocalDateTime.now()
              )
          }
      }
  }
  ```

**상품별 개설 로직**

- [ ] **GroupMeetingAccountService.kt** 생성 (모임통장)
  ```kotlin
  @Service
  class GroupMeetingAccountService(
      private val accountDataService: AccountDataService,
      private val groupMeetingRepository: GroupMeetingRepository
  ) {
      @Transactional
      fun createGroupMeetingAccount(
          command: AccountCommand.Create,
          meetingName: String,
          monthlyFee: BigDecimal?
      ): AccountResult {
          // 기본 계좌 개설
          val result = accountDataService.createAccount(command)

          // GroupMeeting 정보 생성
          val meeting = GroupMeeting(
              accountId = result.id!!,
              name = meetingName,
              monthlyFee = monthlyFee,
              maxMembers = 100,
              currentMembers = 1
          )
          groupMeetingRepository.save(meeting)

          return result
      }
  }
  ```

- [ ] **ChildAccountService.kt** 생성 (우리아이통장)
- [ ] **RecordBookAccountService.kt** 생성 (기록통장)
- [ ] **SafeboxAccountService.kt** 생성 (세이프박스)

**테스트 작성**

- [ ] **AccountDataServiceTest.kt** 작성
  - [ ] 계좌 개설 성공 케이스
  - [ ] 계좌번호 중복 실패 케이스
  - [ ] Balance, AccountHolder, AccountLimit, StatusHistory 자동 생성 확인

---

### 2.2 잔액 조회 및 거래 처리
**우선순위**: 🔴 High
**예상 시간**: 4일

#### 작업 내용

**잔액 조회**

- [ ] **BalanceDataService.kt** 생성
  ```kotlin
  @Service
  class BalanceDataService(
      private val balanceRepository: BalanceRepository,
      private val holdRepository: HoldRepository
  ) {
      @Transactional(readOnly = true)
      fun getBalance(accountId: Long): Balance {
          return balanceRepository.findByAccountId(accountId)
              ?: throw EntityNotFoundException("Balance not found for account: $accountId")
      }

      @Transactional(readOnly = true)
      fun getAvailableBalance(accountId: Long): BigDecimal {
          val balance = getBalance(accountId)
          val activeHolds = holdRepository.sumActiveHoldsByAccountId(accountId) ?: BigDecimal.ZERO
          return balance.ledgerBalance - activeHolds
      }
  }
  ```

**거래 처리 - Command 추가**

- [ ] **TransactionCommand.kt** 생성
  ```kotlin
  sealed interface TransactionCommand {
      data class Deposit(
          val accountId: Long,
          val amount: BigDecimal,
          val description: String,
          val memo: String? = null,
          val sectionId: Long? = null
      ) : TransactionCommand

      data class Withdraw(
          val accountId: Long,
          val amount: BigDecimal,
          val description: String,
          val sectionId: Long? = null
      ) : TransactionCommand

      data class Transfer(
          val fromAccountId: Long,
          val toAccountId: Long,
          val amount: BigDecimal,
          val description: String
      ) : TransactionCommand
  }
  ```

**거래 처리 - Service 구현**

- [ ] **TransactionDataService.kt** 생성
  ```kotlin
  @Service
  class TransactionDataService(
      private val accountRepository: AccountRepository,
      private val balanceRepository: BalanceRepository,
      private val ledgerEntryRepository: LedgerEntryRepository,
      private val accountLimitRepository: AccountLimitRepository,
      private val limitUsageRepository: LimitUsageRepository,
      private val holdRepository: HoldRepository,
      private val accountSectionRepository: AccountSectionRepository
  ) {
      @Transactional(isolation = Isolation.SERIALIZABLE)
      fun deposit(command: TransactionCommand.Deposit): LedgerEntry {
          // 1. 계좌 상태 확인
          val account = accountRepository.findById(command.accountId).orElseThrow()
          require(account.status == AccountStatus.ACTIVE || account.status == AccountStatus.DORMANT) {
              "Account is not active"
          }

          // 2. Balance 업데이트 (낙관적 락)
          val balance = balanceRepository.findByAccountIdWithLock(command.accountId)
              ?: throw EntityNotFoundException("Balance not found")

          balance.ledgerBalance += command.amount
          balance.availableBalance = balance.ledgerBalance - balance.holdBalance
          balance.updatedAt = LocalDateTime.now()
          balanceRepository.save(balance)

          // 3. LedgerEntry 기록
          val entry = LedgerEntry(
              accountId = command.accountId,
              entryType = EntryType.CREDIT,
              amount = command.amount,
              balanceAfter = balance.ledgerBalance,
              description = command.description,
              memo = command.memo,
              sectionId = command.sectionId,
              transactionId = generateTransactionId(),
              occurredAt = LocalDateTime.now()
          )
          val savedEntry = ledgerEntryRepository.save(entry)

          // 4. 기록통장: 섹션별 금액 업데이트
          if (command.sectionId != null) {
              updateSectionAmount(command.sectionId, command.amount)
          }

          return savedEntry
      }

      @Transactional(isolation = Isolation.SERIALIZABLE)
      fun withdraw(command: TransactionCommand.Withdraw): LedgerEntry {
          // 1. 계좌 상태 확인 (ACTIVE만 허용)
          val account = accountRepository.findById(command.accountId).orElseThrow()
          require(account.status == AccountStatus.ACTIVE) {
              "Account is not active"
          }

          // 2. 한도 확인
          val limit = accountLimitRepository.findByAccountId(command.accountId)
              ?: throw EntityNotFoundException("AccountLimit not found")

          val today = LocalDate.now()
          val dailyUsage = limitUsageRepository.sumDailyUsageByAccountIdAndDate(command.accountId, today) ?: BigDecimal.ZERO

          require(command.amount <= limit.singleTransferLimit) {
              "Amount exceeds single transfer limit"
          }
          require(dailyUsage + command.amount <= limit.dailyTransferLimit) {
              "Amount exceeds daily transfer limit"
          }

          // 3. 잔액 확인
          val balance = balanceRepository.findByAccountIdWithLock(command.accountId)
              ?: throw EntityNotFoundException("Balance not found")

          val availableBalance = balance.ledgerBalance - balance.holdBalance
          require(command.amount <= availableBalance) {
              "Insufficient balance"
          }

          // 4. Hold 확인 및 처리
          val activeHolds = holdRepository.findByAccountIdAndStatus(command.accountId, HoldStatus.ACTIVE)
          // Hold 만료 확인 및 해제 로직...

          // 5. Balance 업데이트
          balance.ledgerBalance -= command.amount
          balance.availableBalance = balance.ledgerBalance - balance.holdBalance
          balance.updatedAt = LocalDateTime.now()
          balanceRepository.save(balance)

          // 6. LedgerEntry 기록
          val entry = LedgerEntry(
              accountId = command.accountId,
              entryType = EntryType.DEBIT,
              amount = command.amount,
              balanceAfter = balance.ledgerBalance,
              description = command.description,
              sectionId = command.sectionId,
              transactionId = generateTransactionId(),
              occurredAt = LocalDateTime.now()
          )
          val savedEntry = ledgerEntryRepository.save(entry)

          // 7. 기록통장: 섹션별 금액 차감
          if (command.sectionId != null) {
              updateSectionAmount(command.sectionId, -command.amount)
          }

          // 8. 한도 사용량 업데이트
          updateLimitUsage(command.accountId, command.amount, today)

          return savedEntry
      }

      @Transactional(isolation = Isolation.SERIALIZABLE)
      fun transfer(command: TransactionCommand.Transfer): Pair<LedgerEntry, LedgerEntry> {
          // 출금 처리
          val withdrawEntry = withdraw(TransactionCommand.Withdraw(
              accountId = command.fromAccountId,
              amount = command.amount,
              description = "이체 출금: ${command.description}"
          ))

          // 입금 처리
          val depositEntry = deposit(TransactionCommand.Deposit(
              accountId = command.toAccountId,
              amount = command.amount,
              description = "이체 입금: ${command.description}"
          ))

          return Pair(withdrawEntry, depositEntry)
      }

      private fun updateSectionAmount(sectionId: Long, amount: BigDecimal) {
          val section = accountSectionRepository.findById(sectionId).orElseThrow()
          section.currentAmount += amount
          accountSectionRepository.save(section)
      }

      private fun updateLimitUsage(accountId: Long, amount: BigDecimal, date: LocalDate) {
          val usage = limitUsageRepository.findByAccountIdAndUsageDate(accountId, date)
              ?: LimitUsage(accountId = accountId, usageDate = date, dailyUsage = BigDecimal.ZERO)

          usage.dailyUsage += amount
          limitUsageRepository.save(usage)
      }

      private fun generateTransactionId(): String {
          return "TXN${System.currentTimeMillis()}${(1000..9999).random()}"
      }
  }
  ```

**테스트 작성**

- [ ] **TransactionDataServiceTest.kt** 작성
  - [ ] 입금 성공 케이스
  - [ ] 출금 성공 케이스
  - [ ] 잔액 부족 실패 케이스
  - [ ] 한도 초과 실패 케이스
  - [ ] 이체 성공 케이스 (원자성 확인)
  - [ ] 동시성 테스트 (낙관적 락)

---

### 2.3 이자 계산 및 지급
**우선순위**: 🟡 Medium
**예상 시간**: 3일

#### 작업 내용

- [ ] **InterestService.kt** 생성
  ```kotlin
  @Service
  class InterestService(
      private val accountRepository: AccountRepository,
      private val balanceRepository: BalanceRepository,
      private val ledgerEntryRepository: LedgerEntryRepository,
      private val interestPaymentRepository: InterestPaymentRepository
  ) {
      companion object {
          private const val TAX_RATE = 0.154 // 15.4%
      }

      @Transactional
      fun calculateDailyInterest(accountId: Long) {
          val balance = balanceRepository.findByAccountId(accountId) ?: return
          val account = accountRepository.findById(accountId).orElse(null) ?: return

          if (account.status != AccountStatus.ACTIVE) return

          // 일 이자 = 잔액 × (연 이율 / 365)
          val dailyInterest = balance.ledgerBalance * (account.interestRate / BigDecimal(365) / BigDecimal(100))

          balance.interestAccumulated += dailyInterest
          balanceRepository.save(balance)
      }

      @Transactional
      fun payMonthlyInterest(accountId: Long) {
          val balance = balanceRepository.findByAccountId(accountId) ?: return
          val account = accountRepository.findById(accountId).orElse(null) ?: return

          val interestAmount = balance.interestAccumulated
          if (interestAmount <= BigDecimal.ZERO) return

          // 세금 공제
          val taxAmount = interestAmount * BigDecimal(TAX_RATE)
          val netInterest = interestAmount - taxAmount

          // 잔액에 이자 추가
          balance.ledgerBalance += netInterest
          balance.availableBalance = balance.ledgerBalance - balance.holdBalance
          balance.interestAccumulated = BigDecimal.ZERO
          balance.updatedAt = LocalDateTime.now()
          balanceRepository.save(balance)

          // 원장 기록
          ledgerEntryRepository.save(LedgerEntry(
              accountId = accountId,
              entryType = EntryType.CREDIT,
              amount = netInterest,
              balanceAfter = balance.ledgerBalance,
              description = "이자 지급 (세후)",
              transactionId = "INT${System.currentTimeMillis()}",
              occurredAt = LocalDateTime.now()
          ))

          // 이자 지급 이력
          val today = LocalDate.now()
          interestPaymentRepository.save(InterestPayment(
              accountId = accountId,
              amount = netInterest,
              interestRate = account.interestRate,
              calculationPeriodStart = today.minusMonths(1).withDayOfMonth(1),
              calculationPeriodEnd = today.minusDays(1),
              paidAt = LocalDateTime.now()
          ))
      }

      @Transactional
      fun payDailyCompoundInterest(safeboxId: Long) {
          val balance = balanceRepository.findByAccountId(safeboxId) ?: return
          val account = accountRepository.findById(safeboxId).orElse(null) ?: return

          if (account.productType != ProductType.SAFEBOX) return
          if (account.status != AccountStatus.ACTIVE) return

          val dailyInterest = balance.ledgerBalance * (account.interestRate / BigDecimal(365) / BigDecimal(100))
          val taxAmount = dailyInterest * BigDecimal(TAX_RATE)
          val netInterest = dailyInterest - taxAmount

          // 매일 원금에 추가 (복리)
          balance.ledgerBalance += netInterest
          balance.availableBalance = balance.ledgerBalance - balance.holdBalance
          balance.updatedAt = LocalDateTime.now()
          balanceRepository.save(balance)

          ledgerEntryRepository.save(LedgerEntry(
              accountId = safeboxId,
              entryType = EntryType.CREDIT,
              amount = netInterest,
              balanceAfter = balance.ledgerBalance,
              description = "일일 복리 이자 (세후)",
              transactionId = "DCI${System.currentTimeMillis()}",
              occurredAt = LocalDateTime.now()
          ))
      }
  }
  ```

**배치 작업 구현**

- [ ] **InterestBatchJob.kt** 생성
  ```kotlin
  @Component
  class InterestBatchJob(
      private val accountRepository: AccountRepository,
      private val interestService: InterestService
  ) {
      private val logger = LoggerFactory.getLogger(InterestBatchJob::class.java)

      @Scheduled(cron = "0 0 0 * * *") // 매일 자정
      fun calculateAllDailyInterest() {
          logger.info("Starting daily interest calculation")

          val activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE)

          activeAccounts.chunked(1000).forEach { chunk ->
              chunk.parallelStream().forEach { account ->
                  try {
                      interestService.calculateDailyInterest(account.id!!)
                  } catch (e: Exception) {
                      logger.error("Failed to calculate interest for account ${account.id}", e)
                  }
              }
          }

          logger.info("Completed daily interest calculation for ${activeAccounts.size} accounts")
      }

      @Scheduled(cron = "0 0 1 * * FRI") // 매주 금요일 새벽 1시
      fun payMonthlyInterest() {
          if (!isLastFridayOfMonth()) return

          logger.info("Starting monthly interest payment")

          val accounts = accountRepository.findAll()
              .filter { it.productType != ProductType.SAFEBOX }

          accounts.forEach { account ->
              try {
                  interestService.payMonthlyInterest(account.id!!)
              } catch (e: Exception) {
                  logger.error("Failed to pay interest for account ${account.id}", e)
              }
          }

          logger.info("Completed monthly interest payment for ${accounts.size} accounts")
      }

      @Scheduled(cron = "0 0 0 * * *") // 매일 자정
      fun paySafeboxDailyInterest() {
          logger.info("Starting Safebox daily interest payment")

          val safeboxAccounts = accountRepository.findByProductType(ProductType.SAFEBOX)

          safeboxAccounts.forEach { account ->
              try {
                  interestService.payDailyCompoundInterest(account.id!!)
              } catch (e: Exception) {
                  logger.error("Failed to pay Safebox interest for account ${account.id}", e)
              }
          }

          logger.info("Completed Safebox interest payment for ${safeboxAccounts.size} accounts")
      }

      private fun isLastFridayOfMonth(): Boolean {
          val today = LocalDate.now()
          val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
          val lastFriday = lastDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
          return today == lastFriday
      }
  }
  ```

**테스트 작성**

- [ ] **InterestServiceTest.kt** 작성
  - [ ] 일일 이자 계산 테스트
  - [ ] 월말 이자 지급 테스트 (세금 공제 확인)
  - [ ] 세이프박스 복리 계산 테스트

---

## Phase 3: 상품별 특화 기능 구현 (2주)

### 3.1 기록통장 기능
**우선순위**: 🟡 Medium
**예상 시간**: 3일

#### 작업 내용

- [ ] **AccountSectionCommand.kt** 생성
- [ ] **SavingRuleCommand.kt** 생성
- [ ] **RecordBookDataService.kt** 구현
  - [ ] 섹션 생성 (최대 10개 제한)
  - [ ] 섹션별 금액 관리
  - [ ] 저축 규칙 생성 (섹션당 최대 20개)
  - [ ] 규칙 기반 자동 이체
- [ ] **SavingRuleBatchJob.kt** 구현 (규칙 실행)
- [ ] 테스트 작성

---

### 3.2 모임통장 기능
**우선순위**: 🟡 Medium
**예상 시간**: 3일

#### 작업 내용

- [ ] **GroupMeetingCommand.kt** 생성
- [ ] **GroupMeetingDataService.kt** 구현
  - [ ] 모임원 초대 및 관리
  - [ ] 회비 설정 및 알림
  - [ ] 최대 인원 제한 (100명)
  - [ ] 모임주/모임원 권한 관리
- [ ] 테스트 작성

---

### 3.3 우리아이통장 기능
**우선순위**: 🟡 Medium
**예상 시간**: 2일

#### 작업 내용

- [ ] **ChildAccountCommand.kt** 생성
- [ ] **ChildAccountDataService.kt** 구현
  - [ ] 부모-자녀 연결
  - [ ] 거래 메모 기능
  - [ ] 한도 조정 (부모 권한)
  - [ ] 법정대리인 확인
- [ ] 테스트 작성

---

### 3.4 세이프박스 기능
**우선순위**: 🟡 Medium
**예상 시간**: 2일

#### 작업 내용

- [ ] **SafeboxCommand.kt** 생성
- [ ] **SafeboxDataService.kt** 구현
  - [ ] 연결 계좌 확인
  - [ ] 계좌당 1개 제한
  - [ ] 입금 한도 관리 (기본 1천만원, 최대 1억원)
  - [ ] 매일 복리 이자 계산
- [ ] 테스트 작성

---

## Phase 4: Application Layer 구현 (1주)

### 4.1 DTO 및 Mapper 구현
**우선순위**: 🔴 High
**예상 시간**: 2일

#### 작업 내용

- [ ] **AccountDto.kt** 확장
  - [ ] 상품별 Request DTO
  - [ ] Response DTO with Mapper
- [ ] **TransactionDto.kt** 생성
- [ ] **BalanceDto.kt** 생성
- [ ] **SectionDto.kt** 생성 (기록통장)
- [ ] **MeetingDto.kt** 생성 (모임통장)

---

### 4.2 Application Service 구현
**우선순위**: 🔴 High
**예상 시간**: 3일

#### 작업 내용

- [ ] **AccountService.kt** 확장
  - [ ] DTO → Command 변환
  - [ ] Result → DTO 변환
  - [ ] 비즈니스 로직 조율
- [ ] **TransactionService.kt** 생성
- [ ] **RecordBookService.kt** 생성
- [ ] **GroupMeetingService.kt** 생성
- [ ] 통합 테스트 작성

---

## Phase 5: API 및 Controller 구현 (1주)

### 5.1 Controller 구현
**우선순위**: 🔴 High
**예상 시간**: 3일

#### 작업 내용

- [ ] **AccountController.kt** 생성
  - [ ] `POST /api/v1/accounts` - 계좌 개설
  - [ ] `GET /api/v1/accounts/{accountNumber}` - 계좌 조회
  - [ ] `PATCH /api/v1/accounts/{accountNumber}` - 계좌 수정
  - [ ] `DELETE /api/v1/accounts/{accountNumber}` - 계좌 해지
  - [ ] `GET /api/v1/accounts/{accountNumber}/balance` - 잔액 조회
  - [ ] `GET /api/v1/accounts/{accountNumber}/transactions` - 거래 내역

- [ ] **TransactionController.kt** 생성
  - [ ] `POST /api/v1/accounts/{accountNumber}/deposit` - 입금
  - [ ] `POST /api/v1/accounts/{accountNumber}/withdraw` - 출금
  - [ ] `POST /api/v1/accounts/{accountNumber}/transfer` - 이체

- [ ] **RecordBookController.kt** 생성
  - [ ] 섹션 CRUD API
  - [ ] 저축 규칙 CRUD API

- [ ] **GroupMeetingController.kt** 생성
  - [ ] 모임원 관리 API
  - [ ] 회비 관리 API

---

### 5.2 API 문서화 및 검증
**우선순위**: 🟡 Medium
**예상 시간**: 2일

#### 작업 내용

- [ ] SpringDoc OpenAPI 설정
- [ ] API 문서 자동 생성
- [ ] Validation 추가
- [ ] Exception Handler 구현
- [ ] API 통합 테스트

---

## Phase 6: 성능 최적화 및 모니터링 (1주)

### 6.1 성능 최적화
**우선순위**: 🟢 Low
**예상 시간**: 3일

#### 작업 내용

- [ ] 캐싱 전략 구현 (Redis)
  - [ ] 계좌 정보 캐싱
  - [ ] 잔액 정보 캐싱
- [ ] 데이터베이스 인덱스 생성
- [ ] N+1 쿼리 최적화
- [ ] 배치 처리 최적화
- [ ] 성능 테스트 (JMeter)

---

### 6.2 모니터링 및 로깅
**우선순위**: 🟢 Low
**예상 시간**: 2일

#### 작업 내용

- [ ] Actuator 설정
- [ ] Prometheus 메트릭 수집
- [ ] Grafana 대시보드 구성
- [ ] 로깅 전략 구현
  - [ ] 거래 로그
  - [ ] 에러 로그
  - [ ] 성능 로그
- [ ] 알림 설정

---

## 추가 작업 (Optional)

### 이벤트 기반 아키텍처
**우선순위**: 🟢 Low
**예상 시간**: 1주

- [ ] Domain Event 정의
- [ ] Event Publisher 구현
- [ ] Event Listener 구현
- [ ] Kafka 연동

### 보안 강화
**우선순위**: 🟡 Medium
**예상 시간**: 3일

- [ ] JWT 인증/인가
- [ ] API Rate Limiting
- [ ] 민감 정보 암호화
- [ ] 감사 로그 (Audit Log)

---

## 작업 진행 가이드라인

### 우선순위 정의
- 🔴 **High**: 핵심 기능, 반드시 구현 필요
- 🟡 **Medium**: 주요 기능, 가능한 구현
- 🟢 **Low**: 부가 기능, 여유있을 때 구현

### 개발 프로세스
1. Entity/Repository 구현 및 테스트
2. DataService 구현 및 단위 테스트
3. Application Service 구현 및 통합 테스트
4. Controller 구현 및 API 테스트
5. 성능 테스트 및 최적화

### 테스트 커버리지 목표
- 단위 테스트: 80% 이상
- 통합 테스트: 주요 시나리오 100%
- API 테스트: 모든 엔드포인트

### 문서화
- 각 Phase 완료 시 진행 상황 업데이트
- 주요 의사결정 및 변경사항 기록
- API 문서 자동 생성 및 유지보수

---

## 참고 사항

### 기술 스택
- Kotlin 1.9+
- Spring Boot 3.2+
- JPA (Hibernate)
- H2 (테스트) / PostgreSQL (운영)
- Redis (캐싱)
- Kafka (이벤트)

### 코드 품질
- Ktlint (코드 스타일)
- Detekt (정적 분석)
- JaCoCo (커버리지)

### CI/CD
- GitHub Actions
- 자동 테스트 실행
- 코드 리뷰 필수

---

## 예상 일정

| Phase | 작업 내용 | 기간 | 시작일 | 종료일 |
|-------|----------|------|--------|--------|
| Phase 1 | 기본 인프라 구축 | 1주 | Week 1 | Week 1 |
| Phase 2 | 핵심 비즈니스 로직 | 2-3주 | Week 2 | Week 4 |
| Phase 3 | 상품별 특화 기능 | 2주 | Week 5 | Week 6 |
| Phase 4 | Application Layer | 1주 | Week 7 | Week 7 |
| Phase 5 | API 및 Controller | 1주 | Week 8 | Week 8 |
| Phase 6 | 성능 최적화 | 1주 | Week 9 | Week 9 |
| **총 예상 기간** | | **8-10주** | | |

---

## 체크리스트 사용법

각 작업 항목 앞의 `[ ]`를 완료 시 `[x]`로 변경하여 진행 상황을 추적합니다.

예시:
- [ ] 작업 시작 전
- [x] 작업 완료

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-02-08
**작성자**: Development Team