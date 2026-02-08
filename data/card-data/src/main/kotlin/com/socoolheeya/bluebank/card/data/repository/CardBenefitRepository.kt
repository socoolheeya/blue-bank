package com.socoolheeya.bluebank.card.data.repository

import com.socoolheeya.bluebank.card.data.domain.entity.CardBenefit
import com.socoolheeya.bluebank.card.data.domain.CardEnums.*
import org.springframework.data.jpa.repository.JpaRepository

interface CardBenefitRepository : JpaRepository<CardBenefit, Long> {
    fun findByProductType(productType: CardProductType): List<CardBenefit>
    fun findByProductTypeAndStatus(productType: CardProductType, status: BenefitStatus): List<CardBenefit>
    fun findByStatus(status: BenefitStatus): List<CardBenefit>
}