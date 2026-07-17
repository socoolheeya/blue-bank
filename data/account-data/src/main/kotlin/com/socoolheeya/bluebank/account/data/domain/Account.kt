package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Account(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var accountNumber: String,

    var name: String? = null,

    @Enumerated(EnumType.STRING)
    var accountType: AccountEnums.AccountType,

    @Enumerated(EnumType.STRING)
    var productType: AccountEnums.ProductType,

    @Enumerated(EnumType.STRING)
    var status: AccountEnums.AccountStatus = AccountEnums.AccountStatus.ACTIVE,

    @Column(precision = 8, scale = 6)
    var interestRate: BigDecimal = BigDecimal.ZERO,

    var openedAt: LocalDateTime? = null,

    var closedAt: LocalDateTime? = null,

    var parentAccountId: Long? = null,

    var linkedAccountId: Long? = null
) {
    fun updateName(newName: String) {
        this.name = newName
    }

    fun close() {
        this.status = AccountEnums.AccountStatus.CLOSED
        this.closedAt = LocalDateTime.now()
    }

    fun activate() {
        this.status = AccountEnums.AccountStatus.ACTIVE
    }

    fun freeze() {
        this.status = AccountEnums.AccountStatus.FROZEN
    }

    fun setDormant() {
        this.status = AccountEnums.AccountStatus.DORMANT
    }
}
