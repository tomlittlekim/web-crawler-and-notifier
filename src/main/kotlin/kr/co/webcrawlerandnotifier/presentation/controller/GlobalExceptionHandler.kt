package kr.co.webcrawlerandnotifier.presentation.controller // 또는 kr.co.webcrawlerandnotifier.config

import kr.co.webcrawlerandnotifier.domain.exception.CrawlerNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val timestamp: String, val status: Int, val error: String, val message: String?, val path: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CrawlerNotFoundException::class)
    fun handleCrawlerNotFoundException(
        ex: CrawlerNotFoundException,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("CrawlerNotFoundException: ${ex.message} for path ${request.requestURI}")
        val errorResponse = ErrorResponse(
            timestamp = java.time.LocalDateTime.now().toString(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("ValidationException: $errors for path ${request.requestURI}")
        val errorResponse = ErrorResponse(
            timestamp = java.time.LocalDateTime.now().toString(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed: $errors",
            path = request.requestURI
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: ${ex.message} for path ${request.requestURI}", ex)
        val errorResponse = ErrorResponse(
            timestamp = java.time.LocalDateTime.now().toString(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred: ${ex.localizedMessage}",
            path = request.requestURI
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}