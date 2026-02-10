package com.socoolheeya.bluebank.card.service

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.service.CardApplicationDataService
import com.socoolheeya.bluebank.card.data.service.CardDataService
import com.socoolheeya.bluebank.card.dto.CardApplicationDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.random.Random

@Service
class CardApplicationService(
    private val cardApplicationDataService: CardApplicationDataService,
    private val cardDataService: CardDataService
) {

    fun applyForCard(request: CardApplicationDto.Request): CardApplicationDto.Response {
        // 1. 계좌 검증 (TODO: AccountClient 연동)

        // 2. 모임체크카드인 경우 모임통장 검증
        if (request.productType == CardProductType.MOIM_CHECK) {
            require(request.moimAccountId != null) { "모임통장 ID가 필요합니다" }
        }

        // 3. 신용카드인 경우 신용점수 조회 및 검증
        if (request.cardType == CardType.CREDIT) {
            // TODO: 신용점수 조회 및 검증
        }

        // 4. 신청서 제출
        val command = CardApplicationCommand.Submit(
            customerId = request.customerId,
            accountId = request.accountId,
            cardType = request.cardType,
            productType = request.productType,
            applicantName = request.applicantName,
            residentNumber = request.residentNumber,
            phoneNumber = request.phoneNumber,
            email = request.email,
            address = request.address,
            designCode = request.designCode,
            customText = request.customText,
            requestTransitCard = request.requestTransitCard,
            requestOverseasUsage = request.requestOverseasUsage,
            moimAccountId = request.moimAccountId,
            annualIncome = request.annualIncome,
            employmentType = request.employmentType,
            companyName = request.companyName,
            creditScore = request.creditScore,
            requestedCreditLimit = request.requestedCreditLimit
        )

        val result = cardApplicationDataService.submitApplication(command)

        return CardApplicationDto.Response.from(result)
    }

    fun issueCard(applicationId: Long): CardApplicationDto.IssueResponse {
        // 1. 신청서 조회
        val application = cardApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId")

        require(application.status == CardApplicationStatus.APPROVED) {
            "승인된 신청서만 발급 가능합니다"
        }

        // 2. 카드 생성
        val cardNumber = generateCardNumber()
        val cardNumberMasked = maskCardNumber(cardNumber)
        val cvv = generateCVV()
        val expiryDate = LocalDate.now().plusYears(5)

        // 3. 한도 설정
        val (dailyLimit, monthlyLimit) = determineLimits(application.productType)

        val cardCommand = CardCommand.Create(
            cardNumber = cardNumber,
            cardNumberMasked = cardNumberMasked,
            customerId = application.customerId,
            accountId = application.accountId,
            cardType = application.cardType,
            productType = application.productType,
            cardholderName = application.applicantName,
            expiryDate = expiryDate,
            cvv = cvv,
            dailyLimit = dailyLimit,
            monthlyLimit = monthlyLimit,
            designCode = application.designCode,
            customText = application.customText,
            hasTransitCard = application.requestTransitCard,
            hasOverseasUsage = true,
            creditLimit = application.approvedCreditLimit,
            annualFee = getAnnualFee(application.productType),
            moimAccountId = application.accountId,
            applicationId = applicationId,
            issueFee = getIssueFee(application.productType)
        )

        val cardResult = cardDataService.createCard(cardCommand)

        // 4. 신청서 상태 업데이트
        cardApplicationDataService.markAsIssued(applicationId, cardResult.id!!)

        return CardApplicationDto.IssueResponse(
            applicationId = applicationId,
            cardId = cardResult.id!!,
            cardNumberMasked = cardNumberMasked,
            expiryDate = expiryDate,
            message = "카드가 발급되었습니다. 3-5일 내 배송됩니다."
        )
    }

    fun getApplication(applicationId: Long): CardApplicationDto.Response {
        val result = cardApplicationDataService.getApplication(applicationId)
            ?: throw NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId")

        return CardApplicationDto.Response.from(result)
    }

    fun getApplicationsByCustomerId(customerId: Long): List<CardApplicationDto.Response> {
        val results = cardApplicationDataService.getApplicationsByCustomerId(customerId)
        return results.map { CardApplicationDto.Response.from(it) }
    }

    private fun generateCardNumber(): String {
        // 실제로는 카드사 BIN + 순번 + 체크디지트
        // 5234: MasterCard BIN
        return "5234${Random.nextInt(1000, 9999)}${Random.nextInt(1000, 9999)}${Random.nextInt(1000, 9999)}"
    }

    private fun maskCardNumber(cardNumber: String): String {
        if (cardNumber.length < 16) return cardNumber
        return "${cardNumber.substring(0, 4)}-****-****-${cardNumber.substring(12)}"
    }

    private fun generateCVV(): String {
        return Random.nextInt(100, 999).toString()
    }

    private fun determineLimits(productType: CardProductType): Pair<BigDecimal, BigDecimal> {
        return when (productType) {
            CardProductType.FRIENDS_CHECK -> Pair(
                BigDecimal("5000000"),   // 일 500만원
                BigDecimal("20000000")   // 월 2000만원
            )
            CardProductType.MOIM_CHECK -> Pair(
                BigDecimal("6000000"),   // 일 600만원
                BigDecimal("20000000")   // 월 2000만원
            )
            else -> Pair(
                BigDecimal("3000000"),   // 일 300만원
                BigDecimal("10000000")   // 월 1000만원
            )
        }
    }

    private fun getAnnualFee(productType: CardProductType): BigDecimal {
        return when (productType) {
            CardProductType.FRIENDS_CHECK -> BigDecimal.ZERO
            CardProductType.MOIM_CHECK -> BigDecimal.ZERO
            CardProductType.JUPJUP_CREDIT -> BigDecimal("18000")
            else -> BigDecimal.ZERO
        }
    }

    private fun getIssueFee(productType: CardProductType): BigDecimal {
        return when (productType) {
            CardProductType.MOIM_CHECK -> BigDecimal("2000")
            else -> BigDecimal.ZERO
        }
    }
}