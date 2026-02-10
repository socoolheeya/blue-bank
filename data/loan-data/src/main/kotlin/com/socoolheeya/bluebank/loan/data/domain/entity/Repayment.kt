package com.socoolheeya.bluebank.loan.data.domain.entity

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
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
@Table(name = "repayment")
class Repayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var repaymentType: LoanEnums.RepaymentType,

    @Column(nullable = false, precision = 15, scale = 2)
    var principalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var interestAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var balanceAfter: BigDecimal,

    @Column(nullable = false)
    var scheduledDate: LocalDate,

    var actualDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LoanEnums.RepaymentStatus = LoanEnums.RepaymentStatus.SCHEDULED,

    @Column(nullable = false)
    var isOverdue: Boolean = false,

    @Column(nullable = false)
    var overdueDays: Int = 0,

    @Column(nullable = false, precision = 15, scale = 2)
    var penaltyAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun process() {
        require(status == LoanEnums.RepaymentStatus.SCHEDULED) { "예정된 상환만 처리 가능합니다" }
        this.status = LoanEnums.RepaymentStatus.COMPLETED
        this.actualDate = LocalDate.now()

        // 연체 체크
        if (LocalDate.now().isAfter(scheduledDate)) {
            this.isOverdue = true
            this.overdueDays = java.time.temporal.ChronoUnit.DAYS.between(scheduledDate, LocalDate.now()).toInt()
        }
    }

    fun markOverdue(days: Int, penalty: BigDecimal) {
        this.status = LoanEnums.RepaymentStatus.OVERDUE
        this.isOverdue = true
        this.overdueDays = days
        this.penaltyAmount = penalty
    }
}