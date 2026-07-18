package com.socoolheeya.bluebank.loan

import de.infix.testBalloon.framework.core.testSuite
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder

val loanApplicationSuite by testSuite("Loan application") {
    test("starts with representative application bean") {
        val context = SpringApplicationBuilder(LoanApplication::class.java)
            .web(WebApplicationType.SERVLET)
            .run("--server.port=0", "--eureka.client.enabled=false", "--spring.cloud.discovery.enabled=false")
        try {
            context.getBean(LoanApplication::class.java)
        } finally {
            context.close()
        }
    }
}
