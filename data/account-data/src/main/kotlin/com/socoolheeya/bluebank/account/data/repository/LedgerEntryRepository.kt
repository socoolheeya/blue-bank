package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.LedgerEntry
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface LedgerEntryRepository : JpaRepository<LedgerEntry, Long> {
    fun findByOccurredAtBetween(start: LocalDateTime, end: LocalDateTime): List<LedgerEntry>
    fun findByAccountIdAndOccurredAtBetween(accountId: Long, start: LocalDateTime, end: LocalDateTime): List<LedgerEntry>
    fun findByAccountId(accountId: Long, pageable: Pageable): Page<LedgerEntry>
    fun findByAccountIdAndOccurredAtBetween(accountId: Long, start: LocalDateTime, end: LocalDateTime, pageable: Pageable): Page<LedgerEntry>
    fun findByAccountIdAndType(accountId: Long, type: AccountEnums.EntryType, pageable: Pageable): Page<LedgerEntry>
    fun findByAccountIdAndSectionId(accountId: Long, sectionId: Long, pageable: Pageable): Page<LedgerEntry>
}
