package com.socoolheeya.bluebank.batch.eod

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.account.data.repository.LedgerEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class LedgerCloseService(
    private val balanceRepository: BalanceRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val ledgerCloseRepository: LedgerCloseRepository
) {
    @Transactional
    fun close(accountId: Long, businessDate: LocalDate): LedgerClose? {
        if (ledgerCloseRepository.existsByBusinessDateAndAccountId(businessDate, accountId)) return null
        val balance = balanceRepository.findByAccountId(accountId) ?: return null
        val entries = ledgerEntryRepository.findByAccountIdAndOccurredAtBetween(
            accountId,
            businessDate.atStartOfDay(),
            businessDate.plusDays(1).atStartOfDay()
        )
        val netMovement = entries.fold(BigDecimal.ZERO) { total, entry ->
            val signed = when {
                entry.type == AccountEnums.EntryType.WITHDRAWAL -> entry.amount.abs().negate()
                entry.type == AccountEnums.EntryType.TRANSFER -> entry.amount
                else -> entry.amount.abs()
            }
            total + signed
        }
        val deposits = entries.filter { it.amount.signum() > 0 }.fold(BigDecimal.ZERO) { a, e -> a + e.amount }
        val withdrawals = entries.filter { it.amount.signum() < 0 || it.type == AccountEnums.EntryType.WITHDRAWAL }
            .fold(BigDecimal.ZERO) { a, e -> a + e.amount.abs() }
        val opening = balance.ledgerBalance - netMovement
        val expectedClosing = opening + deposits - withdrawals
        val reconciled = expectedClosing.compareTo(balance.ledgerBalance) == 0
        return ledgerCloseRepository.save(
            LedgerClose(
                businessDate = businessDate,
                accountId = accountId,
                openingBalance = opening,
                deposits = deposits,
                withdrawals = withdrawals,
                closingBalance = balance.ledgerBalance,
                reconciled = reconciled,
                errorMessage = if (reconciled) null else "Ledger movement does not match closing balance"
            )
        )
    }
}
