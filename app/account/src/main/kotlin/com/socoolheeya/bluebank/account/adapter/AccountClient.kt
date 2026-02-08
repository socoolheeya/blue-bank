package com.socoolheeya.bluebank.account.adapter

import com.socoolheeya.bluebank.account.dto.AccountDto
import com.socoolheeya.bluebank.account.dto.AccountFeignConfiguration
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

/**
 * Account 내부 API 통신용 Feign Client
 * 다른 서비스(card, deposit, loan 등)에서 계좌 정보를 조회할 때 사용
 */
@FeignClient(
    name = "account-service",
    url = $$"${feign.account.url:http://localhost:8080}",
    configuration = [AccountFeignConfiguration::class]
)
interface AccountClient {

    /**
     * 계좌 ID로 계좌 정보 조회
     */
    @GetMapping("/internal/accounts/{accountId}")
    fun getAccount(@PathVariable accountId: Long): AccountDto.Response

    /**
     * 계좌번호로 계좌 정보 조회
     */
    @GetMapping("/internal/accounts/by-number/{accountNumber}")
    fun getAccountByNumber(@PathVariable accountNumber: String): AccountDto.Response

    /**
     * 고객 ID로 계좌 목록 조회
     */
    @GetMapping("/internal/accounts/by-customer/{customerId}")
    fun getAccountsByCustomerId(@PathVariable customerId: Long): List<AccountDto.Response>

    /**
     * 계좌 잔액 조회
     */
    @GetMapping("/internal/accounts/{accountId}/balance")
    fun getBalance(@PathVariable accountId: Long): BalanceDto

    /**
     * 계좌 유효성 검증 (존재 여부 + 활성 상태)
     */
    @GetMapping("/internal/accounts/{accountId}/validate")
    fun validateAccount(@PathVariable accountId: Long): AccountValidationDto
}

/**
 * 잔액 정보 DTO
 */
data class BalanceDto(
    val accountId: Long,
    val ledgerBalance: java.math.BigDecimal,
    val availableBalance: java.math.BigDecimal,
    val holdBalance: java.math.BigDecimal
)

/**
 * 계좌 유효성 검증 결과 DTO
 */
data class AccountValidationDto(
    val accountId: Long,
    val isValid: Boolean,
    val isActive: Boolean,
    val exists: Boolean,
    val message: String?
)