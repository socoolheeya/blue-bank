package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.client.AccountClient
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.command.LoanApplicationCommand
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.service.LoanApplicationDataService
import com.socoolheeya.bluebank.loan.dto.LoanApplicationDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class LoanApplicationService(
    private val loanApplicationDataService: LoanApplicationDataService,
    private val accountClient: AccountClient,
    private val creditScoreService: CreditScoreService
) {

    fun applyForLoan(request: LoanApplicationDto.Request): LoanApplicationDto.Response {
        // 1. 신용점수 조회
        val creditScore = creditScoreService.getCreditScore(request.customerId)

        // 2. 계좌 검증
        val accountValidation = accountClient.validateAccount(request.accountId)
        require(accountValidation.isValid) { "유효하지 않은 계좌입니다" }

        // 3. 대출 가능 여부 검증
        validateLoanEligibility(request, creditScore.score)

        // 4. 신청서 제출
        val command = LoanApplicationCommand.Submit(
            customerId = request.customerId,
            requestedAmount = request.amount,
            requestedTerm = request.term,
            productType = request.productType,
            repaymentMethod = request.repaymentMethod,
            annualIncome = request.annualIncome,
            employmentType = request.employmentType,
            employmentPeriodMonths = request.employmentPeriodMonths,
            companyName = request.companyName,
            creditScore = creditScore.score,
            existingLoanCount = request.existingLoanCount,
            totalExistingDebt = request.totalExistingDebt,
            hasDelayHistory = request.hasDelayHistory
        )

        val result = loanApplicationDataService.submitApplication(command)

        return LoanApplicationDto.Response.from(result)
    }

    fun getApplication(applicationId: Long): LoanApplicationDto.Response {
        val result = loanApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId")

        return LoanApplicationDto.Response.from(result)
    }

    fun getApplicationsByCustomerId(customerId: Long): List<LoanApplicationDto.Response> {
        val results = loanApplicationDataService.getApplicationsByCustomerId(customerId)
        return results.map { LoanApplicationDto.Response.from(it) }
    }

    fun approveApplication(
        applicationId: Long,
        approvedAmount: BigDecimal,
        approvedRate: BigDecimal
    ): LoanApplicationDto.Response {
        // 신청서 조회
        val application = loanApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId")

        // Loan 생성을 위한 Command
        val loanNumber = generateLoanNumber()
        val loanCommand = LoanCommand.Create(
            loanNumber = loanNumber,
            customerId = application.customerId,
            accountId = 0L, // 실행 시 설정
            loanType = determineLoanType(application.productType),
            productType = application.productType,
            principalAmount = approvedAmount,
            interestRate = approvedRate,
            rateType = LoanEnums.RateType.VARIABLE,
            loanTerm = application.requestedTerm,
            repaymentMethod = application.repaymentMethod,
            creditScore = application.creditScore
        )

        val result = loanApplicationDataService.approveApplication(
            applicationId = applicationId,
            approvedAmount = approvedAmount,
            approvedRate = approvedRate,
            loanCommand = loanCommand
        )

        return LoanApplicationDto.Response.from(result)
    }

    fun rejectApplication(applicationId: Long, reason: String): LoanApplicationDto.Response {
        val result = loanApplicationDataService.rejectApplication(applicationId, reason)
        return LoanApplicationDto.Response.from(result)
    }

    private fun validateLoanEligibility(request: LoanApplicationDto.Request, creditScore: Int) {
        // 신용점수 기준 검증
        val minScore = getMinimumCreditScore(request.productType)
        require(creditScore >= minScore) {
            "최소 신용점수 미달: 필요 ${minScore}점, 현재 ${creditScore}점"
        }

        // 소득 기준 검증
        val minIncome = getMinimumIncome(request.productType)
        require(request.annualIncome >= minIncome) {
            "최소 연소득 미달: 필요 ${minIncome}원"
        }

        // DSR 검증
        val dsr = calculateDSR(request.annualIncome, request.totalExistingDebt, request.amount)
        require(dsr <= BigDecimal("40")) {
            "DSR 한도 초과: 현재 ${dsr}%, 최대 40%"
        }
    }

    private fun calculateDSR(
        annualIncome: BigDecimal,
        existingDebt: BigDecimal,
        newLoanAmount: BigDecimal
    ): BigDecimal {
        // 간단한 DSR 계산 (실제로는 더 복잡)
        val totalDebt = existingDebt.add(newLoanAmount)
        return totalDebt.divide(annualIncome, 2, RoundingMode.HALF_UP).multiply(BigDecimal("100"))
    }

    private fun getMinimumCreditScore(productType: LoanEnums.ProductType): Int {
        return when (productType) {
            LoanEnums.ProductType.EMERGENCY -> 600
            LoanEnums.ProductType.CREDIT_LINE -> 700
            LoanEnums.ProductType.GENERAL_CREDIT -> 700
            LoanEnums.ProductType.MID_CREDIT -> 500
            LoanEnums.ProductType.HF_MORTGAGE -> 271
            else -> 600
        }
    }

    private fun getMinimumIncome(productType: LoanEnums.ProductType): BigDecimal {
        return when (productType) {
            LoanEnums.ProductType.EMERGENCY -> BigDecimal("12000000")
            LoanEnums.ProductType.CREDIT_LINE -> BigDecimal("30000000")
            LoanEnums.ProductType.GENERAL_CREDIT -> BigDecimal("25000000")
            else -> BigDecimal("15000000")
        }
    }

    private fun determineLoanType(productType: LoanEnums.ProductType): LoanEnums.LoanType {
        return when (productType) {
            LoanEnums.ProductType.EMERGENCY, LoanEnums.ProductType.CREDIT_LINE, LoanEnums.ProductType.GENERAL_CREDIT,
            LoanEnums.ProductType.MID_CREDIT, LoanEnums.ProductType.NEW_HOPE, LoanEnums.ProductType.TOGETHER -> LoanEnums.LoanType.CREDIT

            LoanEnums.ProductType.MORTGAGE, LoanEnums.ProductType.HF_MORTGAGE, LoanEnums.ProductType.LEASE,
            LoanEnums.ProductType.AUTO_LOAN, LoanEnums.ProductType.AUTO_LEASE -> LoanEnums.LoanType.SECURED

            LoanEnums.ProductType.CREDIT_REFINANCE, LoanEnums.ProductType.MORTGAGE_REFINANCE,
            LoanEnums.ProductType.LEASE_REFINANCE -> LoanEnums.LoanType.REFINANCE
        }
    }

    private fun generateLoanNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "LN$timestamp$random"
    }
}