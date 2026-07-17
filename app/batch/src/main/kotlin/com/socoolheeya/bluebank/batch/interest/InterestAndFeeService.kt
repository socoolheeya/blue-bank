package com.socoolheeya.bluebank.batch.interest

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.LedgerEntry
import com.socoolheeya.bluebank.account.data.repository.AccountRepository
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.account.data.repository.LedgerEntryRepository
import com.socoolheeya.bluebank.batch.config.EodProperties
import com.socoolheeya.bluebank.batch.eod.EodAccountingEntry
import com.socoolheeya.bluebank.batch.eod.EodAccountingEntryRepository
import com.socoolheeya.bluebank.batch.support.IdempotencyKeys
import com.socoolheeya.bluebank.deposit.data.repository.DepositRepository
import com.socoolheeya.bluebank.loan.data.repository.LoanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class InterestAndFeeService(
    private val calculator: DailyAmountCalculator,
    private val properties: EodProperties,
    private val accountingRepository: EodAccountingEntryRepository,
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val depositRepository: DepositRepository,
    private val loanRepository: LoanRepository
) {
    @Transactional
    fun processAccount(accountId: Long, businessDate: LocalDate) {
        val account = accountRepository.findById(accountId).orElse(null) ?: return
        if (account.status != AccountEnums.AccountStatus.ACTIVE) return
        val balance = balanceRepository.findByAccountId(accountId) ?: return
        post(
            businessDate, "ACCOUNT", accountId, "INTEREST", balance.ledgerBalance, account.interestRate,
            calculator.interest(balance.ledgerBalance, account.interestRate)
        ) { interest ->
            if (interest.signum() > 0) {
                balance.deposit(interest)
                balance.interestAccumulated += interest
                ledgerEntryRepository.save(
                    LedgerEntry(
                        accountId = accountId,
                        type = AccountEnums.EntryType.INTEREST,
                        amount = interest,
                        balanceAfter = balance.ledgerBalance,
                        description = "Daily interest $businessDate",
                        transactionId = "EOD-INTEREST-$businessDate-ACCOUNT-$accountId",
                        occurredAt = businessDate.atTime(1, 0)
                    )
                )
            }
        }
        val fee = properties.accountMaintenanceFee.setScale(0)
        post(businessDate, "ACCOUNT", accountId, "FEE", balance.ledgerBalance, BigDecimal.ZERO, fee) { amount ->
            if (amount.signum() > 0 && balance.availableBalance >= amount) balance.withdraw(amount)
        }
        balanceRepository.save(balance)
    }

    @Transactional
    fun processDeposit(depositId: Long, businessDate: LocalDate) {
        val deposit = depositRepository.findById(depositId).orElse(null) ?: return
        if (deposit.status.name != "ACTIVE") return
        val interest = calculator.interest(deposit.currentBalance, deposit.appliedRate.movePointLeft(2))
        post(businessDate, "DEPOSIT", depositId, "INTEREST", deposit.currentBalance, deposit.appliedRate, interest) {
            if (it.signum() > 0) deposit.payInterest(it)
        }
        depositRepository.save(deposit)
    }

    @Transactional
    fun processLoan(loanId: Long, businessDate: LocalDate) {
        val loan = loanRepository.findById(loanId).orElse(null) ?: return
        if (loan.status.name != "ACTIVE") return
        val interest = calculator.interest(loan.outstandingBalance, loan.interestRate.movePointLeft(2))
        post(businessDate, "LOAN", loanId, "INTEREST", loan.outstandingBalance, loan.interestRate, interest) {}
    }

    private fun post(
        date: LocalDate,
        referenceType: String,
        referenceId: Long,
        entryType: String,
        base: BigDecimal,
        rate: BigDecimal,
        amount: BigDecimal,
        apply: (BigDecimal) -> Unit
    ) {
        val key = IdempotencyKeys.accounting(date, referenceType, referenceId, entryType)
        if (accountingRepository.existsByIdempotencyKey(key)) return
        apply(amount)
        accountingRepository.save(
            EodAccountingEntry(
                businessDate = date,
                referenceType = referenceType,
                referenceId = referenceId,
                entryType = entryType,
                baseAmount = base,
                rate = rate,
                amount = amount,
                idempotencyKey = key
            )
        )
    }
}
