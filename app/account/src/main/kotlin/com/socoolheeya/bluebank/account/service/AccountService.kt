package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.AccountLimit
import com.socoolheeya.bluebank.account.data.domain.command.AccountCommand
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.data.service.AccountDataService
import com.socoolheeya.bluebank.account.dto.AccountDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class AccountService(
    private val accountDataService: AccountDataService
) {

    /**
     * 계좌 개설
     */
    fun createAccount(request: AccountDto.CreateRequest): AccountResult {
        val command = request.toCommand()
        val defaultLimit = createDefaultAccountLimit(request.productType)

        return accountDataService.createAccount(
            command = command,
            customerId = request.customerId,
            holderRole = request.holderRole,
            defaultLimit = defaultLimit
        )
    }

    /**
     * 계좌 조회 (계좌번호)
     */
    fun getAccountByAccountNumber(accountNumber: String): AccountResult {
        return accountDataService.searchAccount(
            AccountCommand.Search(accountNumber)
        ) ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: $accountNumber")
    }

    /**
     * 계좌 조회 (ID)
     */
    fun getAccountById(id: Long): AccountResult {
        return accountDataService.getAccountById(id)
            ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: $id")
    }

    /**
     * 고객의 계좌 목록 조회
     */
    fun getAccountsByCustomerId(customerId: Long): List<AccountResult> {
        return accountDataService.getAccountsByCustomerId(customerId)
    }

    /**
     * 계좌 정보 수정
     */
    fun modifyAccount(request: AccountDto.ModifyRequest): AccountResult {
        return accountDataService.modifyAccount(request.toCommand())
            ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: ${request.accountNumber}")
    }

    /**
     * 계좌 해지
     */
    fun closeAccount(accountNumber: String, customerId: Long, reason: String?): AccountResult {
        return accountDataService.closeAccount(accountNumber, customerId, reason)
    }

    /**
     * 계좌 동결
     */
    fun freezeAccount(accountNumber: String, customerId: Long, reason: String): AccountResult {
        return accountDataService.freezeAccount(accountNumber, customerId, reason)
    }

    /**
     * 계좌 활성화
     */
    fun activateAccount(accountNumber: String, customerId: Long): AccountResult {
        return accountDataService.activateAccount(accountNumber, customerId)
    }

    /**
     * 상품 유형별 기본 한도 설정
     */
    private fun createDefaultAccountLimit(productType: AccountEnums.ProductType): AccountLimit {
        return when (productType) {
            AccountEnums.ProductType.BASIC_CHECKING -> AccountLimit(
                accountId = 0L, // will be set by DataService
                dailyTransferLimit = BigDecimal("10000000"), // 1천만원
                singleTransferLimit = BigDecimal("5000000"), // 500만원
                monthlyDepositLimit = null
            )
            AccountEnums.ProductType.GROUP_MEETING -> AccountLimit(
                accountId = 0L,
                dailyTransferLimit = BigDecimal("5000000"),
                singleTransferLimit = BigDecimal("2000000"),
                monthlyDepositLimit = null
            )
            AccountEnums.ProductType.CHILD_ACCOUNT -> AccountLimit(
                accountId = 0L,
                dailyTransferLimit = BigDecimal("1000000"),
                singleTransferLimit = BigDecimal("500000"),
                monthlyDepositLimit = null
            )
            AccountEnums.ProductType.RECORD_BOOK -> AccountLimit(
                accountId = 0L,
                dailyTransferLimit = BigDecimal("10000000"),
                singleTransferLimit = BigDecimal("5000000"),
                monthlyDepositLimit = BigDecimal("30000000") // 월 3천만원
            )
            AccountEnums.ProductType.SAFEBOX -> AccountLimit(
                accountId = 0L,
                dailyTransferLimit = BigDecimal("10000000"),
                singleTransferLimit = BigDecimal("5000000"),
                monthlyDepositLimit = null
            )
        }
    }
}
