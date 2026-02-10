package com.socoolheeya.bluebank.loan.data.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "credit_score_history")
class CreditScoreHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var customerId: Long,

    var loanApplicationId: Long? = null,

    @Column(nullable = false)
    var creditScore: Int,

    @Column(nullable = false, length = 20)
    var creditGrade: String,

    @Column(nullable = false, length = 50)
    var scoringAgency: String,

    @Column(nullable = false)
    var scoredAt: LocalDateTime,

    // 세부 점수
    var paymentHistory: Int? = null,

    var creditUsage: Int? = null,

    @Column(precision = 15, scale = 2)
    var debtAmount: BigDecimal? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)