package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ApplicationStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ProductType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentMethod
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.dto.LoanApplicationDto
import com.socoolheeya.bluebank.loan.testing.*
import com.socoolheeya.bluebank.testing.Scenario
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

private class LoanApplicationScenarioContext {
    val data = FakeLoanApplicationDataService()
    val account = FakeAccountClient()
    var creditScore: CreditScoreService = CreditScoreService()
    val service by lazy { LoanApplicationService(data, account, creditScore) }
    var result: LoanApplicationDto.Response? = null
    var secondResult: LoanApplicationDto.Response? = null
    var results: List<LoanApplicationDto.Response> = emptyList()
    var approvalCommand: LoanCommand.Create? = null
    var failure: Throwable? = null
    var failures: List<Throwable?> = emptyList()
}

val loanApplicationScenarios by testSuite("Loan application scenarios") {
    Scenario("valid account and exact score income and DSR boundaries submit an application", ::LoanApplicationScenarioContext) {
        When("applications at the income and exact DSR boundaries are submitted") {
            result = service.applyForLoan(request(debt = "0", amount = "10000000"))
            secondResult = service.applyForLoan(request(income = "25000000", debt = "1", amount = "9999999"))
        }
        Then("both applications pass validation with the stable score and account") {
            check(result!!.id == 1L && secondResult!!.id == 2L)
            check(data.submitted.map { it.creditScore } == listOf(750, 750))
            check(account.validatedAccountIds == listOf(7L, 7L))
        }
    }
    Scenario("invalid account and income and DSR above boundaries are rejected", ::LoanApplicationScenarioContext) {
        Given("an account that initially fails validation") { account.valid = false }
        When("invalid account income and DSR applications are submitted") {
            val accountFailure = runCatching { service.applyForLoan(request()) }.exceptionOrNull()
            account.valid = true
            val incomeFailure = runCatching { service.applyForLoan(request(income = "24999999")) }.exceptionOrNull()
            val dsrFailure = runCatching { service.applyForLoan(request(debt = "0.01")) }.exceptionOrNull()
            failures = listOf(accountFailure, incomeFailure, dsrFailure)
        }
        Then("each application is rejected at its explicit boundary") {
            check(failures.size == 3 && failures.all { it is IllegalArgumentException })
            check(data.submitted.isEmpty())
        }
    }
    Scenario("minimum credit score is inclusive", ::LoanApplicationScenarioContext) {
        Given("a score at the exact minimum") { creditScore = FixedCreditScore(700) }
        When("the application is submitted") { result = service.applyForLoan(request()) }
        Then("the exact minimum is accepted") { check(result!!.id == 1L) }
    }
    Scenario("one point below the minimum credit score is rejected", ::LoanApplicationScenarioContext) {
        Given("a score one point below the minimum") { creditScore = FixedCreditScore(699) }
        When("the application is submitted") { failure = runCatching { service.applyForLoan(request()) }.exceptionOrNull() }
        Then("the application is rejected") { check(failure is IllegalArgumentException) }
    }

    Scenario("submit get list and missing application cover public queries", ::LoanApplicationScenarioContext) {
        Given("two applications for the same customer") {
            result = service.applyForLoan(request())
            service.applyForLoan(request(product = ProductType.EMERGENCY, income = "12000000", amount = "1000000"))
        }
        When("one application and the customer list are queried with a missing lookup") {
            secondResult = service.getApplication(result!!.id!!)
            results = service.getApplicationsByCustomerId(42)
            failure = runCatching { service.getApplication(999) }.exceptionOrNull()
        }
        Then("the matching applications are returned and the missing ID fails") {
            check(secondResult!!.customerId == 42L)
            check(results.size == 2)
            check(failure is NoSuchElementException)
        }
    }

    listOf(
        ProductType.EMERGENCY to LoanType.CREDIT,
        ProductType.MORTGAGE to LoanType.SECURED,
        ProductType.CREDIT_REFINANCE to LoanType.REFINANCE,
    ).forEach { (product, expected) ->
        Scenario("approval maps $product to the $expected family and preserves money and rate", ::LoanApplicationScenarioContext) {
            Given("an application for the product family") {
                result = service.applyForLoan(request(product, income = "30000000", amount = "1000000"))
            }
            When("the application is approved with fractional money and rate") {
                secondResult = service.approveApplication(result!!.id!!, BigDecimal("987654.32"), BigDecimal("4.125"))
                approvalCommand = data.approvalCommands.single()
            }
            Then("the family money rate and approved state are preserved") {
                check(approvalCommand!!.loanType == expected)
                check(approvalCommand!!.principalAmount == BigDecimal("987654.32"))
                check(approvalCommand!!.interestRate == BigDecimal("4.125"))
                check(secondResult!!.status == ApplicationStatus.APPROVED.description)
            }
        }
    }

    Scenario("reject preserves reason and approval missing fails", ::LoanApplicationScenarioContext) {
        Given("a submitted application") { result = service.applyForLoan(request()) }
        When("the application is rejected and a missing application is approved") {
            secondResult = service.rejectApplication(result!!.id!!, "policy")
            failure = runCatching {
                service.approveApplication(999, BigDecimal.ONE, BigDecimal.ONE)
            }.exceptionOrNull()
        }
        Then("the rejection reason is preserved and missing approval fails") {
            check(secondResult!!.rejectionReason == "policy")
            check(failure is NoSuchElementException)
        }
    }
}
