package com.socoolheeya.bluebank.account.data.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 모임 정보 (모임통장용)
 * 모임의 메타데이터 및 회차 정보
 */
@Entity
class GroupMeeting(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var accountId: Long, // 모임통장 계좌 ID

    var meetingName: String, // 모임 이름

    var description: String? = null, // 모임 설명

    var maxMembers: Int, // 최대 멤버 수

    var currentRound: Int = 0, // 현재 회차

    var contributionAmount: BigDecimal, // 회차당 회비

    @Enumerated(EnumType.STRING)
    var status: MeetingStatus = MeetingStatus.ACTIVE, // 모임 상태

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var closedAt: LocalDateTime? = null
) {
    enum class MeetingStatus {
        ACTIVE,      // 진행중
        PAUSED,      // 일시중지
        COMPLETED,   // 완료
        CANCELLED    // 취소
    }
}