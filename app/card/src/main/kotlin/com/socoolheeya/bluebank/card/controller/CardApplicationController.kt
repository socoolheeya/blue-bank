package com.socoolheeya.bluebank.card.controller

import com.socoolheeya.bluebank.card.dto.CardApplicationDto
import com.socoolheeya.bluebank.card.service.CardApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cards/applications")
class CardApplicationController(
    private val cardApplicationService: CardApplicationService
) {

    @PostMapping
    fun applyForCard(
        @RequestBody request: CardApplicationDto.Request
    ): ResponseEntity<CardApplicationDto.Response> {
        val application = cardApplicationService.applyForCard(request)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/{applicationId}")
    fun getApplication(@PathVariable applicationId: Long): ResponseEntity<CardApplicationDto.Response> {
        val application = cardApplicationService.getApplication(applicationId)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/customer/{customerId}")
    fun getApplicationsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<CardApplicationDto.Response>> {
        val applications = cardApplicationService.getApplicationsByCustomerId(customerId)
        return ResponseEntity.ok(applications)
    }

    @PostMapping("/{applicationId}/issue")
    fun issueCard(@PathVariable applicationId: Long): ResponseEntity<CardApplicationDto.IssueResponse> {
        val response = cardApplicationService.issueCard(applicationId)
        return ResponseEntity.ok(response)
    }
}