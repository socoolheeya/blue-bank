package com.socoolheeya.bluebank.card.data.domain.command

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.entity.Card
import java.math.BigDecimal
import java.time.LocalDate

sealed interface CardCommand {

    data class Create(
        val cardNumber: String,
        val cardNumberMasked: String,
        val customerId: Long,
        val accountId: Long,
        val cardType: CardType,
        val productType: CardProductType,
        val cardholderName: String,
        val expiryDate: LocalDate,
        val cvv: String,
        val dailyLimit: BigDecimal,
        val monthlyLimit: BigDecimal,
        val designCode: String,
        val customText: String? = null,
        val hasTransitCard: Boolean = false,
        val hasOverseasUsage: Boolean = true,
        val creditLimit: BigDecimal? = null,
        val annualFee: BigDecimal = BigDecimal.ZERO,
        val partnerCompany: String? = null,
        val moimAccountId: Long? = null,
        val applicationId: Long? = null,
        val issueFee: BigDecimal = BigDecimal.ZERO
    ) : CardCommand {
        fun toEntity(): Card {
            return Card(
                cardNumber = cardNumber,
                cardNumberMasked = cardNumberMasked,
                customerId = customerId,
                accountId = accountId,
                cardType = cardType,
                productType = productType,
                cardholderName = cardholderName,
                issueDate = LocalDate.now(),
                expiryDate = expiryDate,
                cvv = cvv,
                status = CardStatus.ISSUED,
                dailyLimit = dailyLimit,
                monthlyLimit = monthlyLimit,
                designCode = designCode,
                customText = customText,
                hasTransitCard = hasTransitCard,
                hasOverseasUsage = hasOverseasUsage,
                creditLimit = creditLimit,
                availableCredit = creditLimit,
                annualFee = annualFee,
                partnerCompany = partnerCompany,
                moimAccountId = moimAccountId,
                applicationId = applicationId,
                issueFee = issueFee
            )
        }
    }

    data class Activate(val cardId: Long) : CardCommand

    data class Suspend(val cardId: Long, val reason: String) : CardCommand

    data class Terminate(val cardId: Long) : CardCommand

    data class ToggleUsage(val cardId: Long, val enabled: Boolean) : CardCommand
}