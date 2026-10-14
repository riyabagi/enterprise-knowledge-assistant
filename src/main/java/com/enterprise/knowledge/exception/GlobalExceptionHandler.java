
package com.enterprise.knowledge.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                req.getRequestURI(),
                null);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiError> handleDocProcessing(
            DocumentProcessingException ex, HttpServletRequest req) {

        log.warn("Document upload rejected: {}", ex.getMessage());

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                req.getRequestURI(),
                null);
    }

    @ExceptionHandler(AIUnavailableException.class)
    public ResponseEntity<ApiError> handleAIUnavailable(
            AIUnavailableException ex, HttpServletRequest req) {

        log.warn("AI unavailable: {}", ex.getMessage());

        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI service is temporarily unavailable.",
                req.getRequestURI(),
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {

        return build(
                HttpStatus.FORBIDDEN,
                "Access denied.",
                req.getRequestURI(),
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed.",
                req.getRequestURI(),
                details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest req) {

        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());

        return build(
                HttpStatus.BAD_REQUEST,
                "Constraint violation.",
                req.getRequestURI(),
                details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex, HttpServletRequest req) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                req.getRequestURI(),
                null);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            String path,
            List<String> details) {

        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details);

        return ResponseEntity.status(status).body(error);
    }
}
