package com.socoolheeya.bluebank.loan

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = [
    "com.socoolheeya.bluebank"
])
@ConfigurationPropertiesScan(basePackages = [
    "com.socoolheeya.bluebank"
])
@EnableDiscoveryClient
@EnableFeignClients(basePackages = [
    "com.socoolheeya.bluebank.loan.client"
])
class LoanApplication

fun main(args: Array<String>) {
    runApplication<LoanApplication>(*args)
}
