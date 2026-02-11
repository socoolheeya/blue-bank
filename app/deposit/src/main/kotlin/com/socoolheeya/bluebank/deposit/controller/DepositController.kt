package com.socoolheeya.bluebank.deposit.controller

import com.socoolheeya.bluebank.deposit.dto.DepositDto
import com.socoolheeya.bluebank.deposit.service.DepositService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/deposits")
class DepositController(
    private val depositService: DepositService
) {

    @PostMapping
    fun createDeposit(@RequestBody request: DepositDto.CreateRequest): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.createDeposit(request)
        return ResponseEntity.ok(deposit)
    }

    @GetMapping("/{depositId}")
    fun getDeposit(@PathVariable depositId: Long): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.getDeposit(depositId)
        return ResponseEntity.ok(deposit)
    }

    @GetMapping("/customer/{customerId}")
    fun getDepositsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<DepositDto.Response>> {
        val deposits = depositService.getDepositsByCustomer(customerId)
        return ResponseEntity.ok(deposits)
    }

    @PostMapping("/{depositId}/activate")
    fun activateDeposit(
        @PathVariable depositId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.activateDeposit(depositId, customerId)
        return ResponseEntity.ok(deposit)
    }

    @PostMapping("/{depositId}/deposit")
    fun deposit(
        @PathVariable depositId: Long,
        @RequestParam customerId: Long,
        @RequestBody request: DepositDto.DepositRequest
    ): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.deposit(depositId, customerId, request)
        return ResponseEntity.ok(deposit)
    }

    @PostMapping("/{depositId}/withdraw")
    fun earlyWithdraw(
        @PathVariable depositId: Long,
        @RequestParam customerId: Long,
        @RequestBody request: DepositDto.WithdrawRequest
    ): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.earlyWithdraw(depositId, customerId, request)
        return ResponseEntity.ok(deposit)
    }

    @PostMapping("/{depositId}/terminate")
    fun terminateDeposit(
        @PathVariable depositId: Long,
        @RequestParam customerId: Long
    ): ResponseEntity<DepositDto.Response> {
        val deposit = depositService.terminateDeposit(depositId, customerId)
        return ResponseEntity.ok(deposit)
    }
}