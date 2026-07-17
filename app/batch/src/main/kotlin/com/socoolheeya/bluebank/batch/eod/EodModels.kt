package com.socoolheeya.bluebank.batch.eod

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class EodStatus { STARTED, COMPLETED, FAILED }
enum class TransferStatus { PENDING, COMPLETED, FAILED, DISABLED }
enum class TransferFrequency { ONCE, DAILY, WEEKLY, MONTHLY }
enum class SettlementStatus { PENDING, COMPLETED, FAILED }

@Entity
@Table(name = "eod_execution", uniqueConstraints = [UniqueConstraint(columnNames = ["businessDate"])])
class EodExecution(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var businessDate: LocalDate,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: EodStatus = EodStatus.STARTED,
    @Column(nullable = false) var startedAt: LocalDateTime = LocalDateTime.now(),
    var completedAt: LocalDateTime? = null,
    @Column(nullable = false) var successCount: Long = 0,
    @Column(nullable = false) var failureCount: Long = 0,
    @Column(length = 2000) var errorSummary: String? = null
)

@Entity
@Table(name = "scheduled_transfer")
class ScheduledTransfer(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var sourceAccountId: Long,
    @Column(nullable = false) var destinationAccountId: Long,
    @Column(nullable = false, precision = 19, scale = 2) var amount: BigDecimal,
    @Column(nullable = false) var nextExecutionDate: LocalDate,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var frequency: TransferFrequency = TransferFrequency.ONCE,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: TransferStatus = TransferStatus.PENDING,
    @Column(nullable = false) var retryCount: Int = 0,
    var lastExecutedDate: LocalDate? = null,
    @Column(length = 500) var failureReason: String? = null
)

@Entity
@Table(name = "scheduled_transfer_execution", uniqueConstraints = [UniqueConstraint(columnNames = ["idempotencyKey"])])
class ScheduledTransferExecution(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var transferId: Long,
    @Column(nullable = false) var businessDate: LocalDate,
    @Column(nullable = false, unique = true, length = 120) var idempotencyKey: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: TransferStatus,
    @Column(length = 500) var message: String? = null,
    @Column(nullable = false) var executedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "eod_accounting_entry", uniqueConstraints = [UniqueConstraint(columnNames = ["idempotencyKey"])])
class EodAccountingEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var businessDate: LocalDate,
    @Column(nullable = false, length = 30) var referenceType: String,
    @Column(nullable = false) var referenceId: Long,
    @Column(nullable = false, length = 30) var entryType: String,
    @Column(nullable = false, precision = 19, scale = 2) var baseAmount: BigDecimal,
    @Column(nullable = false, precision = 12, scale = 8) var rate: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var amount: BigDecimal,
    @Column(nullable = false, unique = true, length = 160) var idempotencyKey: String,
    @Column(nullable = false) var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "external_settlement", uniqueConstraints = [UniqueConstraint(columnNames = ["idempotencyKey"])])
class ExternalSettlement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var businessDate: LocalDate,
    @Column(nullable = false, length = 80) var institution: String,
    @Column(nullable = false, length = 30) var settlementType: String,
    @Column(nullable = false, precision = 19, scale = 2) var grossAmount: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var feeAmount: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var netAmount: BigDecimal,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: SettlementStatus = SettlementStatus.PENDING,
    @Column(nullable = false, unique = true, length = 160) var idempotencyKey: String,
    @Column(nullable = false) var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "ledger_close", uniqueConstraints = [UniqueConstraint(columnNames = ["businessDate", "accountId"])])
class LedgerClose(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var businessDate: LocalDate,
    @Column(nullable = false) var accountId: Long,
    @Column(nullable = false, precision = 19, scale = 2) var openingBalance: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var deposits: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var withdrawals: BigDecimal,
    @Column(nullable = false, precision = 19, scale = 2) var closingBalance: BigDecimal,
    @Column(nullable = false) var reconciled: Boolean,
    @Column(length = 500) var errorMessage: String? = null
)
