package com.socoolheeya.bluebank.loan.data.service

import com.socoolheeya.bluebank.loan.data.domain.command.LoanApplicationCommand
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.domain.result.LoanApplicationResult
import com.socoolheeya.bluebank.loan.data.repository.LoanApplicationRepository
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class LoanApplicationDataService(
    private val loanApplicationRepository: LoanApplicationRepository,
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun submitApplication(command: LoanApplicationCommand.Submit): LoanApplicationResult {
        val application = command.toEntity()
        val savedApplication = loanApplicationRepository.save(application)
        return LoanApplicationResult.from(savedApplication)
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        approvedAmount: BigDecimal,
        approvedRate: BigDecimal,
        loanCommand: LoanCommand.Create
    ): LoanApplicationResult {
        require(approvedAmount > BigDecimal.ZERO) { "승인 금액은 0보다 커야 합니다" }
        require(approvedRate > BigDecimal.ZERO) { "승인 금리는 0보다 커야 합니다" }
        val application = loanApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        // 1. Loan 생성
        val loan = loanCommand.copy(
            principalAmount = approvedAmount,
            interestRate = approvedRate
        ).toEntity()
        val savedLoan = loanRepository.save(loan)

        // 2. Application 승인 처리
        application.approve(approvedAmount, approvedRate, savedLoan.id!!)
        val updatedApplication = loanApplicationRepository.save(application)

        return LoanApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun rejectApplication(applicationId: Long, reason: String): LoanApplicationResult {
        val application = loanApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.reject(reason)
        val updatedApplication = loanApplicationRepository.save(application)

        return LoanApplicationResult.from(updatedApplication)
    }

    @Transactional(readOnly = true)
    fun getApplication(applicationId: Long): LoanApplicationResult? {
        return loanApplicationRepository.findById(applicationId)
            .map { LoanApplicationResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getApplicationsByCustomerId(customerId: Long): List<LoanApplicationResult> {
        return loanApplicationRepository.findByCustomerId(customerId)
            .map { LoanApplicationResult.from(it) }
    }
}
