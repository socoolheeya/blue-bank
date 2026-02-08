package com.socoolheeya.bluebank.account.data.service

import com.socoolheeya.bluebank.account.data.domain.*
import com.socoolheeya.bluebank.account.data.domain.command.AccountCommand
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.data.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class AccountDataService(
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val accountHolderRepository: AccountHolderRepository,
    private val accountLimitRepository: AccountLimitRepository,
    private val accountStatusHistoryRepository: AccountStatusHistoryRepository
) {

    /**
     * 계좌 생성 (트랜잭션)
     */
    @Transactional
    fun createAccount(
        command: AccountCommand.Create,
        customerId: Long,
        holderRole: AccountEnums.HolderRole,
        defaultLimit: AccountLimit
    ): AccountResult {
        // 1. 계좌번호 중복 체크
        accountRepository.findByAccountNumber(command.accountNumber)?.let {
            throw IllegalArgumentException("이미 존재하는 계좌번호입니다: ${command.accountNumber}")
        }

        // 2. Account 생성
        val account = command.toEntity()
        account.openedAt = LocalDateTime.now()
        val savedAccount = accountRepository.save(account)

        // 3. Balance 초기화
        val balance = Balance(
            accountId = savedAccount.id!!,
            ledgerBalance = BigDecimal.ZERO,
            availableBalance = BigDecimal.ZERO,
            holdBalance = BigDecimal.ZERO,
            interestAccumulated = BigDecimal.ZERO,
            updatedAt = LocalDateTime.now()
        )
        balanceRepository.save(balance)

        // 4. AccountHolder 등록
        val accountHolder = AccountHolder(
            accountId = savedAccount.id!!,
            customerId = customerId,
            role = holderRole,
            relationshipType = null,
            joinedAt = LocalDateTime.now()
        )
        accountHolderRepository.save(accountHolder)

        // 5. AccountLimit 설정
        val newLimit = AccountLimit(
            accountId = savedAccount.id!!,
            dailyTransferLimit = defaultLimit.dailyTransferLimit,
            singleTransferLimit = defaultLimit.singleTransferLimit,
            monthlyDepositLimit = defaultLimit.monthlyDepositLimit,
            updatedAt = LocalDateTime.now()
        )
        accountLimitRepository.save(newLimit)

        // 6. AccountStatusHistory 기록
        val statusHistory = AccountStatusHistory(
            accountId = savedAccount.id!!,
            fromStatus = AccountEnums.AccountStatus.PENDING,
            toStatus = command.status,
            reason = "계좌 개설",
            changedBy = customerId,
            changedAt = LocalDateTime.now()
        )
        accountStatusHistoryRepository.save(statusHistory)

        return AccountResult.from(savedAccount)
    }

    /**
     * 계좌 조회 (계좌번호)
     */
    @Transactional(readOnly = true)
    fun searchAccount(command: AccountCommand.Search): AccountResult? {
        val account = accountRepository.findByAccountNumber(command.accountNumber)
        return account?.let { AccountResult.from(it) }
    }

    /**
     * 계좌 조회 (ID)
     */
    @Transactional(readOnly = true)
    fun getAccountById(id: Long): AccountResult? {
        return accountRepository.findById(id).map { AccountResult.from(it) }.orElse(null)
    }

    /**
     * 고객의 계좌 목록 조회
     */
    @Transactional(readOnly = true)
    fun getAccountsByCustomerId(customerId: Long): List<AccountResult> {
        return accountRepository.findByCustomerId(customerId)
            .map { AccountResult.from(it) }
    }

    /**
     * 계좌 정보 수정 (트랜잭션)
     */
    @Transactional
    fun modifyAccount(command: AccountCommand.Modify): AccountResult? {
        return accountRepository.findByAccountNumber(command.accountNumber)?.let {
            it.updateName(command.name ?: "")
            AccountResult.from(it)
        }
    }

    /**
     * 계좌 해지 (트랜잭션)
     */
    @Transactional
    fun closeAccount(accountNumber: String, customerId: Long, reason: String?): AccountResult {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: $accountNumber")

        // 잔액 확인
        val balance = balanceRepository.findByAccountId(account.id!!)
            ?: throw IllegalStateException("잔액 정보를 찾을 수 없습니다")

        if (balance.ledgerBalance > BigDecimal.ZERO) {
            throw IllegalStateException("잔액이 남아있어 계좌를 해지할 수 없습니다")
        }

        if (balance.holdBalance > BigDecimal.ZERO) {
            throw IllegalStateException("보류 금액이 있어 계좌를 해지할 수 없습니다")
        }

        // 계좌 상태 변경
        val previousStatus = account.status
        account.close()

        // 상태 히스토리 기록
        val statusHistory = AccountStatusHistory(
            accountId = account.id!!,
            fromStatus = previousStatus,
            toStatus = account.status,
            reason = reason ?: "고객 요청",
            changedBy = customerId,
            changedAt = LocalDateTime.now()
        )
        accountStatusHistoryRepository.save(statusHistory)

        val updatedAccount = accountRepository.save(account)
        return AccountResult.from(updatedAccount)
    }

    /**
     * 계좌 동결 (트랜잭션)
     */
    @Transactional
    fun freezeAccount(accountNumber: String, customerId: Long, reason: String): AccountResult {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: $accountNumber")

        val previousStatus = account.status
        account.freeze()

        val statusHistory = AccountStatusHistory(
            accountId = account.id!!,
            fromStatus = previousStatus,
            toStatus = account.status,
            reason = reason,
            changedBy = customerId,
            changedAt = LocalDateTime.now()
        )
        accountStatusHistoryRepository.save(statusHistory)

        val updatedAccount = accountRepository.save(account)
        return AccountResult.from(updatedAccount)
    }

    /**
     * 계좌 활성화 (트랜잭션)
     */
    @Transactional
    fun activateAccount(accountNumber: String, customerId: Long): AccountResult {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw NoSuchElementException("계좌를 찾을 수 없습니다: $accountNumber")

        val previousStatus = account.status
        account.activate()

        val statusHistory = AccountStatusHistory(
            accountId = account.id!!,
            fromStatus = previousStatus,
            toStatus = account.status,
            reason = "계좌 활성화",
            changedBy = customerId,
            changedAt = LocalDateTime.now()
        )
        accountStatusHistoryRepository.save(statusHistory)

        val updatedAccount = accountRepository.save(account)
        return AccountResult.from(updatedAccount)
    }
}