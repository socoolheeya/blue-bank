package com.socoolheeya.bluebank.batch

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

val batchApplicationSuite by testSuite("Batch application") {
    test("starts with representative application bean") {
        val context = SpringApplicationBuilder(BatchApplication::class.java)
            .web(WebApplicationType.NONE)
            .properties("eureka.client.enabled=false", "spring.cloud.discovery.enabled=false")
            .run()
        try {
            context.getBean(BatchApplication::class.java)
        } finally {
            context.close()
        }
    }
}
