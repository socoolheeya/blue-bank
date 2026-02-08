package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.service.CardDataService
import com.socoolheeya.bluebank.card.dto.CardDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CardService(
    private val cardDataService: CardDataService
) {

    fun getCard(cardId: Long): CardDto.Response {
        val result = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $cardId")

        return CardDto.Response.from(result)
    }

    fun getCardsByCustomerId(customerId: Long): List<CardDto.Response> {
        val results = cardDataService.getCardsByCustomerId(customerId)
        return results.map { CardDto.Response.from(it) }
    }

    fun getActiveCardsByCustomerId(customerId: Long): List<CardDto.Response> {
        val results = cardDataService.getActiveCardsByCustomerId(customerId)
        return results.map { CardDto.Response.from(it) }
    }

    fun activateCard(cardId: Long, customerId: Long): CardDto.Response {
        // 본인 확인
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $cardId")

        require(card.customerId == customerId) { "본인의 카드만 활성화 가능합니다" }

        val command = CardCommand.Activate(cardId)
        val result = cardDataService.activateCard(command)
        return CardDto.Response.from(result)
    }

    fun toggleCardUsage(cardId: Long, customerId: Long, enabled: Boolean): CardDto.Response {
        // 본인 확인
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $cardId")

        require(card.customerId == customerId) { "본인의 카드만 사용 설정 가능합니다" }

        val command = CardCommand.ToggleUsage(cardId, enabled)
        val result = cardDataService.toggleCardUsage(command)
        return CardDto.Response.from(result)
    }

    fun reportLostCard(cardId: Long, customerId: Long): CardDto.Response {
        // 본인 확인
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $cardId")

        require(card.customerId == customerId) { "본인의 카드만 분실 신고 가능합니다" }

        val result = cardDataService.reportLostCard(cardId)
        return CardDto.Response.from(result)
    }

    fun terminateCard(cardId: Long, customerId: Long): CardDto.Response {
        // 본인 확인
        val card = cardDataService.getCard(cardId)
            ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $cardId")

        require(card.customerId == customerId) { "본인의 카드만 해지 가능합니다" }

        val command = CardCommand.Terminate(cardId)
        val result = cardDataService.terminateCard(command)
        return CardDto.Response.from(result)
    }
}