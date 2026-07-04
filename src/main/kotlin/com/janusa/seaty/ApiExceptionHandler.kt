package com.janusa.seaty

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(DataAccessException::class)
    // NamedArguments: handleExceptionInternal is inherited from Java, where Kotlin forbids named args.
    @Suppress("NamedArguments")
    fun handleDataAccessException(
        ex: DataAccessException,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val servletRequest = (request as ServletWebRequest).request
        log.error("Unhandled data-access error on {} {}", servletRequest.method, servletRequest.requestURI, ex)
        val body = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        return handleExceptionInternal(ex, body, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    /**
     * Single choke point for every error response. Browsers (an explicit `text/html` Accept) are
     * handed to the error dispatch so the styled static page under `static/error/` renders; every
     * other client keeps the RFC 9457 problem+json body produced by the framework.
     */
    @Suppress("NamedArguments") // super call is a Java method; Kotlin forbids named args there.
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (prefersHtml(request)) {
            (request as? ServletWebRequest)?.response?.sendError(statusCode.value())
            return null
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun prefersHtml(request: WebRequest): Boolean {
        val accept = request.getHeader(HttpHeaders.ACCEPT) ?: return false
        return runCatching {
            MediaType.parseMediaTypes(accept).any { it.subtype.equals("html", ignoreCase = true) }
        }.getOrDefault(false)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
