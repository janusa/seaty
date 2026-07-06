package com.janusa.seaty

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration

@RestController
class AuthController(
    @Value("\${auth.secret}") private val secret: String,
) {
    @GetMapping("/auth")
    fun authenticate(
        @RequestParam(name = "secret") providedSecret: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Unit>? =
        if (!Utils.constantTimeEquals(providedSecret, secret)) {
            log.warn("Failed authentication attempt from {}", request.remoteAddr)
            // sendError triggers the error dispatch so the custom 401-page renders.
            response.sendError(HttpStatus.UNAUTHORIZED.value())
            null
        } else {
            log.info(
                "Authentication succeeded from {}, issuing session cookie (referer={}, secFetchSite={})",
                request.remoteAddr,
                request.getHeader("Referer") ?: "none",
                request.getHeader("Sec-Fetch-Site") ?: "none",
            )
            val cookie =
                ResponseCookie
                    .from("session", secret)
                    .httpOnly(true)
                    .secure(true)
                    .maxAge(Duration.ofDays(1))
                    .sameSite("strict")
                    .build()

            ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(URI.create("/index.html"))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build()
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AuthController::class.java)
    }
}
