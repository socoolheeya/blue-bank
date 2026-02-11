package com.socoolheeya.bluebank.deposit.data.domain.command

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositProductType
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.PeriodUnit
import com.socoolheeya.bluebank.deposit.data.domain.entity.Deposit
import java.math.BigDecimal
import java.time.LocalDate

sealed interface DepositCommand {

    data class Create(
        val depositNumber: String,
        val customerId: Long,
        val accountId: Long,
        val productType: DepositProductType,
        val principalAmount: BigDecimal,
        val baseRate: BigDecimal,
        val bonusRate: BigDecimal = BigDecimal.ZERO,
        val contractPeriod: Int,
        val periodUnit: PeriodUnit,
        val startDate: LocalDate,
        val maturityDate: LocalDate,
        val monthlyPayment: BigDecimal? = null,
        val minMonthlyPayment: BigDecimal? = null,
        val maxMonthlyPayment: BigDecimal? = null,
        val autoTransferEnabled: Boolean = false,
        val autoTransferDay: Int? = null,
        val autoTransferAmount: BigDecimal? = null,
        val autoRenewalEnabled: Boolean = false,
        val initialWeeklyAmount: BigDecimal? = null,
        val childId: Long? = null,
        val parentIds: String? = null,
        val maxBalance: BigDecimal? = null,
        val spareChangeEnabled: Boolean = false,
        val aiSavingsEnabled: Boolean = false,
        val interestPaymentDay: Int? = null,
        val isTaxFree: Boolean = false
    ) : DepositCommand {
        fun toEntity(): Deposit {
            return Deposit(
                depositNumber = depositNumber,
                customerId = customerId,
                accountId = accountId,
                productType = productType,
                principalAmount = principalAmount,
                currentBalance = if (productType == DepositProductType.FIXED_DEPOSIT) principalAmount else BigDecimal.ZERO,
                baseRate = baseRate,
                bonusRate = bonusRate,
                appliedRate = baseRate + bonusRate,
                contractPeriod = contractPeriod,
                periodUnit = periodUnit,
                startDate = startDate,
                maturityDate = maturityDate,
                monthlyPayment = monthlyPayment,
                minMonthlyPayment = minMonthlyPayment,
                maxMonthlyPayment = maxMonthlyPayment,
                autoTransferEnabled = autoTransferEnabled,
                autoTransferDay = autoTransferDay,
                autoTransferAmount = autoTransferAmount,
                status = DepositStatus.PENDING,
                autoRenewalEnabled = autoRenewalEnabled,
                initialWeeklyAmount = initialWeeklyAmount,
                childId = childId,
                parentIds = parentIds,
                maxBalance = maxBalance,
                spareChangeEnabled = spareChangeEnabled,
                aiSavingsEnabled = aiSavingsEnabled,
                interestPaymentDay = interestPaymentDay,
                isTaxFree = isTaxFree
            )
        }
    }

    data class Activate(val depositId: Long) : DepositCommand

    data class MakeDeposit(val depositId: Long, val amount: BigDecimal, val description: String? = null) : DepositCommand

    data class EarlyWithdraw(val depositId: Long, val amount: BigDecimal) : DepositCommand

    data class Terminate(val depositId: Long) : DepositCommand

    data class Mature(val depositId: Long, val totalInterest: BigDecimal) : DepositCommand

    data class PayInterest(val depositId: Long, val interest: BigDecimal) : DepositCommand

    data class UpdateBonusRate(val depositId: Long, val bonusRate: BigDecimal) : DepositCommand
}
