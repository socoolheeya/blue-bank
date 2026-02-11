package com.socoolheeya.bluebank.deposit.data.repository

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.entity.Deposit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DepositRepository : JpaRepository<Deposit, Long> {

    fun findByDepositNumber(depositNumber: String): Deposit?

    fun findByCustomerId(customerId: Long): List<Deposit>

    fun findByCustomerIdAndStatus(customerId: Long, status: DepositStatus): List<Deposit>

    fun findByStatus(status: DepositStatus): List<Deposit>

    fun findByMaturityDateAndStatus(maturityDate: java.time.LocalDate, status: DepositStatus): List<Deposit>
}
