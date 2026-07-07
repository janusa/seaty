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
            // The whole /error tree stays open: the error-dispatch page must render without
            // re-triggering auth, and its flower images must load even on the unauthenticated 401 page.
            // The favicons stay open too: browsers auto-request /favicon.ico and the icons must show
            // on the unauthenticated landing and error pages.
            .excludePathPatterns("/auth", "/error", "/error/**", "/favicon.ico", "/apple-touch-icon.png")
        log.info("Registered AuthInterceptor for /** (excluding /auth, /error/** and favicons)")
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WebConfig::class.java)
    }
}
