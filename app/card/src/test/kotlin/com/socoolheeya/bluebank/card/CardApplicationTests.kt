package com.socoolheeya.bluebank.card

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

val cardApplicationSuite by testSuite("Card application") {
    test("starts with representative application bean") {
        val context = SpringApplicationBuilder(CardApplication::class.java)
            .web(WebApplicationType.SERVLET)
            .run("--server.port=0", "--eureka.client.enabled=false", "--spring.cloud.discovery.enabled=false")
        try {
            context.getBean(CardApplication::class.java)
        } finally {
            context.close()
        }
    }
}
