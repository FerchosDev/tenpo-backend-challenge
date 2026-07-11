package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.exception.InvalidPaginationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

// Punto único de manejo de errores HTTP para toda la API.
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final CallHistoryRecorder callHistoryRecorder;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        HttpStatus status = ApiExceptionStatusResolver.resolve(ex);
        log.warn("Validation failed for {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        // El @Around de CallHistoryLoggingAspect nunca ve este caso: la
        // validación falla durante el binding del argumento, antes de que
        // el método del controller (el join point) se llegue a invocar.
        // Se loguea acá, con el objeto parcialmente bindeado como params.
        callHistoryRecorder.record(request.getRequestURI(), ex.getBindingResult().getTarget(), null, message, status.value());
        return build(status, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = describeMalformedBody(ex);
        HttpStatus status = ApiExceptionStatusResolver.resolve(ex);
        log.warn("Malformed request body for {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        callHistoryRecorder.record(request.getRequestURI(), null, null, message, status.value());
        return build(status, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "a different type";
        String message = "Invalid value for parameter '" + ex.getName() + "': expected " + requiredType;
        log.warn("Type mismatch for {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPagination(InvalidPaginationException ex, HttpServletRequest request) {
        log.warn("Invalid pagination for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        String message = "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL();
        log.warn("Route not found: {}", message);
        return build(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        HttpStatus status = ApiExceptionStatusResolver.resolve(ex);
        log.error("External service failure for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return build(status, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private String describeMalformedBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException invalidFormat && !invalidFormat.getPath().isEmpty()) {
            String field = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            return "Invalid value for field '" + field + "': expected " + invalidFormat.getTargetType().getSimpleName();
        }
        return "Malformed JSON request body";
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

}
