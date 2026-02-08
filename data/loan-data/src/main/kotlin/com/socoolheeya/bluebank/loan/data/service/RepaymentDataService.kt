package com.socoolheeya.bluebank.loan.data.service

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.entity.Repayment
import com.socoolheeya.bluebank.loan.data.domain.result.RepaymentResult
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import com.socoolheeya.bluebank.loan.data.repository.RepaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class RepaymentDataService(
    private val repaymentRepository: RepaymentRepository,
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun createRepayment(
        loanId: Long,
        repaymentType: LoanEnums.RepaymentType,
        principalAmount: BigDecimal,
        interestAmount: BigDecimal,
        scheduledDate: LocalDate
    ): RepaymentResult {
        val loan = loanRepository.findById(loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: $loanId") }

        val repayment = Repayment(
            loanId = loanId,
            repaymentType = repaymentType,
            principalAmount = principalAmount,
            interestAmount = interestAmount,
            totalAmount = principalAmount.add(interestAmount),
            balanceAfter = loan.outstandingBalance.subtract(principalAmount),
            scheduledDate = scheduledDate
        )

        val savedRepayment = repaymentRepository.save(repayment)
        return RepaymentResult.from(savedRepayment)
    }

    @Transactional
    fun processRepayment(repaymentId: Long): RepaymentResult {
        val repayment = repaymentRepository.findById(repaymentId)
            .orElseThrow { NoSuchElementException("상환 내역을 찾을 수 없습니다: $repaymentId") }

        repayment.process()
        val updatedRepayment = repaymentRepository.save(repayment)

        return RepaymentResult.from(updatedRepayment)
    }

    @Transactional(readOnly = true)
    fun getRepaymentsByLoanId(loanId: Long): List<RepaymentResult> {
        return repaymentRepository.findByLoanId(loanId)
            .map { RepaymentResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getScheduledRepayments(startDate: LocalDate, endDate: LocalDate): List<RepaymentResult> {
        return repaymentRepository.findByScheduledDateBetween(startDate, endDate)
            .map { RepaymentResult.from(it) }
    }

    @Transactional
    fun markOverdue(repaymentId: Long, days: Int, penalty: BigDecimal) {
        val repayment = repaymentRepository.findById(repaymentId)
            .orElseThrow { NoSuchElementException("상환 내역을 찾을 수 없습니다: $repaymentId") }

        repayment.markOverdue(days, penalty)
        repaymentRepository.save(repayment)
    }
}