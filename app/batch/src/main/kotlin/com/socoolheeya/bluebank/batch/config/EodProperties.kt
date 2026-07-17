package com.socoolheeya.bluebank.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

@ConfigurationProperties("blue-bank.batch.eod")
data class EodProperties(
    var chunkSize: Int = 100,
    var skipLimit: Int = 100,
    var scheduleEnabled: Boolean = false,
    var cron: String = "0 0 1 * * *",
    var accountMaintenanceFee: BigDecimal = BigDecimal.ZERO,
    var domesticCardSettlementRate: BigDecimal = BigDecimal("0.001"),
    var overseasCardSettlementRate: BigDecimal = BigDecimal("0.003")
)
