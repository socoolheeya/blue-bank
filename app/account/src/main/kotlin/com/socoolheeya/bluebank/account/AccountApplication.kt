package com.socoolheeya.bluebank.account

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = [
    "com.socoolheeya.bluebank"
])
@ConfigurationPropertiesScan(basePackages = [
    "com.socoolheeya.bluebank",
])
@EnableFeignClients(basePackages = [
    "com.socoolheeya.bluebank.account.adapter"
])
class AccountApplication

fun main(args: Array<String>) {
    runApplication<AccountApplication>(*args)
}
