package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 적금 규칙 (우리아이통장용)
 * 정기적인 자동 이체 규칙을 정의
 */
@Entity
class SavingRule(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long, // 대상 계좌 (우리아이통장)

    var sourceAccountId: Long, // 출금 계좌 (부모 계좌)

    var amount: BigDecimal, // 이체 금액

    @Enumerated(EnumType.STRING)
    var frequency: AccountEnums.RuleFrequency, // 주기 (매일, 매주, 매월)

    var dayOfExecution: Int? = null, // 실행일 (매주: 1-7, 매월: 1-31)

    var isActive: Boolean = true, // 활성화 여부

    var startDate: LocalDateTime, // 시작일

    var endDate: LocalDateTime? = null, // 종료일

    var lastExecutedAt: LocalDateTime? = null, // 마지막 실행 시간

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
}