package com.socoolheeya.bluebank.card.data.domain.result

import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardProductType
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardStatus
import com.socoolheeya.bluebank.card.data.domain.CardEnums.CardType
import com.socoolheeya.bluebank.card.data.domain.entity.Card
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CardResult(
    val id: Long?,
    val cardNumber: String,
    val cardNumberMasked: String,
    val customerId: Long,
    val accountId: Long,
    val cardType: CardType,
    val productType: CardProductType,
    val cardholderName: String,
    val issueDate: LocalDate,
    val expiryDate: LocalDate,
    val status: CardStatus,
    val dailyLimit: BigDecimal,
    val monthlyLimit: BigDecimal,
    val dailyUsed: BigDecimal,
    val monthlyUsed: BigDecimal,
    val designCode: String,
    val customText: String?,
    val hasTransitCard: Boolean,
    val hasOverseasUsage: Boolean,
    val isEnabled: Boolean,
    val creditLimit: BigDecimal?,
    val availableCredit: BigDecimal?,
    val annualFee: BigDecimal,
    val nextPaymentDate: LocalDate?,
    val partnerCompany: String?,
    val moimAccountId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(card: Card): CardResult {
            return CardResult(
                id = card.id,
                cardNumber = card.cardNumber,
                cardNumberMasked = card.cardNumberMasked,
                customerId = card.customerId,
                accountId = card.accountId,
                cardType = card.cardType,
                productType = card.productType,
                cardholderName = card.cardholderName,
                issueDate = card.issueDate,
                expiryDate = card.expiryDate,
                status = card.status,
                dailyLimit = card.dailyLimit,
                monthlyLimit = card.monthlyLimit,
                dailyUsed = card.dailyUsed,
                monthlyUsed = card.monthlyUsed,
                designCode = card.designCode,
                customText = card.customText,
                hasTransitCard = card.hasTransitCard,
                hasOverseasUsage = card.hasOverseasUsage,
                isEnabled = card.isEnabled,
                creditLimit = card.creditLimit,
                availableCredit = card.availableCredit,
                annualFee = card.annualFee,
                nextPaymentDate = card.nextPaymentDate,
                partnerCompany = card.partnerCompany,
                moimAccountId = card.moimAccountId,
                createdAt = card.createdAt,
                updatedAt = card.updatedAt
            )
        }
    }
}
