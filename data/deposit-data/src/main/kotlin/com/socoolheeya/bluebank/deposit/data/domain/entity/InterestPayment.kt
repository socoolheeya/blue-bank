package com.socoolheeya.bluebank.deposit.data.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "interest_payment")
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var depositId: Long,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    var interestAmount: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 2)
    var appliedRate: BigDecimal,

    @Column(nullable = false)
    var calculationPeriodStart: LocalDate,

    @Column(nullable = false)
    var calculationPeriodEnd: LocalDate,

    @Column(nullable = false, precision = 15, scale = 2)
    var principalBalance: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var taxAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    var netInterest: BigDecimal,

    @Column(nullable = false)
    var paymentDate: LocalDate,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
