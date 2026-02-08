package com.socoolheeya.bluebank.card.data.domain.command

import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import com.socoolheeya.bluebank.card.data.domain.entity.CardApplication
import java.math.BigDecimal

sealed interface CardApplicationCommand {

    data class Submit(
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
    ) : CardApplicationCommand {
        fun toEntity(): CardApplication {
            return CardApplication(
                customerId = customerId,
                accountId = accountId,
                cardType = cardType,
                productType = productType,
                applicantName = applicantName,
                residentNumber = residentNumber,
                phoneNumber = phoneNumber,
                email = email,
                address = address,
                designCode = designCode,
                customText = customText,
                requestTransitCard = requestTransitCard,
                requestOverseasUsage = requestOverseasUsage,
                moimAccountId = moimAccountId,
                annualIncome = annualIncome,
                employmentType = employmentType,
                companyName = companyName,
                creditScore = creditScore,
                requestedCreditLimit = requestedCreditLimit
            )
        }
    }
}