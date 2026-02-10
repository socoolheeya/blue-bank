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
import java.time.LocalDateTime

@Entity
@Table(name = "loan_application")
class LoanApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false, precision = 15, scale = 2)
    var requestedAmount: BigDecimal,

    @Column(nullable = false)
    var requestedTerm: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var productType: LoanEnums.ProductType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var repaymentMethod: LoanEnums.RepaymentMethod,

    // 고객 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var annualIncome: BigDecimal,

    @Column(nullable = false, length = 50)
    var employmentType: String,

    @Column(nullable = false)
    var employmentPeriodMonths: Int,

    @Column(length = 100)
    var companyName: String? = null,

    // 신용 정보
    @Column(nullable = false)
    var creditScore: Int,

    @Column(nullable = false)
    var existingLoanCount: Int = 0,

    @Column(nullable = false, precision = 15, scale = 2)
    var totalExistingDebt: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var hasDelayHistory: Boolean = false,

    // 담보 정보
    @Column(length = 50)
    var collateralType: String? = null,

    @Column(precision = 15, scale = 2)
    var collateralValue: BigDecimal? = null,

    @Column(length = 200)
    var collateralAddress: String? = null,

    // 심사 결과
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LoanEnums.ApplicationStatus = LoanEnums.ApplicationStatus.SUBMITTED,

    @Column(precision = 15, scale = 2)
    var approvedAmount: BigDecimal? = null,

    @Column(precision = 5, scale = 3)
    var approvedRate: BigDecimal? = null,

    @Column(length = 500)
    var rejectionReason: String? = null,

    var loanId: Long? = null,

    @Column(nullable = false)
    var appliedAt: LocalDateTime = LocalDateTime.now(),

    var reviewedAt: LocalDateTime? = null
) {

    fun approve(amount: BigDecimal, rate: BigDecimal, loanId: Long) {
        this.status = LoanEnums.ApplicationStatus.APPROVED
        this.approvedAmount = amount
        this.approvedRate = rate
        this.loanId = loanId
        this.reviewedAt = LocalDateTime.now()
    }

    fun reject(reason: String) {
        this.status = LoanEnums.ApplicationStatus.REJECTED
        this.rejectionReason = reason
        this.reviewedAt = LocalDateTime.now()
    }

    fun startReview() {
        require(status == LoanEnums.ApplicationStatus.SUBMITTED) { "제출된 신청서만 심사 시작 가능합니다" }
        this.status = LoanEnums.ApplicationStatus.UNDER_REVIEW
    }
}