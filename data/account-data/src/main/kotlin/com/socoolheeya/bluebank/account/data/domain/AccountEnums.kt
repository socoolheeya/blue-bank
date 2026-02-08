package com.socoolheeya.bluebank.account.data.domain

class AccountEnums {
    enum class AccountStatus(
        val description: String
    ) {
        PENDING("대기중"),
        ACTIVE("정상"),
        DORMANT("휴면"),
        FROZEN("동결"),
        CLOSED("해지");
    }

    enum class AccountType {
        CHECKING,
        SAVINGS,
        TIME_DEPOSIT,
        SAFEBOX
    }

    enum class ProductType {
        BASIC_CHECKING,
        GROUP_MEETING,
        CHILD_ACCOUNT,
        RECORD_BOOK,
        SAFEBOX
    }

    enum class RuleFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    enum class DeviceType {
        MOBILE,
        DESKTOP,
        BOTH
    }

    enum class TransactionType {
        DEPOSIT,
        WITHDRAWAL
    }

    enum class TransactionStatus {
        SUCCESS,
        FAILURE
    }

    enum class TransactionCategory {
        DEPOSIT,
        WITHDRAWAL
    }

    enum class TransactionFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }

    enum class TransactionFrequencyUnit {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    enum class TransactionFrequencyInterval {
        EVERY,
        ONCE
    }

    enum class TransactionFrequencyIntervalUnit {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    enum class TransactionFrequencyIntervalValue {
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
    }

    enum class HolderRole {
        PRIMARY,
        JOINT,
        PARENT,
        CHILD,
        MEETING_OWNER,
        MEETING_MEMBER
    }

    enum class HoldStatus {
        ACTIVE, // 보류 중
        RELEASED, //해재됨
        CONVERTED // 실제 출금으로 전환
    }

    enum class EntryType {
        DEPOSIT,    // 입금
        WITHDRAWAL, // 출금
        INTEREST,   // 이자
        TRANSFER    // 이체
    }
}