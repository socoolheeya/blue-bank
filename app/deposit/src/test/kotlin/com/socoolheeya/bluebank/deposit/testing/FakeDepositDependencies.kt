package com.socoolheeya.bluebank.deposit.testing

import com.socoolheeya.bluebank.deposit.adapter.*
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.command.DepositCommand
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositResult
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositTransactionResult
import com.socoolheeya.bluebank.deposit.data.repository.*
import com.socoolheeya.bluebank.deposit.data.service.DepositDataService
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
private fun <T> unused(type: Class<T>): T = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
    error("Unexpected repository call: ${method.name}")
} as T

class FakeDepositDataService : DepositDataService(
    unused(DepositRepository::class.java), unused(DepositTransactionRepository::class.java), unused(InterestPaymentRepository::class.java)
) {
    val deposits = linkedMapOf<Long, DepositResult>()
    val created = mutableListOf<DepositCommand.Create>()
    val contributions = mutableListOf<DepositTransactionResult>()
    private var nextId = 1L
    private var nextTransactionId = 1L

    override fun createDeposit(command: DepositCommand.Create): DepositResult {
        created += command
        val result = DepositResult.from(command.toEntity()).copy(id = nextId++)
        deposits[requireNotNull(result.id)] = result
        return result
    }

    override fun getDeposit(depositId: Long) = deposits[depositId]
        ?: throw IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
    override fun getDepositByNumber(depositNumber: String) = deposits.values.firstOrNull { it.depositNumber == depositNumber }
    override fun getDepositsByCustomer(customerId: Long) = deposits.values.filter { it.customerId == customerId }

    override fun activateDeposit(depositId: Long) = update(depositId) {
        require(it.status == DepositStatus.PENDING) { "대기 중인 상품만 활성화 가능합니다" }
        it.copy(status = DepositStatus.ACTIVE, updatedAt = LocalDateTime.now())
    }
    override fun earlyWithdraw(depositId: Long, amount: BigDecimal) = update(depositId) {
        require(it.status == DepositStatus.ACTIVE) { "활성 상태에서만 출금 가능합니다" }
        require(amount > BigDecimal.ZERO) { "출금액은 0보다 커야 합니다" }
        require(amount <= it.currentBalance) { "잔액 부족" }
        it.copy(currentBalance = it.currentBalance - amount, updatedAt = LocalDateTime.now())
    }
    override fun terminateDeposit(depositId: Long) = update(depositId) {
        require(it.status == DepositStatus.ACTIVE) { "활성 상태만 해지 가능합니다" }
        it.copy(status = DepositStatus.TERMINATED, updatedAt = LocalDateTime.now())
    }

    override fun deposit(depositId: Long, amount: BigDecimal, description: String?): DepositTransactionResult {
        val updated = update(depositId) {
            require(it.status == DepositStatus.ACTIVE) { "활성 상태에서만 입금 가능합니다" }
            require(amount > BigDecimal.ZERO) { "입금액은 0보다 커야 합니다" }
            it.copy(currentBalance = it.currentBalance + amount, updatedAt = LocalDateTime.now())
        }
        return DepositTransactionResult(nextTransactionId++, depositId, updated.customerId, DepositTransactionType.DEPOSIT,
            amount, updated.currentBalance, description, false, null, null, LocalDateTime.now()).also(contributions::add)
    }

    private fun update(id: Long, change: (DepositResult) -> DepositResult): DepositResult {
        val updated = change(getDeposit(id))
        deposits[id] = updated
        return updated
    }
}

class FakeAccountServiceClient : AccountServiceClient {
    private val accounts = linkedMapOf<Long, AccountResponse>()
    val invalidAccountIds = mutableSetOf<Long>()

    fun addAccount(id: Long, customerId: Long) {
        accounts[id] = AccountResponse(id, "110-$id", customerId, "CHECKING", "primary", "ACTIVE", "KRW")
    }

    override fun getAccount(accountId: Long) = accounts[accountId] ?: error("Account not found: $accountId")
    override fun getBalance(accountId: Long) = BalanceResponse(accountId, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO)
    override fun validateAccount(accountId: Long): AccountValidationResponse {
        val exists = accountId in accounts
        val valid = exists && accountId !in invalidAccountIds
        return AccountValidationResponse(accountId, valid, valid, exists, if (valid) "valid" else "invalid")
    }
}
