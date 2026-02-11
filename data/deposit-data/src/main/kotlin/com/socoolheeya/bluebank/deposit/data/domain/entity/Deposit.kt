package com.socoolheeya.bluebank.deposit.data.domain.entity

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.PeriodUnit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "deposit")
class Deposit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    @Column(unique = true, nullable = false)
    var depositNumber: String,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var accountId: Long,

    // 상품 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var productType: DepositProductType,

    // 금액 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var principalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var currentBalance: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    var accumulatedInterest: BigDecimal = BigDecimal.ZERO,

    // 금리 정보
    @Column(nullable = false, precision = 5, scale = 2)
    var baseRate: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 2)
    var bonusRate: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 2)
    var appliedRate: BigDecimal,

    // 기간 정보
    @Column(nullable = false)
    var contractPeriod: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var periodUnit: PeriodUnit,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var maturityDate: LocalDate,

    // 납입 정보 (적금)
    @Column(precision = 15, scale = 2)
    var monthlyPayment: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var minMonthlyPayment: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var maxMonthlyPayment: BigDecimal? = null,

    @Column(nullable = false)
    var totalDepositCount: Int = 0,

    // 자동이체 정보
    @Column(nullable = false)
    var autoTransferEnabled: Boolean = false,

    var autoTransferDay: Int? = null,

    @Column(precision = 15, scale = 2)
    var autoTransferAmount: BigDecimal? = null,

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DepositStatus,

    // 중도인출 정보
    @Column(nullable = false)
    var earlyWithdrawalCount: Int = 0,

    @Column(nullable = false)
    var maxEarlyWithdrawals: Int = 2,

    // 자동재예치 정보 (정기예금)
    @Column(nullable = false)
    var autoRenewalEnabled: Boolean = false,

    @Column(nullable = false)
    var renewalCount: Int = 0,

    @Column(nullable = false)
    var maxRenewals: Int = 5,

    // 26주적금 전용
    @Column(precision = 15, scale = 2)
    var initialWeeklyAmount: BigDecimal? = null,

    @Column(nullable = false)
    var currentWeek: Int = 0,

    // 한달적금 전용
    @Column(nullable = false)
    var currentDay: Int = 0,

    @Column(nullable = false)
    var depositDaysCompleted: Int = 0,

    // 우리아이적금 전용
    var childId: Long? = null,

    @Column(length = 500)
    var parentIds: String? = null,

    // 저금통 전용
    @Column(precision = 15, scale = 2)
    var maxBalance: BigDecimal? = null,

    @Column(nullable = false)
    var spareChangeEnabled: Boolean = false,

    @Column(nullable = false)
    var aiSavingsEnabled: Boolean = false,

    // 이자 지급
    var lastInterestPaymentDate: LocalDate? = null,

    var interestPaymentDay: Int? = null,

    // 세금
    @Column(nullable = false)
    var isTaxFree: Boolean = false,

    // 감사
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
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

    /**
     * 자동재예치
     */
    fun renew(newMaturityDate: LocalDate) {
        require(status == DepositStatus.MATURED) { "만기된 상품만 재예치 가능합니다" }
        require(autoRenewalEnabled) { "자동재예치가 활성화되지 않았습니다" }
        require(renewalCount < maxRenewals) { "최대 재예치 횟수 초과" }

        this.status = DepositStatus.ACTIVE
        this.startDate = LocalDate.now()
        this.maturityDate = newMaturityDate
        this.renewalCount++
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 우대금리 업데이트
     */
    fun updateBonusRate(bonusRate: BigDecimal) {
        this.bonusRate = bonusRate
        this.appliedRate = baseRate + bonusRate
        this.updatedAt = LocalDateTime.now()
    }
}
