package com.socoolheeya.bluebank.card.testing

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.data.repository.CardApplicationRepository
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import com.socoolheeya.bluebank.card.data.service.CardApplicationDataService
import com.socoolheeya.bluebank.card.data.service.CardDataService
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
private fun <T> unusedRepository(type: Class<T>): T = Proxy.newProxyInstance(
    type.classLoader, arrayOf(type)
) { _, method, _ -> error("Unexpected repository call: ${method.name}") } as T

class FakeCardDataService : CardDataService(unusedRepository(CardRepository::class.java)) {
    val cards = linkedMapOf<Long, CardResult>()
    val created = mutableListOf<CardCommand.Create>()
    val commands = mutableListOf<CardCommand>()
    private var nextId = 100L

    override fun getCard(cardId: Long) = cards[cardId]
    override fun getCardsByCustomerId(customerId: Long) = cards.values.filter { it.customerId == customerId }
    override fun getActiveCardsByCustomerId(customerId: Long) = cards.values.filter { it.customerId == customerId && it.status == CardStatus.ACTIVE }

    override fun createCard(command: CardCommand.Create): CardResult {
        created += command
        val result = CardResult.from(command.toEntity()).copy(id = nextId++)
        cards[result.id!!] = result
        return result
    }

    override fun activateCard(command: CardCommand.Activate) = update(command.cardId, command) { it.copy(status = CardStatus.ACTIVE) }
    override fun toggleCardUsage(command: CardCommand.ToggleUsage) = update(command.cardId, command) { it.copy(isEnabled = command.enabled) }
    override fun terminateCard(command: CardCommand.Terminate) = update(command.cardId, command) { it.copy(status = CardStatus.TERMINATED) }
    override fun reportLostCard(cardId: Long) = update(cardId, null) { it.copy(status = CardStatus.LOST) }

    private fun update(id: Long, command: CardCommand?, change: (CardResult) -> CardResult): CardResult {
        command?.let(commands::add)
        val updated = change(cards[id] ?: throw NoSuchElementException("카드를 찾을 수 없습니다: $id"))
        cards[id] = updated
        return updated
    }
}

class FakeCardApplicationDataService : CardApplicationDataService(
    unusedRepository(CardApplicationRepository::class.java), unusedRepository(CardRepository::class.java)
) {
    val applications = linkedMapOf<Long, CardApplicationResult>()
    val submitted = mutableListOf<CardApplicationCommand.Submit>()
    val issued = mutableListOf<Pair<Long, Long>>()
    private var nextId = 1L

    override fun submitApplication(command: CardApplicationCommand.Submit): CardApplicationResult {
        submitted += command
        val result = CardApplicationResult(
            id = nextId++, customerId = command.customerId, accountId = command.accountId,
            cardType = command.cardType, productType = command.productType, applicantName = command.applicantName,
            phoneNumber = command.phoneNumber, email = command.email, designCode = command.designCode,
            customText = command.customText, requestTransitCard = command.requestTransitCard,
            status = CardApplicationStatus.SUBMITTED, approvedCreditLimit = null, rejectionReason = null,
            cardId = null, appliedAt = LocalDateTime.now(), reviewedAt = null
        )
        applications[result.id!!] = result
        return result
    }

    override fun getApplication(applicationId: Long) = applications[applicationId]
    override fun getApplicationsByCustomerId(customerId: Long) = applications.values.filter { it.customerId == customerId }
    override fun markAsIssued(applicationId: Long, cardId: Long): CardApplicationResult {
        issued += applicationId to cardId
        val updated = applications[applicationId]!!.copy(status = CardApplicationStatus.ISSUED, cardId = cardId)
        applications[applicationId] = updated
        return updated
    }
}
