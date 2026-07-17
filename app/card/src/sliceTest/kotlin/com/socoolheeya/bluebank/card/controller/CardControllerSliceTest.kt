package com.socoolheeya.bluebank.card.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import com.socoolheeya.bluebank.card.data.repository.CardApplicationRepository
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import com.socoolheeya.bluebank.card.data.service.CardApplicationDataService
import com.socoolheeya.bluebank.card.data.service.CardDataService
import com.socoolheeya.bluebank.card.dto.CardApplicationDto
import com.socoolheeya.bluebank.card.exception.GlobalErrorHandler
import com.socoolheeya.bluebank.card.service.CardApplicationService
import com.socoolheeya.bluebank.card.service.CardService
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
private fun <T> repo(type: Class<T>) = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, _ -> error(m.name) } as T
private val cardResult = CardResult(1, "5234000011112222", "5234-****-****-2222", 7, 9, CardType.DEBIT,
    CardProductType.FRIENDS_CHECK, "KIM", LocalDate.now(), LocalDate.now().plusYears(5), CardStatus.ACTIVE,
    BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, "BLUE", null, false, true, true,
    null, null, BigDecimal.ZERO, null, null, null, LocalDateTime.now(), LocalDateTime.now())
private class SliceCards : CardDataService(repo(CardRepository::class.java)) {
    override fun getCard(cardId: Long) = if (cardId == 1L) cardResult else null
    override fun getCardsByCustomerId(customerId: Long) = if (customerId == 7L) listOf(cardResult) else emptyList()
    override fun getActiveCardsByCustomerId(customerId: Long) = getCardsByCustomerId(customerId)
    override fun activateCard(command: com.socoolheeya.bluebank.card.data.domain.command.CardCommand.Activate) = cardResult
    override fun toggleCardUsage(command: com.socoolheeya.bluebank.card.data.domain.command.CardCommand.ToggleUsage) = cardResult.copy(isEnabled = command.enabled)
    override fun reportLostCard(cardId: Long) = cardResult.copy(status = CardStatus.LOST)
    override fun terminateCard(command: com.socoolheeya.bluebank.card.data.domain.command.CardCommand.Terminate) = cardResult.copy(status = CardStatus.TERMINATED)
    override fun createCard(command: com.socoolheeya.bluebank.card.data.domain.command.CardCommand.Create) =
        cardResult.copy(id = 10L, cardNumber = command.cardNumber, cardNumberMasked = command.cardNumberMasked)
}
private class SliceApplications : CardApplicationDataService(repo(CardApplicationRepository::class.java), repo(CardRepository::class.java)) {
    private var result = CardApplicationResult(2, 7, 9, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "KIM", "010", null,
        "BLUE", null, false, com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus.SUBMITTED,
        null, null, null, LocalDateTime.now(), null)
    override fun submitApplication(command: com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand.Submit) = result
    override fun getApplication(applicationId: Long) = if (applicationId == 2L) result else null
    override fun getApplicationsByCustomerId(customerId: Long) = if (customerId == 7L) listOf(result) else emptyList()
    override fun markAsIssued(applicationId: Long, cardId: Long) =
        result.copy(status = com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus.ISSUED, cardId = cardId)

    fun approveForIssue() {
        result = result.copy(status = com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus.APPROVED)
    }
}

private fun sliceMvc(cards: SliceCards, applications: SliceApplications) = MockMvcBuilders.standaloneSetup(
    CardController(CardService(cards)), CardApplicationController(CardApplicationService(applications, cards))
).setControllerAdvice(GlobalErrorHandler()).build()

val cardControllerSlices by testSuite("Card controller slices") {
    val json = ObjectMapper().findAndRegisterModules()

    test("card endpoints bind paths queries and bodies") {
        val cards = SliceCards()
        val mvc = sliceMvc(cards, SliceApplications())
        mvc.perform(get("/api/cards")).andExpect(status().isOk).andExpect(content().string("Card Service is running"))
        mvc.perform(get("/api/cards/1")).andExpect(status().isOk).andExpect(jsonPath("$.id").value(1))
        mvc.perform(get("/api/cards/customer/7")).andExpect(jsonPath("$[0].customerId").value(7))
        mvc.perform(get("/api/cards/customer/7/active")).andExpect(jsonPath("$[0].status").value("활성화"))
        mvc.perform(post("/api/cards/1/activate").param("customerId", "7")).andExpect(status().isOk)
        mvc.perform(put("/api/cards/1/toggle").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.enabled").value(false))
        mvc.perform(post("/api/cards/1/report-lost").param("customerId", "7")).andExpect(jsonPath("$.status").value("분실"))
        mvc.perform(post("/api/cards/1/terminate").param("customerId", "7")).andExpect(jsonPath("$.status").value("해지"))
        mvc.perform(get("/api/cards/99")).andExpect(status().is5xxServerError)
    }

    test("application endpoints bind request and list shapes") {
        val cards = SliceCards()
        val applications = SliceApplications()
        val mvc = sliceMvc(cards, applications)
        val request = CardApplicationDto.Request(7, 9, CardType.DEBIT, CardProductType.FRIENDS_CHECK, "KIM", "000", "010", address = "Seoul", designCode = "BLUE")
        mvc.perform(post("/api/cards/applications").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk).andExpect(jsonPath("$.id").value(2))
        mvc.perform(get("/api/cards/applications/2")).andExpect(jsonPath("$.customerId").value(7))
        mvc.perform(get("/api/cards/applications/customer/7")).andExpect(jsonPath("$[0].id").value(2))
        applications.approveForIssue()
        mvc.perform(post("/api/cards/applications/2/issue"))
            .andExpect(status().isOk).andExpect(jsonPath("$.applicationId").value(2))
            .andExpect(jsonPath("$.cardId").value(10)).andExpect(jsonPath("$.cardNumberMasked").isString)
        mvc.perform(post("/api/cards/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().is5xxServerError)
    }
}
