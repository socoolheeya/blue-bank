package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.BenefitStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.BenefitType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
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