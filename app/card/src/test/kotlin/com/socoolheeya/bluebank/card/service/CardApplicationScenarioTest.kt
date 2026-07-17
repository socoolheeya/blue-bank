package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import com.socoolheeya.bluebank.card.dto.CardApplicationDto
import com.socoolheeya.bluebank.card.testing.FakeCardApplicationDataService
import com.socoolheeya.bluebank.card.testing.FakeCardDataService
import de.infix.testBalloon.framework.core.testSuite
import java.time.LocalDateTime

private fun request(product: CardProductType = CardProductType.FRIENDS_CHECK, moim: Long? = null) = CardApplicationDto.Request(
    7, 9, CardType.DEBIT, product, "KIM", "000000-0000000", "010", "a@b.c", "Seoul", "BLUE", moimAccountId = moim
)

private fun approved() = CardApplicationResult(1, 7, 9, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "KIM", "010", null,
    "BLUE", null, false, CardApplicationStatus.APPROVED, null, null, null, LocalDateTime.now(), LocalDateTime.now())

val cardApplicationScenarios by testSuite("Card application scenarios") {
    test("submission maps every application boundary and supports lookup") {
        val applications = FakeCardApplicationDataService()
        val service = CardApplicationService(applications, FakeCardDataService())
        val response = service.applyForCard(request())
        check(response.id == 1L && applications.submitted.single().residentNumber == "000000-0000000")
        check(service.getApplication(1).customerId == 7L)
        check(service.getApplicationsByCustomerId(7).size == 1)
        check(runCatching { service.getApplication(99) }.exceptionOrNull() is NoSuchElementException)
    }

    test("moim application requires a meeting account") {
        val service = CardApplicationService(FakeCardApplicationDataService(), FakeCardDataService())
        check(runCatching { service.applyForCard(request(CardProductType.MOIM_CHECK)) }.exceptionOrNull() is IllegalArgumentException)
    }

    test("approved application issues a stable shaped card and marks application issued") {
        val applications = FakeCardApplicationDataService().apply { this.applications[1] = approved() }
        val cards = FakeCardDataService()
        val response = CardApplicationService(applications, cards).issueCard(1)
        val command = cards.created.single()
        check(response.cardId == 100L && response.cardNumberMasked.matches(Regex("5234-\\*{4}-\\*{4}-\\d{4}")))
        check(command.cardNumber.length == 16 && command.expiryDate == LocalDateTime.now().toLocalDate().plusYears(5))
        check(applications.issued.single() == (1L to 100L))
    }

    test("issuance rejects missing and unapproved applications") {
        val applications = FakeCardApplicationDataService()
        val service = CardApplicationService(applications, FakeCardDataService())
        check(runCatching { service.issueCard(1) }.exceptionOrNull() is NoSuchElementException)
        applications.applications[1] = approved().copy(status = CardApplicationStatus.SUBMITTED)
        check(runCatching { service.issueCard(1) }.exceptionOrNull() is IllegalArgumentException)
    }
}
