package com.socoolheeya.bluebank.batch.support

import java.time.LocalDate
import java.time.format.DateTimeParseException

object BusinessDate {
    fun parse(value: String?): LocalDate {
        require(!value.isNullOrBlank()) { "businessDate job parameter is required (YYYY-MM-DD)" }
        return try {
            LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw IllegalArgumentException("businessDate must use YYYY-MM-DD: $value", exception)
        }
    }
}
