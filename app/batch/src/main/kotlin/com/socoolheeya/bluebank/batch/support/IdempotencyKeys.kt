package com.socoolheeya.bluebank.batch.support

import java.time.LocalDate

object IdempotencyKeys {
    fun accounting(date: LocalDate, referenceType: String, referenceId: Long, entryType: String): String =
        "$date:$referenceType:$referenceId:$entryType"

    fun transfer(date: LocalDate, transferId: Long): String = "$date:TRANSFER:$transferId"

    fun settlement(date: LocalDate, institution: String, type: String): String = "$date:$institution:$type"
}
