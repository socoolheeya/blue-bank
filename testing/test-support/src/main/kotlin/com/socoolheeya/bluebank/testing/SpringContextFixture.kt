package com.socoolheeya.bluebank.testing

import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

object SpringContextFixture {
    @Suppress("UNCHECKED_CAST")
    fun start(
        vararg sources: Class<*>,
        properties: Map<String, Any?> = emptyMap(),
    ): ConfigurableApplicationContext =
        SpringApplicationBuilder(*sources)
            .web(WebApplicationType.NONE)
            .properties(
                (properties + mapOf(
                    "eureka.client.enabled" to false,
                    "spring.cloud.discovery.enabled" to false,
                )) as Map<String, Any>,
            )
            .run()
}
