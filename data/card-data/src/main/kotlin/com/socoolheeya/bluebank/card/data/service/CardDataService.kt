package com.socoolheeya.bluebank.card.data.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardDataService(
    private val cardRepository: CardRepository
) {

    @Transactional
    fun createCard(command: CardCommand.Create): CardResult {
        // 1. 카드 번호 중복 확인
        if (cardRepository.existsByCardNumber(command.cardNumber)) {
            throw IllegalArgumentException("이미 존재하는 카드 번호입니다")
        }

        // 2. Entity 생성 및 저장
        val card = command.toEntity()
        val savedCard = cardRepository.save(card)

        return CardResult.from(savedCard)
    }

    @Transactional
    fun activateCard(command: CardCommand.Activate): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.activate()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun suspendCard(command: CardCommand.Suspend): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.suspend(command.reason)
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun terminateCard(command: CardCommand.Terminate): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        card.terminate()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun toggleCardUsage(command: CardCommand.ToggleUsage): CardResult {
        val card = cardRepository.findById(command.cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: ${command.cardId}") }

        if (command.enabled) card.enableUsage() else card.disableUsage()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional
    fun reportLostCard(cardId: Long): CardResult {
        val card = cardRepository.findById(cardId)
            .orElseThrow { NoSuchElementException("카드를 찾을 수 없습니다: $cardId") }

        card.reportLost()
        val updatedCard = cardRepository.save(card)

        return CardResult.from(updatedCard)
    }

    @Transactional(readOnly = true)
    fun getCard(cardId: Long): CardResult? {
        return cardRepository.findById(cardId)
            .map { CardResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getCardsByCustomerId(customerId: Long): List<CardResult> {
        return cardRepository.findByCustomerId(customerId)
            .map { CardResult.from(it) }
    }

    @Transactional(readOnly = true)
    fun getActiveCardsByCustomerId(customerId: Long): List<CardResult> {
        return cardRepository.findByCustomerIdAndStatus(customerId, CardStatus.ACTIVE)
            .map { CardResult.from(it) }
    }

    @Transactional
    fun resetDailyLimits() {
        val activeCards = cardRepository.findByStatus(CardStatus.ACTIVE)
        activeCards.forEach { it.resetDailyUsage() }
        cardRepository.saveAll(activeCards)
    }

    @Transactional
    fun resetMonthlyLimits() {
        val activeCards = cardRepository.findByStatus(CardStatus.ACTIVE)
        activeCards.forEach { it.resetMonthlyUsage() }
        cardRepository.saveAll(activeCards)
    }
}