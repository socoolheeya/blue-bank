package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.dto.CardDto
import com.socoolheeya.bluebank.card.testing.FakeCardDataService
import com.socoolheeya.bluebank.card.testing.Scenario
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

private class CardScenarioContext {
    val data = FakeCardDataService()
    val service = CardService(data)
    var response: CardDto.Response? = null
    var customerCards: List<CardDto.Response> = emptyList()
    var activeCards: List<CardDto.Response> = emptyList()
    var activationResponse: CardDto.Response? = null
    var toggleResponse: CardDto.Response? = null
    var lostResponse: CardDto.Response? = null
    var terminationResponse: CardDto.Response? = null
    var failure: Throwable? = null
    var mutationFailures: List<Throwable?> = emptyList()
}

val cardServiceScenarios by testSuite("Card service scenarios") {
    testFixture { CardScenarioContext() } asContextForEach {
        Scenario("lookup and customer lists map stored cards") {
            Given("issued active and differently owned cards") {
                data.cards[1] = card(1)
                data.cards[2] = card(2, status = CardStatus.ACTIVE)
                data.cards[3] = card(3, 8)
            }
            When("the card customer lists and a missing card are looked up") {
                response = service.getCard(1)
                customerCards = service.getCardsByCustomerId(7)
                activeCards = service.getActiveCardsByCustomerId(7)
                failure = runCatching { service.getCard(99) }.exceptionOrNull()
            }
            Then("the exact lookup lists are returned and the missing card fails") {
                check(response!!.id == 1L)
                check(customerCards.map { it.id } == listOf(1L, 2L))
                check(activeCards.map { it.id } == listOf(2L))
                check(failure is NoSuchElementException)
            }
        }

        Scenario("owner can activate toggle lose and terminate a card") {
            Given("an issued card owned by the customer") {
                data.cards[1] = card(1)
            }
            When("the owner activates disables loses and terminates the card") {
                activationResponse = service.activateCard(1, 7)
                toggleResponse = service.toggleCardUsage(1, 7, false)
                lostResponse = service.reportLostCard(1, 7)
                terminationResponse = service.terminateCard(1, 7)
            }
            Then("every lifecycle transition is returned") {
                check(activationResponse!!.status == CardStatus.ACTIVE.description)
                check(!toggleResponse!!.isEnabled)
                check(lostResponse!!.status == CardStatus.LOST.description)
                check(terminationResponse!!.status == CardStatus.TERMINATED.description)
            }
        }

        Scenario("mutations reject a different owner") {
            Given("an issued card owned by another customer") {
                data.cards[1] = card(1)
            }
            When("the different owner attempts every lifecycle mutation") {
                mutationFailures = listOf<() -> Unit>(
                    { service.activateCard(1, 8) }, { service.toggleCardUsage(1, 8, false) },
                    { service.reportLostCard(1, 8) }, { service.terminateCard(1, 8) }
                ).map { runCatching(it).exceptionOrNull() }
            }
            Then("all four ownership checks reject the mutations") {
                check(mutationFailures.size == 4)
                check(mutationFailures.all { it is IllegalArgumentException })
            }
        }

        Scenario("every lifecycle mutation rejects a missing card") {
            Given("the requested card does not exist") {
                check(data.cards.isEmpty())
            }
            When("every lifecycle mutation is attempted") {
                mutationFailures = listOf<() -> Unit>(
                    { service.activateCard(99, 7) }, { service.toggleCardUsage(99, 7, false) },
                    { service.reportLostCard(99, 7) }, { service.terminateCard(99, 7) }
                ).map { runCatching(it).exceptionOrNull() }
            }
            Then("all four missing-card checks reject the mutations") {
                check(mutationFailures.size == 4)
                check(mutationFailures.all { it is NoSuchElementException })
            }
        }
    }
}
