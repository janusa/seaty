package com.janusa.seaty

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    @Value("\${auth.secret}") private val secret: String,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(AuthInterceptor(secret))
            .addPathPatterns("/**")
            .excludePathPatterns("/auth")
        log.info("Registered AuthInterceptor for /** (excluding /auth)")
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WebConfig::class.java)
    }
}
