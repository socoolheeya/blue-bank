package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
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