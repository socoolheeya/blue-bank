package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ApplicationStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ProductType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentMethod
import com.socoolheeya.bluebank.loan.dto.LoanApplicationDto
import com.socoolheeya.bluebank.loan.testing.*
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

private fun request(product: ProductType = ProductType.GENERAL_CREDIT, income: String = "25000000",
                    debt: String = "0", amount: String = "10000000") = LoanApplicationDto.Request(
    42, 7, product, BigDecimal(amount), 12, RepaymentMethod.EQUAL_INSTALLMENT,
    BigDecimal(income), "employee", 36, "Blue", totalExistingDebt = BigDecimal(debt)
)

private class FixedCreditScore(private val value: Int) : CreditScoreService() {
    override fun getCreditScore(customerId: Long) = CreditScoreResult(value, "test", "test")
}

val loanApplicationScenarios by testSuite("Loan application scenarios") {
    test("valid account and exact score income and DSR boundaries submit an application") {
        val data = FakeLoanApplicationDataService(); val account = FakeAccountClient()
        val service = LoanApplicationService(data, account, CreditScoreService())
        val result = service.applyForLoan(request(debt = "0", amount = "10000000"))
        check(result.id == 1L && data.submitted.single().creditScore == 750)
        check(account.validatedAccountIds == listOf(7L))
        val exactDsr = service.applyForLoan(request(income = "25000000", debt = "1", amount = "9999999"))
        check(exactDsr.id == 2L)
    }
    test("invalid account and income and DSR above boundaries are rejected") {
        val invalid = LoanApplicationService(FakeLoanApplicationDataService(), FakeAccountClient(false), CreditScoreService())
        check(runCatching { invalid.applyForLoan(request()) }.exceptionOrNull() is IllegalArgumentException)
        val service = LoanApplicationService(FakeLoanApplicationDataService(), FakeAccountClient(), CreditScoreService())
        check(runCatching { service.applyForLoan(request(income = "24999999")) }.exceptionOrNull() is IllegalArgumentException)
        check(runCatching { service.applyForLoan(request(debt = "0.01")) }.exceptionOrNull() is IllegalArgumentException)
    }
    test("minimum credit score is inclusive and one point below is rejected") {
        val atMinimum = LoanApplicationService(FakeLoanApplicationDataService(), FakeAccountClient(), FixedCreditScore(700))
        check(atMinimum.applyForLoan(request()).id == 1L)
        val below = LoanApplicationService(FakeLoanApplicationDataService(), FakeAccountClient(), FixedCreditScore(699))
        check(runCatching { below.applyForLoan(request()) }.exceptionOrNull() is IllegalArgumentException)
    }
    test("submit get list and missing application cover public queries") {
        val service = LoanApplicationService(FakeLoanApplicationDataService(), FakeAccountClient(), CreditScoreService())
        val one = service.applyForLoan(request()); service.applyForLoan(request(product = ProductType.EMERGENCY, income = "12000000", amount = "1000000"))
        check(service.getApplication(one.id!!).customerId == 42L)
        check(service.getApplicationsByCustomerId(42).size == 2)
        check(runCatching { service.getApplication(999) }.exceptionOrNull() is NoSuchElementException)
    }
    test("approval preserves money and maps credit secured and refinance families") {
        listOf(ProductType.EMERGENCY to LoanType.CREDIT, ProductType.MORTGAGE to LoanType.SECURED,
            ProductType.CREDIT_REFINANCE to LoanType.REFINANCE).forEach { (product, expected) ->
            val data = FakeLoanApplicationDataService(); val service = LoanApplicationService(data, FakeAccountClient(), CreditScoreService())
            val application = service.applyForLoan(request(product, income = "30000000", amount = "1000000"))
            val approved = service.approveApplication(application.id!!, BigDecimal("987654.32"), BigDecimal("4.125"))
            val command = data.approvalCommands.single()
            check(command.loanType == expected && command.principalAmount == BigDecimal("987654.32") && command.interestRate == BigDecimal("4.125"))
            check(approved.status == ApplicationStatus.APPROVED.description)
        }
    }
    test("reject preserves reason and approval missing fails") {
        val data = FakeLoanApplicationDataService(); val service = LoanApplicationService(data, FakeAccountClient(), CreditScoreService())
        val id = service.applyForLoan(request()).id!!
        check(service.rejectApplication(id, "policy").rejectionReason == "policy")
        check(runCatching { service.approveApplication(999, BigDecimal.ONE, BigDecimal.ONE) }.exceptionOrNull() is NoSuchElementException)
    }
}
