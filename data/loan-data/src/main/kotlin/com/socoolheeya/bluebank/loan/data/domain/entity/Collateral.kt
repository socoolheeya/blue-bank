package com.socoolheeya.bluebank.loan.data.domain.entity

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "collateral")
class Collateral(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var loanId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var collateralType: LoanEnums.CollateralType,

    // 부동산 담보
    @Column(length = 200)
    var address: String? = null,

    @Column(precision = 10, scale = 2)
    var area: BigDecimal? = null,

    @Column(length = 50)
    var buildingType: String? = null,

    var completionYear: Int? = null,

    // 자동차 담보
    @Column(length = 100)
    var vehicleModel: String? = null,

    var vehicleYear: Int? = null,

    @Column(length = 20)
    var vehicleNumber: String? = null,

    // 평가 정보
    @Column(nullable = false, precision = 15, scale = 2)
    var appraisedValue: BigDecimal,

    @Column(nullable = false)
    var appraisalDate: LocalDate,

    @Column(nullable = false, length = 100)
    var appraisalInstitution: String,

    // 등기 정보
    @Column(length = 50)
    var registrationNumber: String? = null,

    var registrationDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LoanEnums.CollateralStatus = LoanEnums.CollateralStatus.REGISTERED,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun release() {
        require(status == LoanEnums.CollateralStatus.REGISTERED) { "등록된 담보만 해제 가능합니다" }
        this.status = LoanEnums.CollateralStatus.RELEASED
    }

    fun foreclose() {
        this.status = LoanEnums.CollateralStatus.FORECLOSED
    }
}