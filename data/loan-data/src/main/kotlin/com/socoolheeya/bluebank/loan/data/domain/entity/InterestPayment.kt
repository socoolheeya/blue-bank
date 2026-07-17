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

@Entity(name = "LoanInterestPayment")
@Table(name = "loan_interest_payment")
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    var interestAmount: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 3)
    var interestRate: BigDecimal,

    @Column(nullable = false)
    var calculationPeriodStart: LocalDate,

    @Column(nullable = false)
    var calculationPeriodEnd: LocalDate,

    @Column(nullable = false, precision = 15, scale = 2)
    var principalBalance: BigDecimal,

    @Column(nullable = false)
    var dueDate: LocalDate,

    var paidDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LoanEnums.InterestPaymentStatus = LoanEnums.InterestPaymentStatus.PENDING,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun pay() {
        require(status == LoanEnums.InterestPaymentStatus.PENDING) { "미납 상태의 이자만 납부 가능합니다" }
        this.status = LoanEnums.InterestPaymentStatus.PAID
        this.paidDate = LocalDate.now()
    }

    fun markOverdue() {
        this.status = LoanEnums.InterestPaymentStatus.OVERDUE
    }
}
