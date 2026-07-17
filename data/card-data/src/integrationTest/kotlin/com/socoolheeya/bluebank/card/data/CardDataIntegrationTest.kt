package com.socoolheeya.bluebank.card.data

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.service.CardApplicationDataService
import com.socoolheeya.bluebank.card.data.service.CardDataService
import com.socoolheeya.bluebank.card.data.domain.CardEnums.BenefitStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.BenefitType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CashbackType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.TransactionStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.TransactionType
import com.socoolheeya.bluebank.card.data.domain.entity.CardBenefit
import com.socoolheeya.bluebank.card.data.domain.entity.CardStatement
import com.socoolheeya.bluebank.card.data.domain.entity.CardTransaction
import com.socoolheeya.bluebank.card.data.domain.entity.CashbackHistory
import com.socoolheeya.bluebank.card.data.repository.*
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootApplication(scanBasePackages = ["com.socoolheeya.bluebank.card.data"])
private class CardDataIntegrationConfiguration

private inline fun withCardContext(block: (org.springframework.context.ConfigurableApplicationContext) -> Unit) {
    SpringApplicationBuilder(CardDataIntegrationConfiguration::class.java)
        .web(WebApplicationType.NONE)
        .profiles("integration")
        .run()
        .use { context ->
            context.getBean(CashbackHistoryRepository::class.java).deleteAll()
            context.getBean(CardTransactionRepository::class.java).deleteAll()
            context.getBean(CardStatementRepository::class.java).deleteAll()
            context.getBean(CardBenefitRepository::class.java).deleteAll()
            context.getBean(CardApplicationRepository::class.java).deleteAll()
            context.getBean(CardRepository::class.java).deleteAll()
            block(context)
        }
}

private fun create(number: String, customer: Long = 7) = CardCommand.Create(
    number, "5234-****-****-${number.takeLast(4)}", customer, 9, CardType.DEBIT,
    CardProductType.FRIENDS_CHECK, "KIM", LocalDate.now().plusYears(5), "123",
    BigDecimal("5000000"), BigDecimal("20000000"), "BLUE"
)

val cardDataIntegrationTests by testSuite("Card data H2 integration", compartment = { TestCompartment.Sequential }) {
    test("card persistence queries and lifecycle transitions use real repositories") {
        withCardContext { context ->
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
    }

    test("application approval and issuance transitions persist the created card relationship") {
        withCardContext { context ->
        val service = context.getBean(CardApplicationDataService::class.java)
        val submitted = service.submitApplication(CardApplicationCommand.Submit(
            27, 29, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "LEE", "000", "010", address = "Seoul", designCode = "BLUE"
        ))
        val submittedId = requireNotNull(submitted.id)
        check(service.getApplication(submittedId) != null)
        check(service.getApplicationsByCustomerId(27).map { it.id } == listOf(submittedId))
        check(service.startReview(submittedId).status.name == "UNDER_REVIEW")
        val approved = service.approveApplication(submittedId, BigDecimal("700000.00"), create("5234000011110027", 27))
        val cardId = requireNotNull(approved.cardId)
        check(approved.status.name == "APPROVED")
        check(approved.approvedCreditLimit == BigDecimal("700000.00"))
        check(context.getBean(CardRepository::class.java).findById(cardId).orElseThrow().customerId == 27L)
        val issued = service.markAsIssued(submittedId, cardId)
        check(issued.status.name == "ISSUED")
        val persisted = context.getBean(CardApplicationRepository::class.java).findById(submittedId).orElseThrow()
        check(persisted.status.name == "ISSUED")
        check(persisted.cardId == cardId)
        check(service.getApplication(Long.MAX_VALUE) == null)
        }
    }

    test("transaction statement benefit and cashback repositories persist and query real H2 rows") {
        withCardContext { context ->
        val transactions = context.getBean(CardTransactionRepository::class.java)
        val statements = context.getBean(CardStatementRepository::class.java)
        val benefits = context.getBean(CardBenefitRepository::class.java)
        val cashbacks = context.getBean(CashbackHistoryRepository::class.java)
        val now = java.time.LocalDateTime.now()
        val today = LocalDate.now()

        val transaction = transactions.save(CardTransaction(
            cardId = 41, customerId = 42, transactionId = "TX-41", merchantName = "Cafe",
            merchantCategory = "5812", transactionType = TransactionType.PURCHASE,
            amount = BigDecimal("12000"), transactionDate = now, status = TransactionStatus.PENDING
        ))
        check(transactions.findByTransactionId("TX-41").orElseThrow().id == transaction.id)
        check(transactions.findByCardIdAndTransactionDateBetween(41, now.minusMinutes(1), now.plusMinutes(1)).size == 1)

        val statement = statements.save(CardStatement(
            cardId = 41, customerId = 42, statementYear = today.year, statementMonth = today.monthValue,
            periodStart = today.withDayOfMonth(1), periodEnd = today, totalAmount = BigDecimal("12000"),
            totalCashback = BigDecimal("120"), netAmount = BigDecimal("11880"), paymentDueDate = today.plusDays(10), transactionCount = 1
        ))
        check(statements.findByCardIdAndStatementYearAndStatementMonth(41, today.year, today.monthValue).orElseThrow().id == statement.id)

        val benefit = benefits.save(CardBenefit(
            productType = CardProductType.FRIENDS_CHECK, benefitName = "Cafe cashback", benefitDescription = "one percent",
            benefitType = BenefitType.CASHBACK, cashbackRate = BigDecimal("1.00"), startDate = today.minusDays(1)
        ))
        check(benefits.findByProductTypeAndStatus(CardProductType.FRIENDS_CHECK, BenefitStatus.ACTIVE).single().id == benefit.id)

        val cashback = cashbacks.save(CashbackHistory(
            cardId = 41, customerId = 42, transactionId = requireNotNull(transaction.id), cashbackAmount = BigDecimal("120"),
            cashbackRate = BigDecimal("1.00"), transactionAmount = BigDecimal("12000"), cashbackType = CashbackType.STANDARD,
            earnedDate = today, paymentDate = today.plusMonths(1)
        ))
        check(cashbacks.findByTransactionId(requireNotNull(transaction.id)).single().id == cashback.id)
        check(cashbacks.countByCardIdAndEarnedDate(41, today) == 1)
        }
    }
}
