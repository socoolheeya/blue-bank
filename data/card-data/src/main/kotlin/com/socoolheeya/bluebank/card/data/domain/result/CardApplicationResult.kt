package com.socoolheeya.bluebank.card.data.domain.result

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardApplicationStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.entity.CardApplication
import java.math.BigDecimal
import java.time.LocalDateTime

data class CardApplicationResult(
    val id: Long?,
    val customerId: Long,
    val accountId: Long,
    val cardType: CardType,
    val productType: CardProductType,
    val applicantName: String,
    val phoneNumber: String,
    val email: String?,
    val designCode: String,
    val customText: String?,
    val requestTransitCard: Boolean,
    val status: CardApplicationStatus,
    val approvedCreditLimit: BigDecimal?,
    val rejectionReason: String?,
    val cardId: Long?,
    val appliedAt: LocalDateTime,
    val reviewedAt: LocalDateTime?
) {
    companion object {
        fun from(application: CardApplication): CardApplicationResult {
            return CardApplicationResult(
                id = application.id,
                customerId = application.customerId,
                accountId = application.accountId,
                cardType = application.cardType,
                productType = application.productType,
                applicantName = application.applicantName,
                phoneNumber = application.phoneNumber,
                email = application.email,
                designCode = application.designCode,
                customText = application.customText,
                requestTransitCard = application.requestTransitCard,
                status = application.status,
                approvedCreditLimit = application.approvedCreditLimit,
                rejectionReason = application.rejectionReason,
                cardId = application.cardId,
                appliedAt = application.appliedAt,
                reviewedAt = application.reviewedAt
            )
        }
    }
}
