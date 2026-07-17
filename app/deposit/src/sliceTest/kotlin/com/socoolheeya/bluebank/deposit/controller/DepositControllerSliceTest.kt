package com.socoolheeya.bluebank.deposit.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.socoolheeya.bluebank.deposit.adapter.*
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositStatus
import com.socoolheeya.bluebank.deposit.data.domain.DepositEnums.DepositTransactionType
import com.socoolheeya.bluebank.deposit.data.domain.command.DepositCommand
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositResult
import com.socoolheeya.bluebank.deposit.data.domain.result.DepositTransactionResult
import com.socoolheeya.bluebank.deposit.data.repository.*
import com.socoolheeya.bluebank.deposit.data.service.DepositDataService
import com.socoolheeya.bluebank.deposit.exception.GlobalErrorHandler
import com.socoolheeya.bluebank.deposit.service.DepositService
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

@Suppress("UNCHECKED_CAST")
private fun <T> repo(type: Class<T>) = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, m, _ -> error(m.name) } as T

private class SliceData : DepositDataService(repo(DepositRepository::class.java), repo(DepositTransactionRepository::class.java), repo(InterestPaymentRepository::class.java)) {
    private val values = linkedMapOf<Long, DepositResult>()
    private var next = 1L
    override fun createDeposit(command: DepositCommand.Create): DepositResult = DepositResult.from(command.toEntity()).copy(id = next++).also { values[it.id!!] = it }
    override fun getDeposit(depositId: Long) = values[depositId] ?: throw IllegalArgumentException("missing $depositId")
    override fun getDepositsByCustomer(customerId: Long) = values.values.filter { it.customerId == customerId }
    override fun activateDeposit(depositId: Long) = change(depositId) {
        require(it.status == DepositStatus.PENDING)
        it.copy(status = DepositStatus.ACTIVE)
    }
    override fun terminateDeposit(depositId: Long) = change(depositId) {
        require(it.status == DepositStatus.ACTIVE)
        it.copy(status = DepositStatus.TERMINATED)
    }
    override fun earlyWithdraw(depositId: Long, amount: BigDecimal) = change(depositId) {
        require(it.status == DepositStatus.ACTIVE)
        require(amount > BigDecimal.ZERO && amount <= it.currentBalance)
        it.copy(currentBalance = it.currentBalance - amount)
    }
    override fun deposit(depositId: Long, amount: BigDecimal, description: String?): DepositTransactionResult {
        val value = change(depositId) {
            require(it.status == DepositStatus.ACTIVE)
            require(amount > BigDecimal.ZERO)
            it.copy(currentBalance = it.currentBalance + amount)
        }
        return DepositTransactionResult(1, depositId, value.customerId, DepositTransactionType.DEPOSIT, amount, value.currentBalance,
            description, false, null, null, LocalDateTime.now())
    }
    private fun change(id: Long, f: (DepositResult) -> DepositResult) = f(getDeposit(id)).also { values[id] = it }
}

private class SliceAccounts : AccountServiceClient {
    override fun getAccount(accountId: Long) = AccountResponse(accountId, "110", if (accountId == 9L) 7 else 8, "CHECKING", "main", "ACTIVE", "KRW")
    override fun getBalance(accountId: Long) = BalanceResponse(accountId, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO)
    override fun validateAccount(accountId: Long) = AccountValidationResponse(accountId, accountId != 99L, true, true, if (accountId == 99L) "blocked" else "valid")
}

private val createJson = """{"customerId":7,"accountId":9,"productType":"FREE_SAVINGS","principalAmount":1000.25,"baseRate":3.50,"contractPeriod":12,"periodUnit":"MONTH","startDate":"2026-07-17","maturityDate":"2027-07-17","autoTransferEnabled":false,"autoRenewalEnabled":false,"spareChangeEnabled":false,"aiSavingsEnabled":false,"isTaxFree":false}"""

private data class SliceFixture(val data: SliceData, val mvc: org.springframework.test.web.servlet.MockMvc, val json: ObjectMapper)
private fun fixture(): SliceFixture {
    val data = SliceData()
    val mvc = MockMvcBuilders.standaloneSetup(DepositController(DepositService(data, SliceAccounts()), "8088", "deposit-1", "deposit"))
        .setControllerAdvice(GlobalErrorHandler()).build()
    return SliceFixture(data, mvc, ObjectMapper().findAndRegisterModules())
}

private fun create(mvc: org.springframework.test.web.servlet.MockMvc) {
    mvc.perform(post("/api/deposits").contentType(MediaType.APPLICATION_JSON).content(createJson)).andExpect(status().isOk)
}

