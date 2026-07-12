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
            // The favicons and touch icons stay open too: browsers and mobile OSes auto-request them,
            // often directly rather than via a page <link>, so the served ones must show on the
            // unauthenticated landing and error pages. /apple-touch-icon-precomposed.png is a legacy
            // iOS/Android probe path we intentionally do not ship a file for: excluding it lets the
            // request fall through to a clean 404 (the client then falls back to /apple-touch-icon.png)
            // instead of a misleading 401 that would also fill the auth-warning log with benign probes.
            .excludePathPatterns(
                "/auth",
                "/error",
                "/error/**",
                "/favicon.ico",
                "/apple-touch-icon.png",
                "/apple-touch-icon-precomposed.png",
            )
        log.info("Registered AuthInterceptor for /** (excluding /auth, /error/** and icons)")
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WebConfig::class.java)
    }
}
