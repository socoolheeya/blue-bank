package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class AccountStatusHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var fromStatus: AccountEnums.AccountStatus,

    var toStatus: AccountEnums.AccountStatus,

    var reason: String? = null,

    var changedBy: Long? = null, // 변경한 사용자 ID

    var changedAt: LocalDateTime = LocalDateTime.now()
) {
}