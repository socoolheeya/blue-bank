package com.socoolheeya.bluebank.deposit.service

import com.socoolheeya.bluebank.deposit.adapter.AccountServiceClient
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.PeriodUnit
import com.socoolheeya.bluebank.deposit.data.domain.command.DepositCommand
import com.socoolheeya.bluebank.deposit.data.service.DepositDataService
import com.socoolheeya.bluebank.deposit.dto.DepositDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class DepositService(
    private val depositDataService: DepositDataService,
    private val accountServiceClient: AccountServiceClient
) {

    fun createDeposit(request: DepositDto.CreateRequest): DepositDto.Response {
        // 계좌 유효성 검증
        val accountValidation = accountServiceClient.validateAccount(request.accountId)
        require(accountValidation.isValid) {
            "계좌를 사용할 수 없습니다: ${accountValidation.message}"
        }

        // 계좌 소유자 확인
        val account = accountServiceClient.getAccount(request.accountId)
        require(account.customerId == request.customerId) {
            "본인 명의의 계좌만 사용 가능합니다"
        }

        val depositNumber = generateDepositNumber()

        val command = DepositCommand.Create(
            depositNumber = depositNumber,
            customerId = request.customerId,
            accountId = request.accountId,
            productType = DepositProductType.valueOf(request.productType),
            principalAmount = request.principalAmount,
            baseRate = request.baseRate,
            bonusRate = BigDecimal.ZERO,
            contractPeriod = request.contractPeriod,
            periodUnit = PeriodUnit.valueOf(request.periodUnit),
            startDate = request.startDate,
            maturityDate = request.maturityDate,
            monthlyPayment = request.monthlyPayment,
            minMonthlyPayment = request.minMonthlyPayment,
            maxMonthlyPayment = request.maxMonthlyPayment,
            autoTransferEnabled = request.autoTransferEnabled,
            autoTransferDay = request.autoTransferDay,
            autoTransferAmount = request.autoTransferAmount,
            autoRenewalEnabled = request.autoRenewalEnabled,
            initialWeeklyAmount = request.initialWeeklyAmount,
            childId = request.childId,
            parentIds = request.parentIds,
            maxBalance = request.maxBalance,
            spareChangeEnabled = request.spareChangeEnabled,
            aiSavingsEnabled = request.aiSavingsEnabled,
            interestPaymentDay = request.interestPaymentDay,
            isTaxFree = request.isTaxFree
        )

        val result = depositDataService.createDeposit(command)
        return DepositDto.Response.from(result)
    }

    fun activateDeposit(depositId: Long, customerId: Long): DepositDto.Response {
        // 본인 확인
        val deposit = depositDataService.getDeposit(depositId)
        require(deposit.customerId == customerId) { "본인의 예금/적금만 활성화 가능합니다" }

        val result = depositDataService.activateDeposit(depositId)
        return DepositDto.Response.from(result)
    }

    fun deposit(depositId: Long, customerId: Long, request: DepositDto.DepositRequest): DepositDto.Response {
        // 본인 확인
        val deposit = depositDataService.getDeposit(depositId)
        require(deposit.customerId == customerId) { "본인의 예금/적금만 입금 가능합니다" }

        depositDataService.deposit(depositId, request.amount, request.description)
        val result = depositDataService.getDeposit(depositId)
        return DepositDto.Response.from(result)
    }

    fun earlyWithdraw(depositId: Long, customerId: Long, request: DepositDto.WithdrawRequest): DepositDto.Response {
        // 본인 확인
        val deposit = depositDataService.getDeposit(depositId)
        require(deposit.customerId == customerId) { "본인의 예금/적금만 출금 가능합니다" }

        val result = depositDataService.earlyWithdraw(depositId, request.amount)
        return DepositDto.Response.from(result)
    }

    fun terminateDeposit(depositId: Long, customerId: Long): DepositDto.Response {
        // 본인 확인
        val deposit = depositDataService.getDeposit(depositId)
        require(deposit.customerId == customerId) { "본인의 예금/적금만 해지 가능합니다" }

        val result = depositDataService.terminateDeposit(depositId)
        return DepositDto.Response.from(result)
    }

    fun getDeposit(depositId: Long): DepositDto.Response {
        val result = depositDataService.getDeposit(depositId)
        return DepositDto.Response.from(result)
    }

    fun getDepositsByCustomer(customerId: Long): List<DepositDto.Response> {
        val results = depositDataService.getDepositsByCustomer(customerId)
        return results.map { DepositDto.Response.from(it) }
    }

    private fun generateDepositNumber(): String {
        return "DEP${System.currentTimeMillis()}${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
    }
}