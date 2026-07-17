package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanStatus
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.LoanType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.ProductType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RateType
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentMethod
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums.RepaymentType
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.testing.*
import de.infix.testBalloon.framework.core.testSuite
import java.math.BigDecimal

private fun loan(customer: Long = 42) = LoanCommand.Create("LN-$customer", customer, 7, LoanType.CREDIT,
    ProductType.GENERAL_CREDIT, BigDecimal("1000.50"), BigDecimal("3.125"), RateType.VARIABLE, 12,
    RepaymentMethod.EQUAL_INSTALLMENT)

val loanServiceScenarios by testSuite("Loan service scenarios") {
    test("get list and missing loan cover public queries") {
        val data = FakeLoanDataService(); val added = data.add(loan()); data.add(loan(99))
        val service = LoanService(data, FakeAccountClient())
        check(service.getLoan(added.id!!).principalAmount == BigDecimal("1000.50"))
        check(service.getLoansByCustomerId(42).single().id == added.id)
        check(runCatching { service.getLoan(404) }.exceptionOrNull() is NoSuchElementException)
    }
    test("execute maps loan and account identifiers") {
        val data = FakeLoanDataService(); val added = data.add(loan()); val service = LoanService(data, FakeAccountClient())
        check(service.executeLoan(added.id!!).status == LoanStatus.ACTIVE.description)
        check(data.executeCommands.single() == LoanCommand.Execute(added.id!!, 7))
    }
    test("repay preserves fractional amount and maps early repayment") {
        val data = FakeLoanDataService(); val added = data.add(loan()); val service = LoanService(data, FakeAccountClient())
        val result = service.repayLoan(added.id!!, BigDecimal("0.01"))
        check(data.repayCommands.single() == LoanCommand.Repay(added.id!!, BigDecimal("0.01"), RepaymentType.EARLY))
        check(result.outstandingBalance == BigDecimal("1000.49"))
        check(runCatching { service.executeLoan(404) }.exceptionOrNull() is NoSuchElementException)
        check(runCatching { service.repayLoan(404, BigDecimal.ONE) }.exceptionOrNull() is NoSuchElementException)
    }
}
