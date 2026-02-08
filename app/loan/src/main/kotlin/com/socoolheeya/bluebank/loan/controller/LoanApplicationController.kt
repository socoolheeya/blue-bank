package com.socoolheeya.bluebank.loan.controller

import com.socoolheeya.bluebank.loan.dto.LoanApplicationDto
import com.socoolheeya.bluebank.loan.service.LoanApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/loans/applications")
class LoanApplicationController(
    private val loanApplicationService: LoanApplicationService
) {

    @PostMapping
    fun applyForLoan(
        @RequestBody request: LoanApplicationDto.Request
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.applyForLoan(request)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/{applicationId}")
    fun getApplication(@PathVariable applicationId: Long): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.getApplication(applicationId)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/customer/{customerId}")
    fun getApplicationsByCustomer(
        @PathVariable customerId: Long
    ): ResponseEntity<List<LoanApplicationDto.Response>> {
        val applications = loanApplicationService.getApplicationsByCustomerId(customerId)
        return ResponseEntity.ok(applications)
    }

    @PostMapping("/{applicationId}/approve")
    fun approveApplication(
        @PathVariable applicationId: Long,
        @RequestBody request: LoanApplicationDto.ApproveRequest
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.approveApplication(
            applicationId,
            request.approvedAmount,
            request.approvedRate
        )
        return ResponseEntity.ok(application)
    }

    @PostMapping("/{applicationId}/reject")
    fun rejectApplication(
        @PathVariable applicationId: Long,
        @RequestBody request: LoanApplicationDto.RejectRequest
    ): ResponseEntity<LoanApplicationDto.Response> {
        val application = loanApplicationService.rejectApplication(applicationId, request.reason)
        return ResponseEntity.ok(application)
    }
}