package com.janusa.seaty

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(
        ex: DataAccessException,
        request: HttpServletRequest,
    ): ProblemDetail {
        log.error("Unhandled data-access error on {} {}", request.method, request.requestURI, ex)
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
