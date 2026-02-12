package com.socoolheeya.bluebank.loan.adapter

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.math.BigDecimal

@FeignClient(
    name = "account-service",
    url = "\${feign.account.url:http://localhost:8080}"
)
interface AccountServiceClient {

    @GetMapping("/internal/accounts/{accountId}")
    fun getAccount(@PathVariable accountId: Long): AccountResponse

    @GetMapping("/internal/accounts/{accountId}/balance")
    fun getBalance(@PathVariable accountId: Long): BalanceResponse

    @GetMapping("/internal/accounts/{accountId}/validate")
    fun validateAccount(@PathVariable accountId: Long): AccountValidationResponse

    @PostMapping("/internal/accounts/{accountId}/transfer")
    fun transferMoney(
        @PathVariable accountId: Long,
        @RequestBody request: TransferRequest
    ): TransferResponse
}

data class AccountResponse(
    val id: Long,
    val accountNumber: String,
    val customerId: Long,
    val accountType: String,
    val accountName: String,
    val status: String,
    val currency: String
)

data class BalanceResponse(
    val accountId: Long,
    val ledgerBalance: BigDecimal,
    val availableBalance: BigDecimal,
    val holdBalance: BigDecimal
)

data class AccountValidationResponse(
    val accountId: Long,
    val isValid: Boolean,
    val isActive: Boolean,
    val exists: Boolean,
    val message: String
)

data class TransferRequest(
    val amount: BigDecimal,
    val description: String,
    val transactionType: String // LOAN_DISBURSEMENT, LOAN_REPAYMENT
)

data class TransferResponse(
    val transactionId: String,
    val success: Boolean,
    val message: String,
    val newBalance: BigDecimal
)