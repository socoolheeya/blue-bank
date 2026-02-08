package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 한도 사용량
 * 오늘 이미 얼마나 썼는지를 기록
 */
@Entity
class LimitUsage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var date: LocalDate,

    var usedAmount: BigDecimal
) {
}