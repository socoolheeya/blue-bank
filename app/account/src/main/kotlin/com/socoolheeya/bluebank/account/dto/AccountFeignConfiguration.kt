package com.socoolheeya.bluebank.account.dto

import feign.Logger
import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Account Feign Client 설정
 * - 로깅 레벨 설정
 * - 에러 처리
 * - Request Interceptor 설정
 */
@Configuration
class AccountFeignConfiguration {

    /**
     * Feign 로깅 레벨 설정
     * NONE: 로깅 없음
     * BASIC: 요청 메서드, URL, 응답 코드, 실행 시간만 로깅
     * HEADERS: BASIC + 요청/응답 헤더
     * FULL: 요청/응답의 헤더, 바디, 메타데이터 모두 로깅
     */
    @Bean
    fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.BASIC
    }

    /**
     * Request Interceptor
     * - 모든 Feign 요청에 공통 헤더 추가
     * - 인증 토큰, trace id 등 추가 가능
     */
    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("X-Service-Name", "account-service")
            template.header("Content-Type", "application/json")
            // MDC에서 trace id 가져와서 추가 (분산 추적용)
            // val traceId = MDC.get("traceId")
            // if (traceId != null) {
            //     template.header("X-Trace-Id", traceId)
            // }
        }
    }

    /**
     * Error Decoder
     * - Feign 에러 응답을 커스텀 예외로 변환
     */
    @Bean
    fun errorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                404 -> NoSuchElementException("계좌를 찾을 수 없습니다 (method: $methodKey)")
                400 -> IllegalArgumentException("잘못된 요청입니다 (method: $methodKey)")
                403 -> IllegalStateException("접근이 거부되었습니다 (method: $methodKey)")
                500 -> RuntimeException("서버 오류가 발생했습니다 (method: $methodKey)")
                else -> RuntimeException("알 수 없는 오류가 발생했습니다 (status: ${response.status()}, method: $methodKey)")
            }
        }
    }
}