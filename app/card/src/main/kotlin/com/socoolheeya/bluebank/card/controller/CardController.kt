package com.socoolheeya.bluebank.card.controller

import com.socoolheeya.bluebank.card.dto.CardDto
import com.socoolheeya.bluebank.card.service.CardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cards")
class CardController(
    private val cardService: CardService
) {

    @GetMapping
    fun test(): String {
        return "Card Service is running"
    }

    @GetMapping("/{cardId}")
    fun getCard(@PathVariable cardId: Long): ResponseEntity<CardDto.Response> {
        val card = cardService.getCard(cardId)
        return ResponseEntity.ok(card)
    }

    @GetMapping("/customer/{customerId}")
    fun getCardsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<CardDto.Response>> {
        val cards = cardService.getCardsByCustomerId(customerId)
        return ResponseEntity.ok(cards)
    }

    @GetMapping("/customer/{customerId}/active")
    fun getActiveCardsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<CardDto.Response>> {
        val cards = cardService.getActiveCardsByCustomerId(customerId)
        return ResponseEntity.ok(cards)
    }

    @PostMapping("/{cardId}/activate")
    fun activateCard(
        @PathVariable cardId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<CardDto.Response> {
        val card = cardService.activateCard(cardId, customerId)
        return ResponseEntity.ok(card)
    }

    @PutMapping("/{cardId}/toggle")
    fun toggleCardUsage(
        @PathVariable cardId: Long,
        @RequestParam customerId: Long,
        @RequestBody request: CardDto.ToggleUsageRequest
    ): ResponseEntity<CardDto.Response> {
        val card = cardService.toggleCardUsage(cardId, customerId, request.enabled)
        return ResponseEntity.ok(card)
    }

    @PostMapping("/{cardId}/report-lost")
    fun reportLostCard(
        @PathVariable cardId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<CardDto.Response> {
        val card = cardService.reportLostCard(cardId, customerId)
        return ResponseEntity.ok(card)
    }

    @PostMapping("/{cardId}/terminate")
    fun terminateCard(
        @PathVariable cardId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<CardDto.Response> {
        val card = cardService.terminateCard(cardId, customerId)
        return ResponseEntity.ok(card)
    }
}