package com.socoolheeya.bluebank.loan.data.repository

import com.socoolheeya.bluebank.loan.data.domain.entity.Collateral
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CollateralRepository : JpaRepository<Collateral, Long> {
    fun findByLoanId(loanId: Long): Optional<Collateral>
    fun findByStatus(status: LoanEnums.CollateralStatus): List<Collateral>
}