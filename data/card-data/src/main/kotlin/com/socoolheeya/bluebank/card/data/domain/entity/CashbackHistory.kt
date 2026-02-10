package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CashbackStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CashbackType
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