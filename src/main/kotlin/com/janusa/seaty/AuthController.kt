package com.janusa.seaty

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
    ): ResponseEntity<Unit> =
        if (!Utils.constantTimeEquals(providedSecret, secret)) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        } else {
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
}
