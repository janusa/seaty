package com.janusa.seaty

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
            return true
        }
        response.status = HttpStatus.UNAUTHORIZED.value()
        return false
    }
}
