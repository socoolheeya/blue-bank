package com.socoolheeya.bluebank.loan.data

import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.command.LoanApplicationCommand
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.domain.entity.CreditScoreHistory
import com.socoolheeya.bluebank.loan.data.repository.*
import com.socoolheeya.bluebank.loan.data.service.*
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootConfiguration @EnableAutoConfiguration @ComponentScan("com.socoolheeya.bluebank.loan.data")
private open class LoanIntegrationApplication

private inline fun withLoanContext(block: (org.springframework.context.ConfigurableApplicationContext) -> Unit) {
    SpringApplicationBuilder(LoanIntegrationApplication::class.java).web(WebApplicationType.NONE).profiles("integration").run().use { context ->
        listOf(RepaymentRepository::class.java, CreditScoreHistoryRepository::class.java, LoanApplicationRepository::class.java,
            LoanRepository::class.java).forEach { context.getBean(it).deleteAll() }
        block(context)
    }
}

private fun application(customer: Long = 42) = LoanApplicationCommand.Submit(customer, BigDecimal("1000000.25"), 12,
    LoanEnums.ProductType.GENERAL_CREDIT, LoanEnums.RepaymentMethod.EQUAL_INSTALLMENT, BigDecimal("30000000"),
    "employee", 24, creditScore = 750)
private fun loan(number: String = "LN-real", customer: Long = 42) = LoanCommand.Create(number, customer, 7,
    LoanEnums.LoanType.CREDIT, LoanEnums.ProductType.GENERAL_CREDIT, BigDecimal("1000.50"), BigDecimal("3.125"),
    LoanEnums.RateType.VARIABLE, 12, LoanEnums.RepaymentMethod.EQUAL_INSTALLMENT, creditScore = 750)

val loanDataIntegration by testSuite("Loan data integration", compartment = { TestCompartment.Sequential }) {
    test("application create queries approval loan creation and rejection persist in H2") {
        withLoanContext { context ->
            val service = context.getBean(LoanApplicationDataService::class.java)
            val first = service.submitApplication(application())
            check(service.getApplication(first.id!!)!!.requestedAmount.compareTo(BigDecimal("1000000.25")) == 0)
            check(service.getApplicationsByCustomerId(42).single().id == first.id)
            val approved = service.approveApplication(first.id!!, BigDecimal("900000.12"), BigDecimal("4.321"), loan("LN-approved"))
            check(approved.status == LoanEnums.ApplicationStatus.APPROVED && approved.loanId != null)
            val createdLoan = context.getBean(LoanRepository::class.java).findById(approved.loanId!!).orElseThrow()
            check(createdLoan.principalAmount == BigDecimal("900000.12"))
            check(createdLoan.outstandingBalance == BigDecimal("900000.12"))
            check(createdLoan.interestRate == BigDecimal("4.321"))
            val second = service.submitApplication(application(77))
            check(service.rejectApplication(second.id!!, "policy").rejectionReason == "policy")
            check(service.getApplication(999999) == null)
        }
    }
    test("loan lifecycle repayment balances and repayment and credit histories use real repositories") {
        withLoanContext { context ->
            val loans = context.getBean(LoanDataService::class.java)
            val created = loans.createLoan(loan())
            check(loans.getLoan(created.id!!)!!.loanNumber == "LN-real")
            check(loans.getLoanByNumber("LN-real")!!.id == created.id && loans.getLoansByCustomerId(42).single().id == created.id)
            check(runCatching { loans.createLoan(loan()) }.exceptionOrNull() is IllegalArgumentException)
            loans.approveLoan(LoanCommand.Approve(created.id!!, "reviewer")); loans.executeLoan(LoanCommand.Execute(created.id!!, 7))
            val repaid = loans.repayLoan(LoanCommand.Repay(created.id!!, BigDecimal("0.25"), LoanEnums.RepaymentType.EARLY))
            check(repaid.outstandingBalance.compareTo(BigDecimal("1000.25")) == 0)

            val repayments = context.getBean(RepaymentDataService::class.java)
            val scheduled = repayments.createRepayment(created.id!!, LoanEnums.RepaymentType.SCHEDULED, BigDecimal("10.25"),
                BigDecimal("1.10"), LocalDate.now().minusDays(2))
            check(scheduled.totalAmount.compareTo(BigDecimal("11.35")) == 0)
            check(repayments.getRepaymentsByLoanId(created.id!!).single().id == scheduled.id)
            check(repayments.getScheduledRepayments(LocalDate.now().minusDays(3), LocalDate.now()).single().id == scheduled.id)
            check(repayments.processRepayment(scheduled.id!!).status == LoanEnums.RepaymentStatus.COMPLETED)

            val histories = context.getBean(CreditScoreHistoryRepository::class.java)
            val history = histories.save(CreditScoreHistory(customerId = 42, loanApplicationId = 123, creditScore = 750,
                creditGrade = "3", scoringAgency = "NICE", scoredAt = LocalDateTime.of(2026,1,1,0,0)))
            check(histories.findByCustomerIdOrderByScoredAtDesc(42).single().id == history.id)
            check(histories.findByLoanApplicationId(123).get().creditScore == 750)
        }
    }
}
