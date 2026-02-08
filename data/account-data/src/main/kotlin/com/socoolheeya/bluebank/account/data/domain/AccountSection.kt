package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

/**
 * 계좌 섹션 (기록통장용)
 * 기록통장의 각 섹션을 나타냄
 */
@Entity
class AccountSection(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var name: String, // 섹션 이름 (예: "여행 자금", "비상금")

    var targetAmount: java.math.BigDecimal? = null, // 목표 금액

    var displayOrder: Int = 0, // 섹션 표시 순서

    var createdAt: LocalDateTime = LocalDateTime.now()
) {
}