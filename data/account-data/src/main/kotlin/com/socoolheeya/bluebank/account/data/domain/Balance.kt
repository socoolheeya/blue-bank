package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
class Balance(
    @Id
    var accountId: Long,

    var ledgerBalance: BigDecimal, // 총 잔액

    var availableBalance: BigDecimal, // 사용 가능 잔액

    var holdBalance: BigDecimal, // 보류 금액

    var interestAccumulated: BigDecimal = BigDecimal.ZERO, // 누적 이자

    var updatedAt: LocalDateTime,

    @Version
    var version: Long = 0 // 낙관적 락
) {
    fun deposit(amount: BigDecimal) {
        ledgerBalance += amount
        calculateAvailableBalance()
        updatedAt = LocalDateTime.now()
    }

    fun withdraw(amount: BigDecimal) {
        require(availableBalance >= amount) { "Insufficient balance" }
        ledgerBalance -= amount
        calculateAvailableBalance()
        updatedAt = LocalDateTime.now()
    }

    fun addHold(amount: BigDecimal) {
        holdBalance += amount
        calculateAvailableBalance()
        updatedAt = LocalDateTime.now()
    }

    fun releaseHold(amount: BigDecimal) {
        holdBalance -= amount
        calculateAvailableBalance()
        updatedAt = LocalDateTime.now()
    }

    fun calculateAvailableBalance() {
        availableBalance = ledgerBalance - holdBalance
    }
}