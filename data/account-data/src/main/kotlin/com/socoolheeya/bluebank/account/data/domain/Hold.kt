package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Hold(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var amount: BigDecimal, //보류 금액

    var reason: String? = null, // 카드결제, 검사 등

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var expiresAt: LocalDateTime? = null,

    var releasedAt: LocalDateTime? = null,

    var status: AccountEnums.HoldStatus
) {
}