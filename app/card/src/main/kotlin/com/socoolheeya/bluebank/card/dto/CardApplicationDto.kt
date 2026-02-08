package com.socoolheeya.bluebank.card.dto

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object CardApplicationDto {

    data class Request(
        val customerId: Long,
        val accountId: Long,
        val cardType: CardType,
        val productType: CardProductType,
        val applicantName: String,
        val residentNumber: String,
        val phoneNumber: String,
        val email: String? = null,
        val address: String,
        val designCode: String,
        val customText: String? = null,
        val requestTransitCard: Boolean = false,
        val requestOverseasUsage: Boolean = true,
        val moimAccountId: Long? = null,
        val annualIncome: BigDecimal? = null,
        val employmentType: String? = null,
        val companyName: String? = null,
        val creditScore: Int? = null,
        val requestedCreditLimit: BigDecimal? = null
    )

    data class Response(
        val id: Long?,
        val customerId: Long,
        val accountId: Long,
        val cardType: String,
        val productType: String,
        val applicantName: String,
        val phoneNumber: String,
        val email: String?,
        val designCode: String,
        val customText: String?,
        val requestTransitCard: Boolean,
        val status: String,
        val approvedCreditLimit: BigDecimal?,
        val rejectionReason: String?,
        val cardId: Long?,
        val appliedAt: LocalDateTime,
        val reviewedAt: LocalDateTime?
    ) {
        companion object {
            fun from(result: CardApplicationResult): Response {
                return Response(
                    id = result.id,
                    customerId = result.customerId,
                    accountId = result.accountId,
                    cardType = result.cardType.description,
                    productType = result.productType.description,
                    applicantName = result.applicantName,
                    phoneNumber = result.phoneNumber,
                    email = result.email,
                    designCode = result.designCode,
                    customText = result.customText,
                    requestTransitCard = result.requestTransitCard,
                    status = result.status.description,
                    approvedCreditLimit = result.approvedCreditLimit,
                    rejectionReason = result.rejectionReason,
                    cardId = result.cardId,
                    appliedAt = result.appliedAt,
                    reviewedAt = result.reviewedAt
                )
            }
        }
    }

    data class IssueResponse(
        val applicationId: Long,
        val cardId: Long,
        val cardNumberMasked: String,
        val expiryDate: LocalDate,
        val message: String
    )
}