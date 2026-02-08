package com.socoolheeya.bluebank.account.data.service

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.InterestPayment
import com.socoolheeya.bluebank.account.data.repository.AccountRepository
import com.socoolheeya.bluebank.account.data.repository.BalanceRepository
import com.socoolheeya.bluebank.account.data.repository.InterestPaymentRepository
import com.socoolheeya.bluebank.account.data.repository.LedgerEntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Service
class InterestDataService(
    private val accountRepository: AccountRepository,
    private val balanceRepository: BalanceRepository,
    private val interestPaymentRepository: InterestPaymentRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val balanceDataService: BalanceDataService
) {

    /**
     * 월별 이자 계산 및 지급 (트랜잭션)
     */
    @Transactional
    fun calculateAndPayMonthlyInterest(accountId: Long, yearMonth: YearMonth): InterestPayment? {
        val account = accountRepository.findById(accountId).orElseThrow {
            NoSuchElementException("계좌를 찾을 수 없습니다: $accountId")
        }

        // 이자 지급 대상 계좌인지 확인
        if (account.interestRate <= BigDecimal.ZERO) {
            return null // 이율이 0이면 이자 지급하지 않음
        }

        if (account.status != AccountEnums.AccountStatus.ACTIVE) {
            return null // 활성 계좌가 아니면 이자 지급하지 않음
        }

        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 일평균 잔액 계산
        val averageBalance = calculateAverageDailyBalance(accountId, startDate, endDate)

        if (averageBalance <= BigDecimal.ZERO) {
            return null // 평균 잔액이 0 이하면 이자 없음
        }

        // 이자 계산: (평균잔액 × 연이율 × 일수) / 365
        val daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1))
        val interestAmount = averageBalance
            .multiply(account.interestRate)
            .multiply(BigDecimal(daysInPeriod))
            .divide(BigDecimal(365), 0, RoundingMode.DOWN) // 원 단위 절사

        if (interestAmount <= BigDecimal.ZERO) {
            return null // 이자가 0원 이하면 지급하지 않음
        }

        // 잔액에 이자 추가
        balanceDataService.payInterest(accountId, interestAmount)

        // 이자 지급 내역 기록
        val interestPayment = InterestPayment(
            accountId = accountId,
            amount = interestAmount,
            interestRate = account.interestRate,
            calculationPeriodStart = startDate,
            calculationPeriodEnd = endDate,
            averageBalance = averageBalance,
            paidAt = LocalDateTime.now()
        )
        return interestPaymentRepository.save(interestPayment)
    }

    /**
     * 일평균 잔액 계산
     */
    @Transactional(readOnly = true)
    fun calculateAverageDailyBalance(accountId: Long, startDate: LocalDate, endDate: LocalDate): BigDecimal {
        var currentDate = startDate
        var totalBalance = BigDecimal.ZERO
        var dayCount = 0

        while (!currentDate.isAfter(endDate)) {
            val dayEndBalance = getDayEndBalance(accountId, currentDate)
            totalBalance = totalBalance.add(dayEndBalance)
            dayCount++
            currentDate = currentDate.plusDays(1)
        }

        return if (dayCount > 0) {
            totalBalance.divide(BigDecimal(dayCount), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * 특정일 종료 시점의 잔액 조회
     */
    @Transactional(readOnly = true)
    fun getDayEndBalance(accountId: Long, date: LocalDate): BigDecimal {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.plusDays(1).atStartOfDay()

        // 해당일의 마지막 거래 조회
        val lastEntry = ledgerEntryRepository.findByAccountIdAndOccurredAtBetween(
            accountId = accountId,
            start = startOfDay,
            end = endOfDay,
            pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "occurredAt"))
        ).content.firstOrNull()

        return lastEntry?.balanceAfter ?: run {
            // 거래가 없으면 현재 잔액 또는 0
            if (date.isAfter(LocalDate.now())) {
                BigDecimal.ZERO
            } else {
                balanceRepository.findByAccountId(accountId)?.ledgerBalance ?: BigDecimal.ZERO
            }
        }
    }

    /**
     * 계좌의 총 이자 수령액 조회
     */
    @Transactional(readOnly = true)
    fun getTotalInterestReceived(accountId: Long): BigDecimal {
        return interestPaymentRepository.getTotalInterestByAccountId(accountId) ?: BigDecimal.ZERO
    }

    /**
     * 계좌의 이자 지급 내역 조회
     */
    @Transactional(readOnly = true)
    fun getInterestPaymentHistory(accountId: Long): List<InterestPayment> {
        return interestPaymentRepository.findByAccountIdOrderByPaidAtDesc(accountId)
    }

    /**
     * 특정 기간의 이자 지급 내역 조회
     */
    @Transactional(readOnly = true)
    fun getInterestPaymentHistoryByPeriod(
        accountId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<InterestPayment> {
        return interestPaymentRepository.findByAccountIdAndCalculationPeriodStartBetween(
            accountId = accountId,
            start = startDate,
            end = endDate
        )
    }

    /**
     * 예상 이자 계산 (실제 지급하지 않음)
     */
    @Transactional(readOnly = true)
    fun calculateExpectedInterest(accountId: Long, months: Int = 1): BigDecimal {
        val account = accountRepository.findById(accountId).orElseThrow {
            NoSuchElementException("계좌를 찾을 수 없습니다: $accountId")
        }

        if (account.interestRate <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        val balance = balanceRepository.findByAccountId(accountId)
            ?: return BigDecimal.ZERO

        // 예상 이자 = 현재잔액 × 연이율 × (개월수 × 30일) / 365
        val days = months * 30
        return balance.availableBalance
            .multiply(account.interestRate)
            .multiply(BigDecimal(days))
            .divide(BigDecimal(365), 0, RoundingMode.DOWN)
    }
}