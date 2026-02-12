package com.socoolheeya.bluebank.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(LoggingGlobalFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val startTime = System.currentTimeMillis()

        logger.info(
            "Incoming request: [{}] {} from {}",
            request.method,
            request.uri.path,
            request.remoteAddress
        )

        return chain.filter(exchange).then(
            Mono.fromRunnable {
                val response = exchange.response
                val duration = System.currentTimeMillis() - startTime

                logger.info(
                    "Outgoing response: [{}] {} - Status: {} - Duration: {}ms",
                    request.method,
                    request.uri.path,
                    response.statusCode,
                    duration
                )
            }
        )
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}