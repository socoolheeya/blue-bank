package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.testing.FakeCardDataService
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private fun card(id: Long, customer: Long = 7L, status: CardStatus = CardStatus.ISSUED) = CardResult(
    id, "5234000011112222", "5234-****-****-2222", customer, 9L, CardType.DEBIT,
    CardProductType.FRIENDS_CHECK, "KIM", LocalDate.now(), LocalDate.now().plusYears(5), status,
    BigDecimal("5000000"), BigDecimal("20000000"), BigDecimal.ZERO, BigDecimal.ZERO, "BLUE", null,
    false, true, true, null, null, BigDecimal.ZERO, null, null, null, LocalDateTime.now(), LocalDateTime.now()
)

val cardServiceScenarios by testSuite("Card service scenarios") {
    test("lookup and customer lists map stored cards") {
        val data = FakeCardDataService().apply { cards[1] = card(1); cards[2] = card(2, status = CardStatus.ACTIVE); cards[3] = card(3, 8) }
        val service = CardService(data)
        check(service.getCard(1).id == 1L)
        check(service.getCardsByCustomerId(7).map { it.id } == listOf(1L, 2L))
        check(service.getActiveCardsByCustomerId(7).map { it.id } == listOf(2L))
        check(runCatching { service.getCard(99) }.exceptionOrNull() is NoSuchElementException)
    }

    test("owner can activate toggle lose and terminate a card") {
        val data = FakeCardDataService().apply { cards[1] = card(1) }
        val service = CardService(data)
        check(service.activateCard(1, 7).status == CardStatus.ACTIVE.description)
        check(!service.toggleCardUsage(1, 7, false).isEnabled)
        check(service.reportLostCard(1, 7).status == CardStatus.LOST.description)
        check(service.terminateCard(1, 7).status == CardStatus.TERMINATED.description)
    }

    test("mutations reject a different owner") {
        val service = CardService(FakeCardDataService().apply { cards[1] = card(1) })
        listOf<() -> Unit>(
            { service.activateCard(1, 8) }, { service.toggleCardUsage(1, 8, false) },
            { service.reportLostCard(1, 8) }, { service.terminateCard(1, 8) }
        ).forEach { check(runCatching(it).exceptionOrNull() is IllegalArgumentException) }
    }
}
