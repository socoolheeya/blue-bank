package com.socoolheeya.bluebank.loan.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalErrorHandler {

    private val logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    @ExceptionHandler(LoanNotFoundException::class)
    fun handleLoanNotFoundException(
        ex: LoanNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Loan not found: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Loan not found",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(LoanApplicationNotFoundException::class)
    fun handleLoanApplicationNotFoundException(
        ex: LoanApplicationNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Loan application not found: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Loan application not found",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(InvalidLoanStatusException::class)
    fun handleInvalidLoanStatusException(
        ex: InvalidLoanStatusException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Invalid loan status: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Invalid loan status",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(LoanLimitExceededException::class)
    fun handleLoanLimitExceededException(
        ex: LoanLimitExceededException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Loan limit exceeded: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Loan limit exceeded",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = mutableMapOf<String, String?>()

        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val errorMessage = error.defaultMessage
            errors[fieldName] = errorMessage
        }

        logger.error("Validation failed: $errors")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed",
            path = request.getDescription(false).replace("uri=", ""),
            details = errors
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Invalid argument: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Invalid argument",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Malformed request body",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred",
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: Map<String, String?>? = null
)

// Custom Exceptions
class LoanNotFoundException(message: String) : RuntimeException(message)
class LoanApplicationNotFoundException(message: String) : RuntimeException(message)
class InvalidLoanStatusException(message: String) : RuntimeException(message)
class LoanLimitExceededException(message: String) : RuntimeException(message)
