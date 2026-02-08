package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

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