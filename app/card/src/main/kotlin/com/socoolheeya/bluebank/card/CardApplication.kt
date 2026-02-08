package com.socoolheeya.bluebank.card

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication(scanBasePackages = [
    "com.socoolheeya.bluebank.card"
])
@ConfigurationPropertiesScan(basePackages = [
    "com.socoolheeya.bluebank.card"
])
@EnableFeignClients(basePackages = [
    "com.socoolheeya.bluebank.account.adapter"
])
class CardApplication

fun main(args: Array<String>) {
    runApplication<CardApplication>(*args)
}
