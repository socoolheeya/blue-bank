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
    7, 9, CardType.DEBIT, product, "KIM", "000000-0000000", "010", "a@b.c", "Seoul", "BLUE",
    customText = "HELLO", requestTransitCard = true, requestOverseasUsage = false, moimAccountId = moim,
    annualIncome = java.math.BigDecimal("70000000"), employmentType = "FULL_TIME", companyName = "BLUE",
    creditScore = 900, requestedCreditLimit = java.math.BigDecimal("5000000")
)

private fun approved() = CardApplicationResult(1, 7, 9, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "KIM", "010", null,
    "BLUE", null, false, CardApplicationStatus.APPROVED, null, null, null, LocalDateTime.now(), LocalDateTime.now())

val cardApplicationScenarios by testSuite("Card application scenarios") {
    test("submission maps every application boundary and supports lookup") {
        val applications = FakeCardApplicationDataService()
        val service = CardApplicationService(applications, FakeCardDataService())
        val response = service.applyForCard(request())
        val command = applications.submitted.single()
        check(response.id == 1L)
        check(command.customerId == 7L && command.accountId == 9L && command.cardType == CardType.DEBIT)
        check(command.productType == CardProductType.FRIENDS_CHECK && command.applicantName == "KIM")
        check(command.residentNumber == "000000-0000000" && command.phoneNumber == "010" && command.email == "a@b.c")
        check(command.address == "Seoul" && command.designCode == "BLUE" && command.customText == "HELLO")
        check(command.requestTransitCard && !command.requestOverseasUsage && command.moimAccountId == null)
        check(command.annualIncome == java.math.BigDecimal("70000000") && command.employmentType == "FULL_TIME")
        check(command.companyName == "BLUE" && command.creditScore == 900 && command.requestedCreditLimit == java.math.BigDecimal("5000000"))
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
        val before = java.time.LocalDate.now()
        val response = CardApplicationService(applications, cards).issueCard(1)
        val after = java.time.LocalDate.now()
        val command = cards.created.single()
        check(response.cardId == 100L && response.cardNumberMasked.matches(Regex("5234-\\*{4}-\\*{4}-\\d{4}")))
        check(command.cardNumber.length == 16)
        check(!command.expiryDate.isBefore(before.plusYears(5)) && !command.expiryDate.isAfter(after.plusYears(5)))
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
