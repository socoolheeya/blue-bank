package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.StatementStatus
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