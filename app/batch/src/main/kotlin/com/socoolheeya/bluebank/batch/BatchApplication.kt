package com.socoolheeya.bluebank.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.socoolheeya.bluebank.batch"])
@EntityScan(basePackages = ["com.socoolheeya.bluebank"])
@EnableJpaRepositories(
    basePackages = ["com.socoolheeya.bluebank"],
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator::class
)
@ConfigurationPropertiesScan("com.socoolheeya.bluebank.batch.config")
@EnableScheduling
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
