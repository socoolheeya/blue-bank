package com.socoolheeya.bluebank.deposit.data.domain.entity

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "deposit_transaction")
class DepositTransaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var depositId: Long,

    @Column(nullable = false)
    var customerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var transactionType: DepositTransactionType,

    @Column(nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var balanceAfter: BigDecimal,

    @Column(length = 500)
    var description: String? = null,

    // 자동이체 정보
    @Column(nullable = false)
    var isAutoTransfer: Boolean = false,

    // 26주적금 전용
    var weekNumber: Int? = null,

    // 한달적금 전용
    var dayNumber: Int? = null,

    @Column(nullable = false)
    var transactionDate: LocalDateTime = LocalDateTime.now()
)
