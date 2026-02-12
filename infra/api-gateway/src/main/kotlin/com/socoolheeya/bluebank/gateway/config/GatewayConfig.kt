package com.socoolheeya.bluebank.gateway.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.time.Duration

@Configuration
class GatewayConfig {

    /**
     * Circuit Breaker Factory 커스터마이징
     */
    @Bean
    fun defaultCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(
                        CircuitBreakerConfig.custom()
                            .slidingWindowSize(10)
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .failureRateThreshold(50.0f)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .slowCallDurationThreshold(Duration.ofSeconds(5))
                            .slowCallRateThreshold(50.0f)
                            .build()
                    )
                    .timeLimiterConfig(
                        TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build()
                    )
                    .build()
            }
        }
    }

    /**
     * Rate Limiter를 위한 Key Resolver
     * IP 주소 기반으로 rate limiting
     */
    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            Mono.just(
                exchange.request.remoteAddress?.address?.hostAddress ?: "anonymous"
            )
        }
    }
}