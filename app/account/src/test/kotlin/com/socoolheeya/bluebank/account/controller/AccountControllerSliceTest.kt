package com.socoolheeya.bluebank.account.controller

import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import com.socoolheeya.bluebank.account.data.domain.Balance
import com.socoolheeya.bluebank.account.data.domain.result.AccountResult
import com.socoolheeya.bluebank.account.service.AccountService
import com.socoolheeya.bluebank.account.service.BalanceService
import de.infix.testBalloon.framework.core.testSuite
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader
import org.springframework.context.support.registerBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.math.BigDecimal
import java.time.LocalDateTime

private class AccountMvcFixture : AutoCloseable {
    val accountService: AccountService = mock(AccountService::class.java)
    val balanceService: BalanceService = mock(BalanceService::class.java)
    private val context = GenericWebApplicationContext().apply {
        environment.setActiveProfiles("controller-slice-test")
        registerBean<AccountService> { accountService }
        registerBean<BalanceService> { balanceService }
        AnnotatedBeanDefinitionReader(this).register(MvcConfig::class.java)
        servletContext = org.springframework.mock.web.MockServletContext()
        refresh()
    }
    val mvc: MockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    override fun close() = context.close()
}

@Configuration
@EnableWebMvc
@Profile("controller-slice-test")
private open class MvcConfig {
    @Bean open fun externalAccountController() = ExternalAccountController()
    @Bean open fun internalAccountController(accountService: AccountService, balanceService: BalanceService) =
        InternalAccountController(accountService, balanceService)
}

private fun account(id: Long, status: AccountEnums.AccountStatus = AccountEnums.AccountStatus.ACTIVE) = AccountResult(
    id, "1000-$id", "account-$id", AccountEnums.AccountType.CHECKING, AccountEnums.ProductType.BASIC_CHECKING,
    status, BigDecimal("0.02"), LocalDateTime.of(2026, 1, 2, 3, 4), null
)

val accountControllerSlices by testSuite("Account controller slices") {
    test("GET api accounts binds the external route") {
        AccountMvcFixture().use { it.mvc.get("/api/accounts").andExpect { status { isOk() }; content { string("test") } } }
    }
    test("internal id and number lookups serialize account fields") {
        AccountMvcFixture().use {
            whenever(it.accountService.getAccountById(7)).thenReturn(account(7))
            whenever(it.accountService.getAccountByAccountNumber("1000-7")).thenReturn(account(7))
            it.mvc.get("/internal/accounts/7").andExpect { status { isOk() }; jsonPath("$.id") { value(7) }; jsonPath("$.accountNumber") { value("1000-7") } }
            it.mvc.get("/internal/accounts/by-number/1000-7").andExpect { status { isOk() }; jsonPath("$.status") { value("ACTIVE") } }
        }
    }
    test("customer listing binds customer id and serializes every account") {
        AccountMvcFixture().use {
            whenever(it.accountService.getAccountsByCustomerId(42)).thenReturn(listOf(account(1), account(2)))
            it.mvc.get("/internal/accounts/by-customer/42").andExpect {
                status { isOk() }; jsonPath("$.length()") { value(2) }; jsonPath("$[1].id") { value(2) }
            }
        }
    }
    test("balance lookup serializes all monetary fields") {
        AccountMvcFixture().use {
            whenever(it.balanceService.getBalance(7)).thenReturn(Balance(7, BigDecimal("100"), BigDecimal("80"), BigDecimal("20"), updatedAt = LocalDateTime.now()))
            it.mvc.get("/internal/accounts/7/balance").andExpect {
                status { isOk() }; jsonPath("$.accountId") { value(7) }; jsonPath("$.ledgerBalance") { value(100) }
                jsonPath("$.availableBalance") { value(80) }; jsonPath("$.holdBalance") { value(20) }
            }
        }
    }
    test("validation reports active inactive and missing accounts in Korean") {
        AccountMvcFixture().use {
            whenever(it.accountService.getAccountById(1)).thenReturn(account(1))
            whenever(it.accountService.getAccountById(2)).thenReturn(account(2, AccountEnums.AccountStatus.FROZEN))
            whenever(it.accountService.getAccountById(3)).thenThrow(NoSuchElementException("missing"))
            it.mvc.get("/internal/accounts/1/validate").andExpect { status { isOk() }; jsonPath("$.valid") { value(true) }; jsonPath("$.message") { value("유효한 계좌입니다") } }
            it.mvc.get("/internal/accounts/2/validate").andExpect { status { isOk() }; jsonPath("$.active") { value(false) }; jsonPath("$.message") { value("비활성 상태의 계좌입니다 (FROZEN)") } }
            it.mvc.get("/internal/accounts/3/validate").andExpect { status { isOk() }; jsonPath("$.exists") { value(false) }; jsonPath("$.message") { value("존재하지 않는 계좌입니다") } }
        }
    }
}
