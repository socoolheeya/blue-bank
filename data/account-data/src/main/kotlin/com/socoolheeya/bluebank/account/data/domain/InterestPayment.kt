package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 이자 지급 내역
 * 계좌에 지급된 이자 기록
 */
@Entity
class InterestPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var amount: BigDecimal, // 지급 이자

    var interestRate: BigDecimal, // 적용 이율

    var calculationPeriodStart: LocalDate, // 계산 기간 시작

    var calculationPeriodEnd: LocalDate, // 계산 기간 종료

    var averageBalance: BigDecimal, // 평균 잔액

    var paidAt: LocalDateTime = LocalDateTime.now() // 지급 시간
) {
}