package com.socoolheeya.bluebank.loan.data.service

import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoanDataService(
    private val loanRepository: LoanRepository
) {

    @Transactional
    fun createLoan(command: LoanCommand.Create): LoanResult {
        // 1. 대출 번호 중복 확인
        if (loanRepository.existsByLoanNumber(command.loanNumber)) {
            throw IllegalArgumentException("이미 존재하는 대출 번호입니다: ${command.loanNumber}")
        }

        // 2. Entity 생성 및 저장
        val loan = command.toEntity()
        val savedLoan = loanRepository.save(loan)

        return LoanResult.from(savedLoan)
    }

    @Transactional
    fun approveLoan(command: LoanCommand.Approve): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.approve(command.approver)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun executeLoan(command: LoanCommand.Execute): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.execute()
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun repayLoan(command: LoanCommand.Repay): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.repay(command.amount)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional
    fun rejectLoan(command: LoanCommand.Reject): LoanResult {
        val loan = loanRepository.findById(command.loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: ${command.loanId}") }

        loan.reject(command.reason)
        val updatedLoan = loanRepository.save(loan)

        return LoanResult.from(updatedLoan)
    }

    @Transactional(readOnly = true)
    fun getLoan(loanId: Long): LoanResult? {
        return loanRepository.findById(loanId)
            .map { LoanResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getLoanByNumber(loanNumber: String): LoanResult? {
        return loanRepository.findByLoanNumber(loanNumber)
            .map { LoanResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getLoansByCustomerId(customerId: Long): List<LoanResult> {
        return loanRepository.findByCustomerId(customerId)
            .map { LoanResult.from(it) }
    }

    @Transactional
    fun markAsOverdue(loanId: Long) {
        val loan = loanRepository.findById(loanId)
            .orElseThrow { NoSuchElementException("대출을 찾을 수 없습니다: $loanId") }

        loan.markAsOverdue()
        loanRepository.save(loan)
    }
}