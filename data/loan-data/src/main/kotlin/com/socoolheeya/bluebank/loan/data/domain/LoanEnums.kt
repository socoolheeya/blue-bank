package com.socoolheeya.bluebank.loan.data.domain

object LoanEnums {

    enum class LoanType(val description: String) {
        CREDIT("신용대출"),
        SECURED("담보대출"),
        REFINANCE("대환대출")
    }

    enum class ProductType(val description: String) {
        // 신용대출
        EMERGENCY("비상금대출"),
        CREDIT_LINE("마이너스통장"),
        GENERAL_CREDIT("신용대출"),
        MID_CREDIT("중신용대출"),
        NEW_HOPE("새희망홀씨II"),
        TOGETHER("같이대출"),

        // 담보대출
        MORTGAGE("주택담보대출"),
        HF_MORTGAGE("HF 아낌e 보금자리론"),
        LEASE("전월세보증금대출"),
        AUTO_LOAN("자동차담보대출"),
        AUTO_LEASE("리스금융대출"),

        // 대환대출
        CREDIT_REFINANCE("신용대출 갈아타기"),
        MORTGAGE_REFINANCE("주택담보대출 갈아타기"),
        LEASE_REFINANCE("전월세보증금 갈아타기")
    }

    enum class LoanStatus(val description: String) {
        PENDING("심사 중"),
        APPROVED("승인"),
        ACTIVE("실행 중"),
        OVERDUE("연체"),
        SETTLED("완료"),
        REJECTED("거절"),
        CANCELLED("취소")
    }

    enum class ApplicationStatus(val description: String) {
        SUBMITTED("신청 완료"),
        UNDER_REVIEW("심사 중"),
        APPROVED("승인"),
        REJECTED("거절")
    }

    enum class RepaymentMethod(val description: String) {
        LUMP_SUM("만기일시상환"),
        EQUAL_PRINCIPAL("원금균등분할"),
        EQUAL_INSTALLMENT("원리금균등분할"),
        BALLOON("체증식분할상환")
    }

    enum class RepaymentType(val description: String) {
        SCHEDULED("정기 상환"),
        EARLY("중도 상환"),
        FINAL("만기 상환")
    }

    enum class RepaymentStatus(val description: String) {
        SCHEDULED("예정"),
        COMPLETED("완료"),
        OVERDUE("연체"),
        WAIVED("면제")
    }

    enum class RateType(val description: String) {
        FIXED("고정금리"),
        VARIABLE("변동금리")
    }

    enum class CollateralType(val description: String) {
        REAL_ESTATE("부동산"),
        VEHICLE("차량"),
        DEPOSIT("보증금")
    }

    enum class CollateralStatus(val description: String) {
        REGISTERED("등록 완료"),
        RELEASED("해제"),
        FORECLOSED("압류")
    }

    enum class InterestPaymentStatus(val description: String) {
        PENDING("미납"),
        PAID("납부 완료"),
        OVERDUE("연체")
    }
}