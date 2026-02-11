package com.socoolheeya.bluebank.deposit.data.repository

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.entity.DepositTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface DepositTransactionRepository : JpaRepository<DepositTransaction, Long> {

    fun findByDepositId(depositId: Long): List<DepositTransaction>

    fun findByDepositIdOrderByTransactionDateDesc(depositId: Long): List<DepositTransaction>

    fun findByCustomerId(customerId: Long): List<DepositTransaction>

    fun findByDepositIdAndTransactionType(depositId: Long, transactionType: DepositTransactionType): List<DepositTransaction>

    fun findByDepositIdAndTransactionDateBetween(
        depositId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<DepositTransaction>
}
