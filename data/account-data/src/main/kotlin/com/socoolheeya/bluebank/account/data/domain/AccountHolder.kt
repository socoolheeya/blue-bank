package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class AccountHolder(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long,

    var customerId: Long,

    @Enumerated(EnumType.STRING)
    var role: AccountEnums.HolderRole,

    var relationshipType: String? = null,

    var joinedAt: LocalDateTime = LocalDateTime.now()
) {
}