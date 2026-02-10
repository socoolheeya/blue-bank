package com.socoolheeya.bluebank.card.data.domain.entity

import com.socoolheeya.bluebank.card.data.domain.CardEnums.TransactionStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.TransactionType
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