val depositControllerSlices by testSuite("Deposit controller slices") {
    test("health and explicit error endpoints expose status and response shape") {
        val mvc = fixture().mvc
        mvc.perform(get("/api/deposits")).andExpect(status().isOk).andExpect(jsonPath("$.message").value("Deposit Service is running"))
            .andExpect(jsonPath("$.instanceInfo.port").value("8088"))
        mvc.perform(get("/api/deposits/error-test")).andExpect(status().isNotFound).andExpect(jsonPath("$.status").value(404))
    }

    test("create lookup and customer list bind decimal dates paths and response shapes") {
        val mvc = fixture().mvc
        mvc.perform(post("/api/deposits").contentType(MediaType.APPLICATION_JSON).content(createJson))
            .andExpect(status().isOk).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.currentBalance").value(0))
            .andExpect(jsonPath("$.startDate").value("2026-07-17"))
        mvc.perform(get("/api/deposits/1")).andExpect(status().isOk).andExpect(jsonPath("$.customerId").value(7))
        mvc.perform(get("/api/deposits/customer/7")).andExpect(status().isOk).andExpect(jsonPath("$[0].id").value(1))
    }

    test("mutation endpoints bind customer query and decimal JSON bodies") {
        val (data, mvc, json) = fixture()
        create(mvc)
        mvc.perform(post("/api/deposits/1/activate").param("customerId", "7")).andExpect(jsonPath("$.status").value("활성"))
        mvc.perform(post("/api/deposits/1/deposit").param("customerId", "7").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(mapOf("amount" to BigDecimal("50.75"), "description" to "monthly"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.currentBalance").value(50.75))
        mvc.perform(post("/api/deposits/1/withdraw").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10.25}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.currentBalance").value(40.5))
        mvc.perform(post("/api/deposits/1/terminate").param("customerId", "7")).andExpect(jsonPath("$.status").value("해지"))
        check(data.getDeposit(1).currentBalance.compareTo(BigDecimal("40.50")) == 0)
    }

    test("invalid account ownership missing deposit and missing query parameter are rejected") {
        val mvc = fixture().mvc
        mvc.perform(post("/api/deposits").contentType(MediaType.APPLICATION_JSON).content(createJson.replace("\"accountId\":9", "\"accountId\":99")))
            .andExpect(status().isBadRequest)
        create(mvc)
        mvc.perform(post("/api/deposits/1/activate").param("customerId", "8")).andExpect(status().isBadRequest)
        mvc.perform(get("/api/deposits/999")).andExpect(status().isBadRequest)
        mvc.perform(post("/api/deposits/1/activate")).andExpect(status().is5xxServerError)
    }

    test("invalid lifecycle and signed money requests preserve response state and balance") {
        val (data, mvc) = fixture()
        create(mvc)
        mvc.perform(post("/api/deposits/1/deposit").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
            .andExpect(status().isBadRequest)
        mvc.perform(post("/api/deposits/1/withdraw").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
            .andExpect(status().isBadRequest)
        mvc.perform(post("/api/deposits/1/terminate").param("customerId", "7")).andExpect(status().isBadRequest)
        check(data.getDeposit(1).status == DepositStatus.PENDING && data.getDeposit(1).currentBalance.compareTo(BigDecimal.ZERO) == 0)
        mvc.perform(post("/api/deposits/1/activate").param("customerId", "7")).andExpect(status().isOk)
        mvc.perform(post("/api/deposits/1/activate").param("customerId", "7")).andExpect(status().isBadRequest)
        listOf("0", "-1").forEach { amount ->
            mvc.perform(post("/api/deposits/1/deposit").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":$amount}"))
                .andExpect(status().isBadRequest)
            mvc.perform(post("/api/deposits/1/withdraw").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":$amount}"))
                .andExpect(status().isBadRequest)
        }
        mvc.perform(post("/api/deposits/1/deposit").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10}"))
            .andExpect(status().isOk)
        mvc.perform(post("/api/deposits/1/withdraw").param("customerId", "7").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":11}"))
            .andExpect(status().isBadRequest)
        check(data.getDeposit(1).status == DepositStatus.ACTIVE && data.getDeposit(1).currentBalance.compareTo(BigDecimal.TEN) == 0)
        mvc.perform(post("/api/deposits/1/terminate").param("customerId", "7")).andExpect(status().isOk)
        mvc.perform(post("/api/deposits/1/terminate").param("customerId", "7")).andExpect(status().isBadRequest)
        check(data.getDeposit(1).status == DepositStatus.TERMINATED && data.getDeposit(1).currentBalance.compareTo(BigDecimal.TEN) == 0)
    }
}
