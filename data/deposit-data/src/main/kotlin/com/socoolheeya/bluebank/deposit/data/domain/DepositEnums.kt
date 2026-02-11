package com.socoolheeya.bluebank.deposit.data.domain

object DepositEnums {

    /**
     * 예금/적금 상품 유형
     */
    enum class DepositProductType(val description: String) {
        FIXED_DEPOSIT("정기예금"),
        FREE_SAVINGS("자유적금"),
        COINBOX("저금통"),
        WEEKS_26_SAVINGS("26주적금"),
        M1_SAVINGS("한달적금"),
        CHILD_SAVINGS("우리아이적금")
    }

    /**
     * 예금/적금 상태
     */
    enum class DepositStatus(val description: String) {
        PENDING("대기"),
        ACTIVE("활성"),
        MATURED("만기"),
        TERMINATED("해지"),
        SUSPENDED("정지")
    }

    /**
     * 입출금 거래 유형
     */
    enum class DepositTransactionType(val description: String) {
        DEPOSIT("입금"),
        WITHDRAWAL("출금"),
        INTEREST_PAYMENT("이자 지급"),
        EARLY_WITHDRAWAL("중도인출"),
        MATURITY("만기 지급"),
        TERMINATION("해지 지급"),
        AUTO_TRANSFER("자동이체"),
        SPARE_CHANGE("잔돈 모으기"),
        AI_SAVINGS("AI 자동저축"),
        CASHBACK("캐시백")
    }

    /**
     * 기간 단위
     */
    enum class PeriodUnit(val description: String) {
        DAY("일"),
        WEEK("주"),
        MONTH("월")
    }
}
