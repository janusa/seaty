package com.janusa.seaty

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor

class AuthInterceptor(
    private val secret: String,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val session = request.cookies?.firstOrNull { it.name == "session" }?.value
        if (session != null && Utils.constantTimeEquals(session, secret)) {
            log.debug("Authenticated request {} {}", request.method, request.requestURI)
            return true
        }
        val reason = if (session == null) "missing cookie" else "invalid cookie"
        // Sec-Fetch-Site reveals the navigation context (e.g. cross-site for an externally
        // initiated QR scan vs none for an address-bar visit), useful when diagnosing auth issues.
        log.warn(
            "Rejected unauthenticated request {} {} from {} (reason={}, secFetchSite={})",
            request.method,
            request.requestURI,
            request.remoteAddr,
            reason,
            request.getHeader("Sec-Fetch-Site") ?: "none",
        )
        // sendError (not a bare status) triggers the error dispatch so the custom error page renders.
        response.sendError(HttpStatus.UNAUTHORIZED.value())
        return false
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AuthInterceptor::class.java)
    }
}
