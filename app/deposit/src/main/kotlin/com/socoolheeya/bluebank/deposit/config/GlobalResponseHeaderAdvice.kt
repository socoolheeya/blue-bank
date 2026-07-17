package com.socoolheeya.bluebank.deposit.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@ControllerAdvice
class GlobalResponseHeaderAdvice(
    @Value("\${server.port:8080}") private val serverPort: String,
    @Value("\${eureka.instance.instance-id:unknown}") private val instanceId: String,
    @Value("\${spring.application.name:deposit}") private val appName: String
) : ResponseBodyAdvice<Any> {

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean {
        // 모든 응답에 적용
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        // 모든 응답에 인스턴스 정보 헤더 추가
        response.headers.add("X-Instance-Port", serverPort)
        response.headers.add("X-Instance-Id", instanceId)
        response.headers.add("X-Service-Name", appName)

        return body
    }
}