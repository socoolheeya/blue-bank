package com.socoolheeya.bluebank.loan.controller

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ApplicationStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ProductType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RateType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentMethod
import com.socoolheeya.bluebank.loan.data.domain.result.LoanApplicationResult
import com.socoolheeya.bluebank.loan.data.domain.result.LoanResult
import com.socoolheeya.bluebank.loan.dto.LoanApplicationDto
import com.socoolheeya.bluebank.loan.dto.LoanDto
import com.socoolheeya.bluebank.loan.exception.GlobalErrorHandler
import com.socoolheeya.bluebank.loan.service.LoanApplicationService
import com.socoolheeya.bluebank.loan.service.LoanService
import de.infix.testBalloon.framework.core.testSuite
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.registerBean
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private class LoanMvcFixture : AutoCloseable {
    val applications = mock(LoanApplicationService::class.java)
    val loans = mock(LoanService::class.java)
    private val context = GenericWebApplicationContext().apply {
        registerBean<LoanApplicationService> { applications }; registerBean<LoanService> { loans }
        AnnotatedBeanDefinitionReader(this).register(LoanMvcConfig::class.java)
        servletContext = org.springframework.mock.web.MockServletContext(); refresh()
    }
    val mvc = MockMvcBuilders.webAppContextSetup(context).build()
    override fun close() = context.close()
}

@Configuration @EnableWebMvc
private open class LoanMvcConfig {
    @Bean open fun applicationController(service: LoanApplicationService) = LoanApplicationController(service)
    @Bean open fun loanController(service: LoanService) = LoanController(service)
    @Bean open fun errors() = GlobalErrorHandler()
}

private val now = LocalDateTime.of(2026, 1, 2, 3, 4)
private fun application(id: Long = 1, status: ApplicationStatus = ApplicationStatus.SUBMITTED) = LoanApplicationDto.Response.from(
    LoanApplicationResult(id, 42, BigDecimal("1000.50"), 12, ProductType.GENERAL_CREDIT, RepaymentMethod.EQUAL_INSTALLMENT,
        BigDecimal("30000000"), 750, status, null, null, null, null, now, null))
private fun loan(id: Long = 1, status: LoanStatus = LoanStatus.APPROVED) = LoanDto.Response.from(LoanResult(id, "LN-1", 42, 7,
    LoanType.CREDIT, ProductType.GENERAL_CREDIT, BigDecimal("1000.50"), BigDecimal("900.25"), BigDecimal("3.125"),
    RateType.VARIABLE, 12, LocalDate.of(2026,1,1), LocalDate.of(2027,1,1), RepaymentMethod.EQUAL_INSTALLMENT,
    null, status, null, null, 750, BigDecimal.ZERO, now, now))

val loanControllerSlices by testSuite("Loan controller slices") {
    test("health loan object list execution and repayment endpoints bind and serialize") {
        LoanMvcFixture().use { f ->
            whenever(f.loans.getLoan(1)).thenReturn(loan()); whenever(f.loans.getLoansByCustomerId(42)).thenReturn(listOf(loan()))
            whenever(f.loans.executeLoan(1)).thenReturn(loan(status = LoanStatus.ACTIVE)); whenever(f.loans.repayLoan(eq(1), any())).thenReturn(loan())
            f.mvc.get("/api/loans").andExpect { status { isOk() }; content { string("Loan Service is running") } }
            f.mvc.get("/api/loans/1").andExpect { status { isOk() }; jsonPath("$.principalAmount") { value(1000.50) } }
            f.mvc.get("/api/loans/customer/42").andExpect { status { isOk() }; jsonPath("$[0].loanNumber") { value("LN-1") } }
            f.mvc.post("/api/loans/1/execute").andExpect { status { isOk() }; jsonPath("$.status") { value(LoanStatus.ACTIVE.description) } }
            f.mvc.post("/api/loans/1/repay") { contentType = org.springframework.http.MediaType.APPLICATION_JSON; content = """{"amount":12.34}""" }
                .andExpect { status { isOk() } }
            verify(f.loans).repayLoan(1, BigDecimal("12.34"))
        }
    }
    test("application submit object list approval and rejection bind exact values") {
        LoanMvcFixture().use { f ->
            whenever(f.applications.applyForLoan(any())).thenReturn(application()); whenever(f.applications.getApplication(1)).thenReturn(application())
            whenever(f.applications.getApplicationsByCustomerId(42)).thenReturn(listOf(application()))
            whenever(f.applications.approveApplication(eq(1), any(), any())).thenReturn(application(status = ApplicationStatus.APPROVED))
            whenever(f.applications.rejectApplication(eq(1), any())).thenReturn(application(status = ApplicationStatus.REJECTED))
            val body = """{"customerId":42,"accountId":7,"productType":"GENERAL_CREDIT","amount":1000.50,"term":12,"repaymentMethod":"EQUAL_INSTALLMENT","annualIncome":30000000,"employmentType":"employee","employmentPeriodMonths":12,"existingLoanCount":0,"totalExistingDebt":0,"hasDelayHistory":false}"""
            f.mvc.post("/api/loans/applications") { contentType = org.springframework.http.MediaType.APPLICATION_JSON; content = body }.andExpect { status { isOk() }; jsonPath("$.id") { value(1) } }
            f.mvc.get("/api/loans/applications/1").andExpect { status { isOk() }; jsonPath("$.customerId") { value(42) } }
            f.mvc.get("/api/loans/applications/customer/42").andExpect { status { isOk() }; jsonPath("$.length()") { value(1) } }
            f.mvc.post("/api/loans/applications/1/approve") { contentType = org.springframework.http.MediaType.APPLICATION_JSON; content = """{"approvedAmount":900.12,"approvedRate":4.321}""" }.andExpect { status { isOk() } }
            f.mvc.post("/api/loans/applications/1/reject") { contentType = org.springframework.http.MediaType.APPLICATION_JSON; content = """{"reason":"policy"}""" }.andExpect { status { isOk() } }
            verify(f.applications).approveApplication(1, BigDecimal("900.12"), BigDecimal("4.321")); verify(f.applications).rejectApplication(1, "policy")
        }
    }
    test("malformed request is bad request and missing service result uses global error response") {
        LoanMvcFixture().use { f ->
            f.mvc.post("/api/loans/1/repay") { contentType = org.springframework.http.MediaType.APPLICATION_JSON; content = "{}" }.andExpect { status { isBadRequest() } }
            whenever(f.loans.getLoan(404)).thenThrow(NoSuchElementException("missing"))
            f.mvc.get("/api/loans/404").andExpect { status { is5xxServerError() }; jsonPath("$.message") { value("An unexpected error occurred") } }
        }
    }
}
