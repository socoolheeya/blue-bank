package com.socoolheeya.bluebank.gateway.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestController
@RequestMapping("/fallback")
class FallbackController {

    @GetMapping("/account")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun accountFallback(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "service" to "Account",
                "message" to "Account service is temporarily unavailable. Please try again later.",
                "status" to HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp" to LocalDateTime.now().toString()
            )
        )
    }

    @GetMapping("/deposit")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun depositFallback(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "service" to "Deposit",
                "message" to "Deposit service is temporarily unavailable. Please try again later.",
                "status" to HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp" to LocalDateTime.now().toString()
            )
        )
    }

    @GetMapping("/loan")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun loanFallback(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "service" to "Loan",
                "message" to "Loan service is temporarily unavailable. Please try again later.",
                "status" to HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp" to LocalDateTime.now().toString()
            )
        )
    }

    @GetMapping("/card")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun cardFallback(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "service" to "Card",
                "message" to "Card service is temporarily unavailable. Please try again later.",
                "status" to HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp" to LocalDateTime.now().toString()
            )
        )
    }

    @GetMapping("/general")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun generalFallback(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "message" to "Service is temporarily unavailable. Please try again later.",
                "status" to HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp" to LocalDateTime.now().toString()
            )
        )
    }
}