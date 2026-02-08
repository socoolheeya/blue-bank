package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.data.service.BalanceDataService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BalanceService(
    private val balanceDataService: BalanceDataService
) {

    /**
     * 잔액 조회
     */
    fun getBalance(accountId: Long): Balance {
        return balanceDataService.getBalance(accountId)
            ?: throw NoSuchElementException("잔액 정보를 찾을 수 없습니다: $accountId")
    }

    /**
     * 입금
     */
    fun deposit(
        accountId: Long,
        amount: BigDecimal,
        description: String? = null,
        memo: String? = null,
        sectionId: Long? = null,
        transactionId: String? = null
    ): Balance {
        return balanceDataService.deposit(
            accountId = accountId,
            amount = amount,
            description = description,
            memo = memo,
            sectionId = sectionId,
            transactionId = transactionId
        )
    }

    /**
     * 출금
     */
    fun withdraw(
        accountId: Long,
        amount: BigDecimal,
        description: String? = null,
        memo: String? = null,
        transactionId: String? = null
    ): Balance {
        return balanceDataService.withdraw(
            accountId = accountId,
            amount = amount,
            description = description,
            memo = memo,
            transactionId = transactionId
        )
    }

    /**
     * 이체 (출금 + 입금)
     */
    fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
        description: String? = null,
        transactionId: String? = null
    ): Pair<Balance, Balance> {
        require(amount > BigDecimal.ZERO) { "이체 금액은 0보다 커야 합니다" }
        require(fromAccountId != toAccountId) { "동일한 계좌로는 이체할 수 없습니다" }

        // 출금
        val fromBalance = withdraw(
            accountId = fromAccountId,
            amount = amount,
            description = description ?: "이체 출금",
            transactionId = transactionId
        )

        // 입금
        val toBalance = deposit(
            accountId = toAccountId,
            amount = amount,
            description = description ?: "이체 입금",
            transactionId = transactionId
        )

        return Pair(fromBalance, toBalance)
    }
}