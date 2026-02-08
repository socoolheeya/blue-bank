package com.socoolheeya.bluebank.card.data.service

import com.socoolheeya.bluebank.card.data.domain.command.CardApplicationCommand
import com.socoolheeya.bluebank.card.data.domain.command.CardCommand
import com.socoolheeya.bluebank.card.data.domain.result.CardApplicationResult
import com.socoolheeya.bluebank.card.data.repository.CardApplicationRepository
import com.socoolheeya.bluebank.card.data.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class CardApplicationDataService(
    private val cardApplicationRepository: CardApplicationRepository,
    private val cardRepository: CardRepository
) {

    @Transactional
    fun submitApplication(command: CardApplicationCommand.Submit): CardApplicationResult {
        val application = command.toEntity()
        val savedApplication = cardApplicationRepository.save(application)
        return CardApplicationResult.from(savedApplication)
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        creditLimit: BigDecimal?,
        cardCommand: CardCommand.Create
    ): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        // 1. Card 생성
        val card = cardCommand.toEntity()
        val savedCard = cardRepository.save(card)

        // 2. Application 승인 처리
        application.approve(creditLimit, savedCard.id!!)
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun rejectApplication(applicationId: Long, reason: String): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.reject(reason)
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun markAsIssued(applicationId: Long, cardId: Long): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.markAsIssued()
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional
    fun startReview(applicationId: Long): CardApplicationResult {
        val application = cardApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("신청서를 찾을 수 없습니다: $applicationId") }

        application.startReview()
        val updatedApplication = cardApplicationRepository.save(application)

        return CardApplicationResult.from(updatedApplication)
    }

    @Transactional(readOnly = true)
    fun getApplication(applicationId: Long): CardApplicationResult? {
        return cardApplicationRepository.findById(applicationId)
            .map { CardApplicationResult.from(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getApplicationsByCustomerId(customerId: Long): List<CardApplicationResult> {
        return cardApplicationRepository.findByCustomerId(customerId)
            .map { CardApplicationResult.from(it) }
    }
}