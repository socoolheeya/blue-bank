package com.socoolheeya.bluebank.account.data.service

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.data.domain.LedgerEntry
import com.socoolheeya.bluebank.account.data.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BalanceDataService(
    private val balanceRepository: BalanceRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val accountLimitRepository: AccountLimitRepository,
    private val limitUsageRepository: LimitUsageRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * 잔액 조회
     */
    @Transactional(readOnly = true)
    fun getBalance(accountId: Long): Balance? {
        return balanceRepository.findByAccountId(accountId)
    }

    /**
     * 입금 (트랜잭션)
     */
    @Transactional
    fun deposit(
        accountId: Long,
        amount: BigDecimal,
        description: String? = null,
        memo: String? = null,
        sectionId: Long? = null,
        transactionId: String? = null
    ): Balance {
        require(amount > BigDecimal.ZERO) { "입금 금액은 0보다 커야 합니다" }

        // 계좌 상태 확인
        val account = accountRepository.findById(accountId).orElseThrow {
            NoSuchElementException("계좌를 찾을 수 없습니다: $accountId")
        }
        require(account.status == AccountEnums.AccountStatus.ACTIVE) {
            "활성 상태의 계좌만 입금할 수 있습니다"
        }

        // 잔액 업데이트
        val balance = balanceRepository.findByAccountId(accountId)
            ?: throw NoSuchElementException("잔액 정보를 찾을 수 없습니다: $accountId")

        balance.deposit(amount)
        val updatedBalance = balanceRepository.save(balance)

        // 원장 기록
        val ledgerEntry = LedgerEntry(
            accountId = accountId,
            type = AccountEnums.EntryType.DEPOSIT,
            amount = amount,
            balanceAfter = updatedBalance.ledgerBalance,
            description = description,
            memo = memo,
            sectionId = sectionId,
            transactionId = transactionId,
            occurredAt = LocalDateTime.now()
        )
        ledgerEntryRepository.save(ledgerEntry)

        return updatedBalance
    }

    /**
     * 출금 (트랜잭션)
     */
    @Transactional
    fun withdraw(
        accountId: Long,
        amount: BigDecimal,
        description: String? = null,
        memo: String? = null,
        transactionId: String? = null
    ): Balance {
        require(amount > BigDecimal.ZERO) { "출금 금액은 0보다 커야 합니다" }

        // 계좌 상태 확인
        val account = accountRepository.findById(accountId).orElseThrow {
            NoSuchElementException("계좌를 찾을 수 없습니다: $accountId")
        }
        require(account.status == AccountEnums.AccountStatus.ACTIVE) {
            "활성 상태의 계좌만 출금할 수 있습니다"
        }

        // 한도 체크
        checkTransferLimit(accountId, amount)

        // 잔액 업데이트
        val balance = balanceRepository.findByAccountId(accountId)
            ?: throw NoSuchElementException("잔액 정보를 찾을 수 없습니다: $accountId")

        balance.withdraw(amount) // 잔액 부족 시 예외 발생
        val updatedBalance = balanceRepository.save(balance)

        // 한도 사용량 업데이트
        updateLimitUsage(accountId, amount)

        // 원장 기록
        val ledgerEntry = LedgerEntry(
            accountId = accountId,
            type = AccountEnums.EntryType.WITHDRAWAL,
            amount = amount,
            balanceAfter = updatedBalance.ledgerBalance,
            description = description,
            memo = memo,
            transactionId = transactionId,
            occurredAt = LocalDateTime.now()
        )
        ledgerEntryRepository.save(ledgerEntry)

        return updatedBalance
    }

    /**
     * 이자 지급 (트랜잭션)
     */
    @Transactional
    fun payInterest(accountId: Long, interestAmount: BigDecimal): Balance {
        require(interestAmount > BigDecimal.ZERO) { "이자 금액은 0보다 커야 합니다" }

        val balance = balanceRepository.findByAccountId(accountId)
            ?: throw NoSuchElementException("잔액 정보를 찾을 수 없습니다: $accountId")

        balance.deposit(interestAmount)
        val updatedBalance = balanceRepository.save(balance)

        // 원장 기록
        val ledgerEntry = LedgerEntry(
            accountId = accountId,
            type = AccountEnums.EntryType.INTEREST,
            amount = interestAmount,
            balanceAfter = updatedBalance.ledgerBalance,
            description = "이자 지급",
            occurredAt = LocalDateTime.now()
        )
        ledgerEntryRepository.save(ledgerEntry)

        return updatedBalance
    }

    /**
     * 한도 체크
     */
    private fun checkTransferLimit(accountId: Long, amount: BigDecimal) {
        val limit = accountLimitRepository.findByAccountId(accountId)
            ?: throw IllegalStateException("한도 정보를 찾을 수 없습니다: $accountId")

        // 1회 한도 체크
        require(amount <= limit.singleTransferLimit) {
            "1회 이체 한도(${limit.singleTransferLimit}원)를 초과했습니다"
        }

        // 일일 한도 체크
        val today = java.time.LocalDate.now()
        val todayUsage = limitUsageRepository.findByAccountIdAndDate(accountId, today)
        val usedAmount = todayUsage?.usedAmount ?: BigDecimal.ZERO
        val newTotalAmount = usedAmount + amount

        require(newTotalAmount <= limit.dailyTransferLimit) {
            "일일 이체 한도(${limit.dailyTransferLimit}원)를 초과했습니다. 현재 사용액: ${usedAmount}원"
        }
    }

    /**
     * 한도 사용량 업데이트
     */
    private fun updateLimitUsage(accountId: Long, amount: BigDecimal) {
        val today = java.time.LocalDate.now()
        val usage = limitUsageRepository.findByAccountIdAndDate(accountId, today)

        if (usage == null) {
            // 신규 생성
            val newUsage = com.socoolheeya.bluebank.account.data.domain.LimitUsage(
                accountId = accountId,
                date = today,
                usedAmount = amount
            )
            limitUsageRepository.save(newUsage)
        } else {
            // 기존 사용량 업데이트
            usage.usedAmount += amount
            limitUsageRepository.save(usage)
        }
    }
}