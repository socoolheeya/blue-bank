package com.socoolheeya.bluebank.card.dto

import com.socoolheeya.bluebank.card.data.domain.result.CardResult
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object CardDto {

    data class Response(
        val id: Long?,
        val cardNumberMasked: String,
        val customerId: Long,
        val accountId: Long,
        val cardType: String,
        val productType: String,
        val cardholderName: String,
        val issueDate: LocalDate,
        val expiryDate: LocalDate,
        val status: String,
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
        val createdAt: LocalDateTime
    ) {
        companion object {
            fun from(result: CardResult): Response {
                return Response(
                    id = result.id,
                    cardNumberMasked = result.cardNumberMasked,
                    customerId = result.customerId,
                    accountId = result.accountId,
                    cardType = result.cardType.description,
                    productType = result.productType.description,
                    cardholderName = result.cardholderName,
                    issueDate = result.issueDate,
                    expiryDate = result.expiryDate,
                    status = result.status.description,
                    dailyLimit = result.dailyLimit,
                    monthlyLimit = result.monthlyLimit,
                    dailyUsed = result.dailyUsed,
                    monthlyUsed = result.monthlyUsed,
                    designCode = result.designCode,
                    customText = result.customText,
                    hasTransitCard = result.hasTransitCard,
                    hasOverseasUsage = result.hasOverseasUsage,
                    isEnabled = result.isEnabled,
                    creditLimit = result.creditLimit,
                    availableCredit = result.availableCredit,
                    annualFee = result.annualFee,
                    nextPaymentDate = result.nextPaymentDate,
                    createdAt = result.createdAt
                )
            }
        }
    }

    data class ToggleUsageRequest(
        val enabled: Boolean
    )
}