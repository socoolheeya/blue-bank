package com.socoolheeya.bluebank.card.data.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.command.TransactionCommand
import com.socoolheeya.bluebank.card.data.domain.entity.CashbackHistory
import com.socoolheeya.bluebank.card.data.domain.result.TransactionResult
import com.socoolheeya.bluebank.card.data.repository.CardBenefitRepository
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import com.socoolheeya.bluebank.card.data.repository.CardTransactionRepository
import com.socoolheeya.bluebank.card.data.repository.CashbackHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.random.Random

@Service
class CardTransactionDataService(
    private val cardTransactionRepository: CardTransactionRepository,
    private val cardRepository: CardRepository,
    private val cashbackHistoryRepository: CashbackHistoryRepository,
    private val cardBenefitRepository: CardBenefitRepository
) {

    @Transactional
    fun createTransaction(command: TransactionCommand.Create): TransactionResult {
        // 1. 카드 조회
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        // 2. 카드 상태 및 한도 검증
        require(card.status == CardStatus.ACTIVE) { "활성화된 카드만 사용 가능합니다" }
        require(card.isEnabled) { "카드 사용이 비활성화되어 있습니다" }
        require(card.validateDailyLimit(command.amount)) { "일 한도를 초과합니다" }
        require(card.validateMonthlyLimit(command.amount)) { "월 한도를 초과합니다" }

        // 3. 거래 생성
        val transaction = command.toEntity()
        val savedTransaction = cardTransactionRepository.save(transaction)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun approveTransaction(transactionId: Long, approvalNumber: String): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("거래를 찾을 수 없습니다: $transactionId") }

        // 승인 처리
        transaction.approve(approvalNumber)
        val savedTransaction = cardTransactionRepository.save(transaction)

        // 카드 사용액 누적
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        card.addUsage(transaction.amount)
        cardRepository.save(card)

        // 캐시백 계산 및 적립
        calculateAndEarnCashback(savedTransaction)

        return TransactionResult.from(savedTransaction)
    }

    @Transactional
    fun cancelTransaction(transactionId: Long): TransactionResult {
        val transaction = cardTransactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("거래를 찾을 수 없습니다: $transactionId") }

        transaction.cancel()

        // 카드 사용액 차감
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        card.addUsage(transaction.amount.negate())
        cardRepository.save(card)

        // 캐시백 취소
        cancelCashback(transactionId)

        return TransactionResult.from(cardTransactionRepository.save(transaction))
    }

    @Transactional(readOnly = true)
    fun getTransactionsByCardId(
        cardId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionResult> {
        return cardTransactionRepository
            .findByCardIdAndTransactionDateBetween(
                cardId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            )
            .map { TransactionResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getTransactionsByCustomerId(customerId: Long): List<TransactionResult> {
        return cardTransactionRepository.findByCustomerId(customerId)
            .map { TransactionResult.from(it) }
    }

    private fun calculateAndEarnCashback(transaction: com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction) {
        val card = cardRepository.findById(transaction.cardId).orElseThrow()
        val benefits = cardBenefitRepository
            .findByProductTypeAndStatus(card.productType, BenefitStatus.ACTIVE)

        benefits.filter { it.benefitType == BenefitType.CASHBACK && it.isActive() }
            .forEach { benefit ->
                val cashbackAmount = calculateCashbackAmount(transaction, benefit)
                if (cashbackAmount > BigDecimal.ZERO) {
                    val cashback = CashbackHistory(
                        cardId = card.id!!,
                        customerId = card.customerId,
                        transactionId = transaction.id!!,
                        cashbackAmount = cashbackAmount,
                        cashbackRate = benefit.cashbackRate ?: BigDecimal.ZERO,
                        transactionAmount = transaction.amount,
                        cashbackType = determineCashbackType(benefit),
                        earnedDate = LocalDate.now(),
                        paymentDate = LocalDate.now().withDayOfMonth(10).plusMonths(1)
                    )
                    cashbackHistoryRepository.save(cashback)
                }
            }
    }

    private fun calculateCashbackAmount(
        transaction: com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction,
        benefit: com.socoolheeya.bluebank.card.data.domain.entity.CardBenefit
    ): BigDecimal {
        // 혜택 조건 검증
        if (benefit.minTransactionAmount != null &&
            transaction.amount < benefit.minTransactionAmount!!
        ) {
            return BigDecimal.ZERO
        }

        // 랜덤 캐시백 처리 (모임체크카드)
        if (benefit.benefitName.contains("랜덤")) {
            return if (transaction.amount >= BigDecimal("50000")) {
                // 일 1회 제한 체크
                val todayCount = cashbackHistoryRepository.countByCardIdAndEarnedDate(
                    transaction.cardId,
                    LocalDate.now()
                )
                if (todayCount > 0) {
                    BigDecimal.ZERO
                } else {
                    if (Random.nextDouble() < 0.1) BigDecimal("3000") else BigDecimal("300")
                }
            } else {
                BigDecimal.ZERO
            }
        }

        // 일반 캐시백 계산
        val cashbackRate = benefit.cashbackRate ?: return BigDecimal.ZERO
        var cashbackAmount = transaction.amount
            .multiply(cashbackRate)
            .divide(BigDecimal("100"), 0, RoundingMode.DOWN)

        // 월 최대 캐시백 체크
        if (benefit.maxCashbackPerMonth != null) {
            val monthlyTotal = cashbackHistoryRepository.findByCardId(transaction.cardId)
                .filter { it.earnedDate.month == LocalDate.now().month }
                .sumOf { it.cashbackAmount }

            val remaining = benefit.maxCashbackPerMonth!! - monthlyTotal
            if (remaining <= BigDecimal.ZERO) {
                return BigDecimal.ZERO
            }
            cashbackAmount = cashbackAmount.min(remaining)
        }

        return cashbackAmount
    }

    private fun determineCashbackType(benefit: com.socoolheeya.bluebank.card.data.domain.entity.CardBenefit): CashbackType {
        return when {
            benefit.benefitName.contains("랜덤") -> CashbackType.RANDOM
            benefit.benefitName.contains("보너스") -> CashbackType.BONUS
            else -> CashbackType.STANDARD
        }
    }

    private fun cancelCashback(transactionId: Long) {
        cashbackHistoryRepository.findByTransactionId(transactionId)
            .forEach {
                it.cancel()
                cashbackHistoryRepository.save(it)
            }
    }
}