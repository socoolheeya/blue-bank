package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import com.socoolheeya.bluebank.card.dto.CardApplicationDto
import com.socoolheeya.bluebank.card.testing.FakeCardApplicationDataService
import com.socoolheeya.bluebank.card.testing.FakeCardDataService
import com.socoolheeya.bluebank.card.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.time.LocalDate
import java.time.LocalDateTime

private fun request(product: CardProductType = CardProductType.FRIENDS_CHECK, moim: Long? = null) = CardApplicationDto.Request(
    7, 9, CardType.DEBIT, product, "KIM", "000000-0000000", "010", "a@b.c", "Seoul", "BLUE",
    customText = "HELLO", requestTransitCard = true, requestOverseasUsage = false, moimAccountId = moim,
    annualIncome = java.math.BigDecimal("70000000"), employmentType = "FULL_TIME", companyName = "BLUE",
    creditScore = 900, requestedCreditLimit = java.math.BigDecimal("5000000")
)

private fun approved() = CardApplicationResult(1, 7, 9, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "KIM", "010", null,
    "BLUE", null, false, CardApplicationStatus.APPROVED, null, null, null, LocalDateTime.now(), LocalDateTime.now())

private class CardApplicationScenarioContext {
    val applications = FakeCardApplicationDataService()
    val cards = FakeCardDataService()
    val service = CardApplicationService(applications, cards)
    var applicationRequest: CardApplicationDto.Request? = null
    var response: CardApplicationDto.Response? = null
    var issueResponse: CardApplicationDto.IssueResponse? = null
    var application: CardApplicationDto.Response? = null
    var customerApplications: List<CardApplicationDto.Response> = emptyList()
    var failure: Throwable? = null
    var secondFailure: Throwable? = null
    var before: LocalDate? = null
    var after: LocalDate? = null
}

val cardApplicationScenarios by testSuite("Card application scenarios") {
    testFixture { CardApplicationScenarioContext() } asContextForEach {
        Scenario("submission maps every application boundary and supports lookup") {
            Given("a complete card application request") {
                applicationRequest = request()
            }
            When("the request is submitted and applications are looked up") {
                response = service.applyForCard(applicationRequest!!)
                application = service.getApplication(1)
                customerApplications = service.getApplicationsByCustomerId(7)
                failure = runCatching { service.getApplication(99) }.exceptionOrNull()
            }
            Then("every request field and lookup result is preserved") {
                val command = applications.submitted.single()
                check(response!!.id == 1L)
                check(command.customerId == 7L && command.accountId == 9L && command.cardType == CardType.DEBIT)
                check(command.productType == CardProductType.FRIENDS_CHECK && command.applicantName == "KIM")
                check(command.residentNumber == "000000-0000000" && command.phoneNumber == "010" && command.email == "a@b.c")
                check(command.address == "Seoul" && command.designCode == "BLUE" && command.customText == "HELLO")
                check(command.requestTransitCard && !command.requestOverseasUsage && command.moimAccountId == null)
                check(command.annualIncome == java.math.BigDecimal("70000000") && command.employmentType == "FULL_TIME")
                check(command.companyName == "BLUE" && command.creditScore == 900 && command.requestedCreditLimit == java.math.BigDecimal("5000000"))
                check(application!!.customerId == 7L)
                check(customerApplications.size == 1)
                check(failure is NoSuchElementException)
            }
        }

        Scenario("moim application requires a meeting account") {
            Given("a moim card request without a meeting account") {
                applicationRequest = request(CardProductType.MOIM_CHECK)
            }
            When("the moim request is submitted") {
                failure = runCatching { service.applyForCard(applicationRequest!!) }.exceptionOrNull()
            }
            Then("the missing meeting account is rejected") {
                check(failure is IllegalArgumentException)
            }
        }

        Scenario("approved application issues a stable shaped card and marks application issued") {
            Given("an approved card application") {
                applications.applications[1] = approved()
                before = LocalDate.now()
            }
            When("the approved application is issued") {
                issueResponse = service.issueCard(1)
                after = LocalDate.now()
            }
            Then("the card shape expiry and issued link are stable") {
                val command = cards.created.single()
                check(issueResponse!!.cardId == 100L && issueResponse!!.cardNumberMasked.matches(Regex("5234-\\*{4}-\\*{4}-\\d{4}")))
                check(command.cardNumber.length == 16)
                check(!command.expiryDate.isBefore(before!!.plusYears(5)) && !command.expiryDate.isAfter(after!!.plusYears(5)))
                check(applications.issued.single() == (1L to 100L))
            }
        }

        Scenario("issuance rejects missing and unapproved applications") {
            Given("no application exists") {
                check(applications.applications.isEmpty())
            }
            When("a missing application and then an unapproved application are issued") {
                failure = runCatching { service.issueCard(1) }.exceptionOrNull()
                applications.applications[1] = approved().copy(status = CardApplicationStatus.SUBMITTED)
                secondFailure = runCatching { service.issueCard(1) }.exceptionOrNull()
            }
            Then("both invalid issuance attempts fail") {
                check(failure is NoSuchElementException)
                check(secondFailure is IllegalArgumentException)
            }
        }
    }
}
