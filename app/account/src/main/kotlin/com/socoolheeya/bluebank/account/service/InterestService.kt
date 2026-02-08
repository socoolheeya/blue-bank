package com.socoolheeya.bluebank.account.service

import com.socoolheeya.bluebank.account.data.domain.InterestPayment
import com.socoolheeya.bluebank.account.data.service.InterestDataService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class InterestService(
    private val interestDataService: InterestDataService
) {

    /**
     * 월별 이자 계산 및 지급
     */
    fun calculateAndPayMonthlyInterest(accountId: Long, yearMonth: YearMonth): InterestPayment? {
        return interestDataService.calculateAndPayMonthlyInterest(accountId, yearMonth)
    }

    /**
     * 일평균 잔액 계산
     */
    fun calculateAverageDailyBalance(accountId: Long, startDate: LocalDate, endDate: LocalDate): BigDecimal {
        return interestDataService.calculateAverageDailyBalance(accountId, startDate, endDate)
    }

    /**
     * 계좌의 총 이자 수령액 조회
     */
    fun getTotalInterestReceived(accountId: Long): BigDecimal {
        return interestDataService.getTotalInterestReceived(accountId)
    }

    /**
     * 계좌의 이자 지급 내역 조회
     */
    fun getInterestPaymentHistory(accountId: Long): List<InterestPayment> {
        return interestDataService.getInterestPaymentHistory(accountId)
    }

    /**
     * 특정 기간의 이자 지급 내역 조회
     */
    fun getInterestPaymentHistoryByPeriod(
        accountId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<InterestPayment> {
        return interestDataService.getInterestPaymentHistoryByPeriod(accountId, startDate, endDate)
    }

    /**
     * 예상 이자 계산
     */
    fun calculateExpectedInterest(accountId: Long, months: Int = 1): BigDecimal {
        return interestDataService.calculateExpectedInterest(accountId, months)
    }
}
