package com.socoolheeya.bluebank.card.data

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.service.CardApplicationDataService
import com.socoolheeya.bluebank.card.data.service.CardDataService
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootApplication(scanBasePackages = ["com.socoolheeya.bluebank.card.data"])
private class CardDataIntegrationConfiguration

private val context by lazy {
    SpringApplicationBuilder(CardDataIntegrationConfiguration::class.java)
        .web(WebApplicationType.NONE)
        .profiles("integration")
        .run()
}

private fun create(number: String, customer: Long = 7) = CardCommand.Create(
    number, "5234-****-****-${number.takeLast(4)}", customer, 9, CardType.DEBIT,
    CardProductType.FRIENDS_CHECK, "KIM", LocalDate.now().plusYears(5), "123",
    BigDecimal("5000000"), BigDecimal("20000000"), "BLUE"
)

val cardDataIntegrationTests by testSuite("Card data H2 integration") {
    test("card persistence queries and lifecycle transitions use real repositories") {
        val service = context.getBean(CardDataService::class.java)
        val first = service.createCard(create("5234000011110001"))
        val firstId = requireNotNull(first.id)
        service.createCard(create("5234000011110002"))
        service.createCard(create("5234000011110003", 8))
        check(service.getCardsByCustomerId(7).size == 2)
        check(service.getActiveCardsByCustomerId(7).isEmpty())
        check(service.activateCard(CardCommand.Activate(firstId)).status == CardStatus.ACTIVE)
        check(service.getActiveCardsByCustomerId(7).map { it.id } == listOf(firstId))
        check(!service.toggleCardUsage(CardCommand.ToggleUsage(firstId, false)).isEnabled)
        check(service.reportLostCard(firstId).status == CardStatus.LOST)
        check(service.terminateCard(CardCommand.Terminate(firstId)).status == CardStatus.TERMINATED)
    }

    test("application persistence review rejection and lookup use real repositories") {
        val service = context.getBean(CardApplicationDataService::class.java)
        val submitted = service.submitApplication(CardApplicationCommand.Submit(
            27, 29, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "LEE", "000", "010", address = "Seoul", designCode = "BLUE"
        ))
        val submittedId = requireNotNull(submitted.id)
        check(service.getApplication(submittedId) != null)
        check(service.getApplicationsByCustomerId(27).map { it.id } == listOf(submittedId))
        check(service.startReview(submittedId).status.name == "UNDER_REVIEW")
        check(service.rejectApplication(submittedId, "policy").rejectionReason == "policy")
        check(service.getApplication(Long.MAX_VALUE) == null)
    }
}
