package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ProductType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RateType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentMethod
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentType
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.dto.LoanDto
import com.socoolheeya.bluebank.loan.testing.*
import com.socoolheeya.bluebank.loan.testing.Scenario
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

private fun loan(customer: Long = 42) = LoanCommand.Create("LN-$customer", customer, 7, LoanType.CREDIT,
    ProductType.GENERAL_CREDIT, BigDecimal("1000.50"), BigDecimal("3.125"), RateType.VARIABLE, 12,
    RepaymentMethod.EQUAL_INSTALLMENT)

private class LoanServiceScenarioContext {
    val data = FakeLoanDataService()
    val accounts = FakeAccountClient()
    val service = LoanService(data, accounts)
    var added = data.add(loan())
    var response: LoanDto.Response? = null
    var responses: List<LoanDto.Response> = emptyList()
    var failure: Throwable? = null
    var secondFailure: Throwable? = null
}

val loanServiceScenarios by testSuite("Loan service scenarios") {
    testFixture { LoanServiceScenarioContext() } asContextForEach {
    Scenario("get list and missing loan cover public queries") {
        Given("loans for the requested customer and another customer") {
            data.add(loan(99))
        }
        When("the loan and customer list are queried with a missing lookup") {
            response = service.getLoan(added.id!!)
            responses = service.getLoansByCustomerId(42)
            failure = runCatching { service.getLoan(404) }.exceptionOrNull()
        }
        Then("queries return only matching loans and the missing loan fails") {
            check(response!!.principalAmount == BigDecimal("1000.50"))
            check(responses.single().id == added.id)
            check(failure is NoSuchElementException)
        }
    }
    Scenario("execute maps loan and account identifiers") {
        When("the stored loan is executed") {
            response = service.executeLoan(added.id!!)
        }
        Then("the loan becomes active and both identifiers are mapped") {
            check(response!!.status == LoanStatus.ACTIVE.description)
            check(data.executeCommands.single() == LoanCommand.Execute(added.id!!, 7))
        }
    }
    Scenario("repay preserves fractional amount and missing mutations fail") {
        When("a cent is repaid and missing loans are executed and repaid") {
            response = service.repayLoan(added.id!!, BigDecimal("0.01"))
            failure = runCatching { service.executeLoan(404) }.exceptionOrNull()
            secondFailure = runCatching { service.repayLoan(404, BigDecimal.ONE) }.exceptionOrNull()
        }
        Then("the early repayment preserves precision and both missing mutations fail") {
            check(data.repayCommands.single() == LoanCommand.Repay(added.id!!, BigDecimal("0.01"), RepaymentType.EARLY))
            check(response!!.outstandingBalance == BigDecimal("1000.49"))
            check(failure is NoSuchElementException)
            check(secondFailure is NoSuchElementException)
        }
    }
    }
}
