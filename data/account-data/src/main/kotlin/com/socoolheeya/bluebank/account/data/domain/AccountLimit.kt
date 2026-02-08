package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class AccountLimit(
    @Id
    var accountId: Long,

    var dailyTransferLimit: BigDecimal, // 하루 송금 한도

    var singleTransferLimit: BigDecimal, // 1회 송금 한도

    var monthlyDepositLimit: BigDecimal? = null, // 월 입금 한도 (기록통장 섹션용)

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
}