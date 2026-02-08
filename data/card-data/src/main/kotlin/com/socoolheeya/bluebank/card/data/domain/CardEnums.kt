package com.socoolheeya.bluebank.card.data.domain

object CardEnums {

    enum class CardType(val description: String) {
        DEBIT("체크카드"),
        CREDIT("신용카드")
    }

    enum class CardProductType(val description: String) {
        // 체크카드
        FRIENDS_CHECK("프렌즈 체크카드"),
        MOIM_CHECK("모임 체크카드"),

        // 신용카드
        JUPJUP_CREDIT("줍줍 신용카드"),

        // 카드 플랫폼 제휴카드
        SHINHAN_CREDIT("신한카드"),
        KB_CREDIT("KB국민카드"),
        SAMSUNG_CREDIT("삼성카드"),
        LOTTE_CREDIT("롯데카드"),
        HYUNDAI_CREDIT("현대카드")
    }

    enum class CardStatus(val description: String) {
        PENDING("발급 대기"),
        ISSUED("발급 완료"),
        ACTIVE("활성화"),
        SUSPENDED("정지"),
        LOST("분실"),
        EXPIRED("만료"),
        TERMINATED("해지")
    }

    enum class CardApplicationStatus(val description: String) {
        SUBMITTED("신청 완료"),
        UNDER_REVIEW("심사 중"),
        APPROVED("승인"),
        REJECTED("거절"),
        ISSUED("발급 완료")
    }

    enum class TransactionType(val description: String) {
        PURCHASE("일반 결제"),
        REFUND("환불"),
        CANCELLATION("취소"),
        CASH_ADVANCE("현금 서비스")
    }

    enum class TransactionStatus(val description: String) {
        PENDING("승인 대기"),
        APPROVED("승인 완료"),
        REJECTED("승인 거절"),
        CANCELLED("취소"),
        SETTLED("정산 완료")
    }

    enum class BenefitType(val description: String) {
        CASHBACK("캐시백"),
        DISCOUNT("할인"),
        POINTS("포인트 적립"),
        SUBSCRIPTION("구독 서비스"),
        FREE_SERVICE("무료 서비스")
    }

    enum class BenefitStatus(val description: String) {
        ACTIVE("진행 중"),
        EXPIRED("종료"),
        SUSPENDED("일시 중단")
    }

    enum class CashbackType(val description: String) {
        STANDARD("일반 캐시백"),
        RANDOM("랜덤 캐시백"),
        BONUS("보너스 캐시백")
    }

    enum class CashbackStatus(val description: String) {
        EARNED("적립"),
        PAID("지급 완료"),
        CANCELLED("취소")
    }

    enum class StatementStatus(val description: String) {
        PENDING("미납"),
        PAID("납부 완료"),
        OVERDUE("연체"),
        PARTIAL_PAID("부분 납부")
    }
}
