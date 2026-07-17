package com.socoolheeya.bluebank.batch.config

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Component
@ConditionalOnProperty(prefix = "blue-bank.batch.eod", name = ["schedule-enabled"], havingValue = "true")
class EodScheduler(
    private val jobOperator: JobOperator,
    private val dailyEodJob: Job
) {
    @Scheduled(cron = "\${blue-bank.batch.eod.cron:0 0 1 * * *}", zone = "Asia/Seoul")
    fun launchPreviousBusinessDate() {
        val businessDate = LocalDate.now(Clock.system(ZoneId.of("Asia/Seoul"))).minusDays(1)
        jobOperator.start(
            dailyEodJob,
            JobParametersBuilder()
                .addString("businessDate", businessDate.toString())
                .addLong("scheduledAt", System.currentTimeMillis(), false)
                .toJobParameters()
        )
    }
}
