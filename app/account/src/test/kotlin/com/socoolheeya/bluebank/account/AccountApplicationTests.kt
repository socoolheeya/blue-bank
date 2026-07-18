package com.socoolheeya.bluebank.account

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.beans.factory.getBean
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

val accountApplicationSuite by testSuite("Account application") {
    test("starts with representative application bean") {
        val context = SpringApplicationBuilder(AccountApplication::class.java)
            .web(WebApplicationType.SERVLET)
            .run("--server.port=0", "--eureka.client.enabled=false", "--spring.cloud.discovery.enabled=false")
        context.use { context ->
            context.getBean<AccountApplication>()
        }
    }
}
