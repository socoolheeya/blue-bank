package com.socoolheeya.bluebank.loan.controller

import com.socoolheeya.bluebank.loan.dto.LoanDto
import com.socoolheeya.bluebank.loan.service.LoanService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/loans")
class LoanController(
    private val loanService: LoanService
) {

    @GetMapping("/{loanId}")
    fun getLoan(@PathVariable loanId: Long): ResponseEntity<LoanDto.Response> {
        val loan = loanService.getLoan(loanId)
        return ResponseEntity.ok(loan)
    }

    @GetMapping("/customer/{customerId}")
    fun getLoansByCustomer(@PathVariable customerId: Long): ResponseEntity<List<LoanDto.Response>> {
        val loans = loanService.getLoansByCustomerId(customerId)
        return ResponseEntity.ok(loans)
    }

    @PostMapping("/{loanId}/execute")
    fun executeLoan(@PathVariable loanId: Long): ResponseEntity<LoanDto.Response> {
        val loan = loanService.executeLoan(loanId)
        return ResponseEntity.ok(loan)
    }

    @PostMapping("/{loanId}/repay")
    fun repayLoan(
        @PathVariable loanId: Long,
        @RequestBody request: LoanDto.RepayRequest
    ): ResponseEntity<LoanDto.Response> {
        val loan = loanService.repayLoan(loanId, request.amount)
        return ResponseEntity.ok(loan)
    }
}