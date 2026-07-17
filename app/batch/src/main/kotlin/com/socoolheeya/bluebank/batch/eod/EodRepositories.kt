package com.socoolheeya.bluebank.batch.eod

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface EodExecutionRepository : JpaRepository<EodExecution, Long> {
    fun findByBusinessDate(businessDate: LocalDate): EodExecution?
}

interface ScheduledTransferRepository : JpaRepository<ScheduledTransfer, Long> {
    fun findByStatusAndNextExecutionDateLessThanEqual(status: TransferStatus, date: LocalDate): List<ScheduledTransfer>
}

interface ScheduledTransferExecutionRepository : JpaRepository<ScheduledTransferExecution, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}

interface EodAccountingEntryRepository : JpaRepository<EodAccountingEntry, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}

interface ExternalSettlementRepository : JpaRepository<ExternalSettlement, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}

interface LedgerCloseRepository : JpaRepository<LedgerClose, Long> {
    fun existsByBusinessDateAndAccountId(businessDate: LocalDate, accountId: Long): Boolean
}
