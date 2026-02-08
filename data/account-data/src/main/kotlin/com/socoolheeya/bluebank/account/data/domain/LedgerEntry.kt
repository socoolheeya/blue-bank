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
 * 입출금 기록 요약
 * 돈이 들어오고 나간 흔적
 */
@Entity
class LedgerEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    @Enumerated(EnumType.STRING)
    var type: AccountEnums.EntryType,

    var amount: BigDecimal,

    var balanceAfter: BigDecimal,

    var description: String? = null, // 거래 설명

    var memo: String? = null, // 사용자 메모

    var sectionId: Long? = null, // 섹션 ID (기록통장용)

    var transactionId: String? = null, // 외부 거래 ID

    var occurredAt: LocalDateTime
) {
}