package com.socoolheeya.bluebank.account.controller

import com.socoolheeya.bluebank.account.adapter.AccountValidationDto
import com.socoolheeya.bluebank.account.adapter.BalanceDto
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.dto.AccountDto
import com.socoolheeya.bluebank.account.service.AccountService
import com.socoolheeya.bluebank.account.service.BalanceService
import org.springframework.web.bind.annotation.*

/**
 * 내부 API 전용 Controller
 * 다른 마이크로서비스(card, deposit, loan 등)에서 계좌 정보를 조회하기 위한 API
 * 외부 노출 X, 내부 통신 전용
 */
@RestController
@RequestMapping("/internal/accounts")
class InternalAccountController(
    private val accountService: AccountService,
    private val balanceService: BalanceService
) {

    /**
     * 계좌 ID로 계좌 정보 조회
     */
    @GetMapping("/{accountId}")
    fun getAccount(@PathVariable accountId: Long): AccountDto.Response {
        val result = accountService.getAccountById(accountId)
        return AccountDto.Response.from(result)
    }

    /**
     * 계좌번호로 계좌 정보 조회
     */
    @GetMapping("/by-number/{accountNumber}")
    fun getAccountByNumber(@PathVariable accountNumber: String): AccountDto.Response {
        val result = accountService.getAccountByAccountNumber(accountNumber)
        return AccountDto.Response.from(result)
    }

    /**
     * 고객 ID로 계좌 목록 조회
     */
    @GetMapping("/by-customer/{customerId}")
    fun getAccountsByCustomerId(@PathVariable customerId: Long): List<AccountDto.Response> {
        return accountService.getAccountsByCustomerId(customerId)
            .map { AccountDto.Response.from(it) }
    }

    /**
     * 계좌 잔액 조회
     */
    @GetMapping("/{accountId}/balance")
    fun getBalance(@PathVariable accountId: Long): BalanceDto {
        val balance = balanceService.getBalance(accountId)
        return BalanceDto(
            accountId = balance.accountId,
            ledgerBalance = balance.ledgerBalance,
            availableBalance = balance.availableBalance,
            holdBalance = balance.holdBalance
        )
    }

    /**
     * 계좌 유효성 검증
     * - 계좌 존재 여부
     * - 계좌 활성 상태 여부
     * - 사용 가능 여부
     */
    @GetMapping("/{accountId}/validate")
    fun validateAccount(@PathVariable accountId: Long): AccountValidationDto {
        return try {
            val account = accountService.getAccountById(accountId)
            val isActive = account.status == AccountEnums.AccountStatus.ACTIVE

            AccountValidationDto(
                accountId = accountId,
                isValid = isActive,
                isActive = isActive,
                exists = true,
                message = if (isActive) "유효한 계좌입니다" else "비활성 상태의 계좌입니다 (${account.status})"
            )
        } catch (e: NoSuchElementException) {
            AccountValidationDto(
                accountId = accountId,
                isValid = false,
                isActive = false,
                exists = false,
                message = "존재하지 않는 계좌입니다"
            )
        }
    }
}