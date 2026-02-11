package com.socoolheeya.bluebank.deposit.data.service

import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.command.DepositCommand
import com.socoolheeya.bluebank.deposit.data.domain.entity.DepositTransaction
import com.socoolheeya.bluebank.deposit.data.domain.entity.InterestPayment
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositResult
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositTransactionResult
import com.socoolheeya.bluebank.deposit.data.domain.result.InterestPaymentResult
import com.socoolheeya.bluebank.deposit.data.repository.DepositRepository
import com.socoolheeya.bluebank.deposit.data.repository.DepositTransactionRepository
import com.socoolheeya.bluebank.deposit.data.repository.InterestPaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class DepositDataService(
    private val depositRepository: DepositRepository,
    private val depositTransactionRepository: DepositTransactionRepository,
    private val interestPaymentRepository: InterestPaymentRepository
) {

    @Transactional
    fun createDeposit(command: DepositCommand.Create): DepositResult {
        val deposit = command.toEntity()
        val savedDeposit = depositRepository.save(deposit)
        return DepositResult.from(savedDeposit)
    }

    @Transactional
    fun activateDeposit(depositId: Long): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.activate()
        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun deposit(depositId: Long, amount: BigDecimal, description: String?): DepositTransactionResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.deposit(amount)
        depositRepository.save(deposit)

        val transaction = DepositTransaction(
            depositId = depositId,
            customerId = deposit.customerId,
            transactionType = DepositTransactionType.DEPOSIT,
            amount = amount,
            balanceAfter = deposit.currentBalance,
            description = description
        )
        val savedTransaction = depositTransactionRepository.save(transaction)

        return DepositTransactionResult.from(savedTransaction)
    }

    @Transactional
    fun earlyWithdraw(depositId: Long, amount: BigDecimal): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.earlyWithdraw(amount)

        // 거래 내역 생성
        depositTransactionRepository.save(
            DepositTransaction(
                depositId = depositId,
                customerId = deposit.customerId,
                transactionType = DepositTransactionType.EARLY_WITHDRAWAL,
                amount = amount,
                balanceAfter = deposit.currentBalance
            )
        )

        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun matureDeposit(depositId: Long, totalInterest: BigDecimal): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.mature(totalInterest)
        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun terminateDeposit(depositId: Long): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.terminate()

        // 거래 내역 생성
        depositTransactionRepository.save(
            DepositTransaction(
                depositId = depositId,
                customerId = deposit.customerId,
                transactionType = DepositTransactionType.TERMINATION,
                amount = deposit.currentBalance,
                balanceAfter = BigDecimal.ZERO
            )
        )

        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional
    fun payInterest(depositId: Long, interest: BigDecimal): InterestPaymentResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }

        val tax = calculateTax(interest, deposit.isTaxFree)
        val netInterest = interest - tax

        deposit.payInterest(netInterest)
        depositRepository.save(deposit)

        // 거래 내역 생성
        depositTransactionRepository.save(
            DepositTransaction(
                depositId = depositId,
                customerId = deposit.customerId,
                transactionType = DepositTransactionType.INTEREST_PAYMENT,
                amount = netInterest,
                balanceAfter = deposit.currentBalance
            )
        )

        val interestPayment = InterestPayment(
            depositId = depositId,
            customerId = deposit.customerId,
            interestAmount = interest,
            appliedRate = deposit.appliedRate,
            calculationPeriodStart = deposit.lastInterestPaymentDate ?: deposit.startDate,
            calculationPeriodEnd = LocalDate.now(),
            principalBalance = deposit.currentBalance,
            taxAmount = tax,
            netInterest = netInterest,
            paymentDate = LocalDate.now()
        )
        val saved = interestPaymentRepository.save(interestPayment)

        return InterestPaymentResult.from(saved)
    }

    @Transactional
    fun updateBonusRate(depositId: Long, bonusRate: BigDecimal): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        deposit.updateBonusRate(bonusRate)
        return DepositResult.from(depositRepository.save(deposit))
    }

    @Transactional(readOnly = true)
    fun getDeposit(depositId: Long): DepositResult {
        val deposit = depositRepository.findById(depositId).orElseThrow {
            IllegalArgumentException("예금/적금을 찾을 수 없습니다: $depositId")
        }
        return DepositResult.from(deposit)
    }

    @Transactional(readOnly = true)
    fun getDepositByNumber(depositNumber: String): DepositResult? {
        val deposit = depositRepository.findByDepositNumber(depositNumber)
        return deposit?.let { DepositResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getDepositsByCustomer(customerId: Long): List<DepositResult> {
        return depositRepository.findByCustomerId(customerId)
            .map { DepositResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactions(depositId: Long): List<DepositTransactionResult> {
        return depositTransactionRepository.findByDepositIdOrderByTransactionDateDesc(depositId)
            .map { DepositTransactionResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getInterestPayments(depositId: Long): List<InterestPaymentResult> {
        return interestPaymentRepository.findByDepositIdOrderByPaymentDateDesc(depositId)
            .map { InterestPaymentResult.from(it) }
    }

    private fun calculateTax(interest: BigDecimal, isTaxFree: Boolean): BigDecimal {
        if (isTaxFree) return BigDecimal.ZERO
        // 이자소득세 15.4% (소득세 14% + 지방소득세 1.4%)
        return interest.multiply(BigDecimal("0.154")).setScale(0, RoundingMode.DOWN)
    }
}