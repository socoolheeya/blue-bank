package com.socoolheeya.bluebank.loan.client

import com.socoolheeya.bluebank.loan.adapter.AccountValidationResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "account-service",
    url = "\${feign.account.url:http://localhost:8081}"
)
interface AccountClient {

    @GetMapping("/api/accounts/{accountId}")
    fun getAccount(@PathVariable accountId: Long): AccountResponse

    @GetMapping("/api/accounts/{accountId}/validate")
    fun validateAccount(@PathVariable accountId: Long): AccountValidationResponse
}

data class AccountResponse(
    val id: Long,
    val customerId: Long,
    val accountNumber: String,
    val accountName: String,
    val balance: Double,
    val status: String
)