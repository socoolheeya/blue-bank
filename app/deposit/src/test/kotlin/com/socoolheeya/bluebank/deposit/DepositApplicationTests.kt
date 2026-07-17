package com.socoolheeya.bluebank.deposit

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

val depositApplicationSuite by testSuite("Deposit application") {
    test("starts with representative application bean") {
        val context = SpringApplicationBuilder(DepositApplication::class.java)
            .web(WebApplicationType.NONE)
            .properties("eureka.client.enabled=false", "spring.cloud.discovery.enabled=false")
            .run()
        try {
            context.getBean(DepositApplication::class.java)
        } finally {
            context.close()
        }
    }
}
