package com.socoolheeya.bluebank.loan.service

import org.springframework.stereotype.Service

@Service
class CreditScoreService {

    data class CreditScoreResult(
        val score: Int,
        val grade: String,
        val agency: String
    )

    /**
     * 신용점수 조회 (Mock 구현)
     * TODO: 실제 신용평가 기관 API 연동 필요 (NICE, KCB 등)
     */
    fun getCreditScore(customerId: Long): CreditScoreResult {
        // Mock 데이터 반환
        // 실제 구현 시 외부 API 호출
        return CreditScoreResult(
            score = 750,
            grade = "3등급",
            agency = "NICE"
        )
    }
}