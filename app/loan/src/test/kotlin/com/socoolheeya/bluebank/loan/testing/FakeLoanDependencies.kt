package com.socoolheeya.bluebank.loan.testing

import com.socoolheeya.bluebank.loan.adapter.AccountValidationResponse
import com.socoolheeya.bluebank.loan.client.AccountClient
import com.socoolheeya.bluebank.loan.client.AccountResponse
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.command.LoanApplicationCommand
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.domain.result.LoanApplicationResult
import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import com.socoolheeya.bluebank.loan.data.repository.LoanApplicationRepository
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import com.socoolheeya.bluebank.loan.data.service.LoanApplicationDataService
import com.socoolheeya.bluebank.loan.data.service.LoanDataService
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
private fun <T> unused(type: Class<T>): T = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
    error("Unexpected repository call: ${method.name}")
} as T

class FakeLoanDataService : LoanDataService(unused(LoanRepository::class.java)) {
    val loans = linkedMapOf<Long, LoanResult>()
    val executeCommands = mutableListOf<LoanCommand.Execute>()
    val repayCommands = mutableListOf<LoanCommand.Repay>()

    fun add(command: LoanCommand.Create, id: Long = (loans.keys.maxOrNull() ?: 0) + 1): LoanResult =
        LoanResult.from(command.toEntity()).copy(id = id).also { loans[id] = it }

    override fun getLoan(loanId: Long) = loans[loanId]
    override fun getLoansByCustomerId(customerId: Long) = loans.values.filter { it.customerId == customerId }
    override fun executeLoan(command: LoanCommand.Execute): LoanResult {
        executeCommands += command
        return update(command.loanId) { it.copy(status = LoanEnums.LoanStatus.ACTIVE, updatedAt = LocalDateTime.now()) }
    }
    override fun repayLoan(command: LoanCommand.Repay): LoanResult {
        repayCommands += command
        return update(command.loanId) {
            val balance = it.outstandingBalance - command.amount
            it.copy(outstandingBalance = balance, status = if (balance.signum() == 0) LoanEnums.LoanStatus.SETTLED else it.status,
                updatedAt = LocalDateTime.now())
        }
    }
    private fun update(id: Long, change: (LoanResult) -> LoanResult): LoanResult =
        change(requireNotNull(loans[id])).also { loans[id] = it }
}

class FakeLoanApplicationDataService : LoanApplicationDataService(
    unused(LoanApplicationRepository::class.java), unused(LoanRepository::class.java)
) {
    val applications = linkedMapOf<Long, LoanApplicationResult>()
    val submitted = mutableListOf<LoanApplicationCommand.Submit>()
    val approvalCommands = mutableListOf<LoanCommand.Create>()
    private var nextId = 1L

    override fun submitApplication(command: LoanApplicationCommand.Submit): LoanApplicationResult {
        submitted += command
        return LoanApplicationResult.from(command.toEntity()).copy(id = nextId++).also { applications[it.id!!] = it }
    }
    override fun getApplication(applicationId: Long) = applications[applicationId]
    override fun getApplicationsByCustomerId(customerId: Long) = applications.values.filter { it.customerId == customerId }
    override fun approveApplication(applicationId: Long, approvedAmount: BigDecimal, approvedRate: BigDecimal,
                                    loanCommand: LoanCommand.Create): LoanApplicationResult {
        approvalCommands += loanCommand
        return update(applicationId) { it.copy(status = LoanEnums.ApplicationStatus.APPROVED, approvedAmount = approvedAmount,
            approvedRate = approvedRate, loanId = 1000 + applicationId, reviewedAt = LocalDateTime.now()) }
    }
    override fun rejectApplication(applicationId: Long, reason: String): LoanApplicationResult = update(applicationId) {
        it.copy(status = LoanEnums.ApplicationStatus.REJECTED, rejectionReason = reason, reviewedAt = LocalDateTime.now())
    }
    private fun update(id: Long, change: (LoanApplicationResult) -> LoanApplicationResult): LoanApplicationResult =
        change(applications[id] ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $id")).also { applications[id] = it }
}

class FakeAccountClient(var valid: Boolean = true) : AccountClient {
    val validatedAccountIds = mutableListOf<Long>()
    override fun validateAccount(accountId: Long): AccountValidationResponse {
        validatedAccountIds += accountId
        return AccountValidationResponse(accountId, valid, valid, valid, if (valid) "valid" else "invalid")
    }
    override fun getAccount(accountId: Long) = AccountResponse(accountId, 1, "100-$accountId", "test", 0.0, "ACTIVE")
}
