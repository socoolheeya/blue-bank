package com.socoolheeya.bluebank.loan.data.domain.entity

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "loan")
class Loan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // 기본 정보
    @Column(unique = true, nullable = false, length = 50)
    var loanNumber: String,

    @Column(nullable = false)
    var customerId: Long,

    @Column(nullable = false)
    var accountId: Long,

    // 대출 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var loanType: LoanEnums.LoanType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var productType: LoanEnums.ProductType,

    // 대출 금액 및 금리
    @Column(nullable = false, precision = 15, scale = 2)
    var principalAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var outstandingBalance: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 3)
    var interestRate: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var rateType: LoanEnums.RateType,

    // 대출 기간
    @Column(nullable = false)
    var loanTerm: Int,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var maturityDate: LocalDate,

    // 상환 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var repaymentMethod: LoanEnums.RepaymentMethod,

    @Column(precision = 15, scale = 2)
    var monthlyPayment: BigDecimal? = null,

    // 대출 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LoanEnums.LoanStatus,

    // 담보 정보
    var collateralId: Long? = null,

    @Column(precision = 5, scale = 2)
    var loanToValueRatio: BigDecimal? = null,

    // 대환 정보
    var refinanceSourceLoanId: Long? = null,

    @Column(nullable = false)
    var isRefinanced: Boolean = false,

    // 우대 금리
    @Column(nullable = false, precision = 5, scale = 3)
    var preferentialRate: BigDecimal = BigDecimal.ZERO,

    @Column(length = 200)
    var discountReason: String? = null,

    // 기타
    var creditScore: Int? = null,

    @Column(length = 50)
    var approvedBy: String? = null,

    // 감사
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 대출 승인
     */
    fun approve(approver: String) {
        require(status == LoanEnums.LoanStatus.PENDING) { "대출 심사 중인 상태만 승인 가능합니다" }
        this.status = LoanEnums.LoanStatus.APPROVED
        this.approvedBy = approver
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 대출 실행
     */
    fun execute() {
        require(status == LoanEnums.LoanStatus.APPROVED) { "승인된 대출만 실행 가능합니다" }
        this.status = LoanEnums.LoanStatus.ACTIVE
        this.startDate = LocalDate.now()
        this.maturityDate = startDate.plusMonths(loanTerm.toLong())
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 대출 상환
     */
    fun repay(amount: BigDecimal) {
        require(status == LoanEnums.LoanStatus.ACTIVE) { "실행 중인 대출만 상환 가능합니다" }
        require(amount <= outstandingBalance) { "상환 금액이 잔여 원금을 초과할 수 없습니다" }

        this.outstandingBalance = outstandingBalance.subtract(amount)
        this.updatedAt = LocalDateTime.now()

        if (outstandingBalance <= BigDecimal.ZERO) {
            this.status = LoanEnums.LoanStatus.SETTLED
        }
    }

    /**
     * 대출 거절
     */
    fun reject(reason: String) {
        require(status == LoanEnums.LoanStatus.PENDING) { "심사 중인 대출만 거절 가능합니다" }
        this.status = LoanEnums.LoanStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 연체 처리
     */
    fun markAsOverdue() {
        require(status == LoanEnums.LoanStatus.ACTIVE) { "실행 중인 대출만 연체 처리 가능합니다" }
        this.status = LoanEnums.LoanStatus.OVERDUE
        this.updatedAt = LocalDateTime.now()
    }
}