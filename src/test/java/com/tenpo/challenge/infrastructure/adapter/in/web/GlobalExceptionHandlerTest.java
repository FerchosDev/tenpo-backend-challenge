package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.exception.ExternalServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/calculate";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(REQUEST_PATH);
    }

    @Test
    void shouldReturn400WithFieldErrorsOnValidationFailure() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "calculationRequest");
        bindingResult.addError(new FieldError("calculationRequest", "num1", "num1 is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("num1", "num1 is required");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturn503WhenExternalServiceRetriesAreExhausted() {
        ExternalServiceException ex = new ExternalServiceException(
                "Percentage provider failed after exhausting retries", new RuntimeException("timeout"));

        ResponseEntity<ErrorResponse> response = handler.handleExternalService(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().message()).isEqualTo("Percentage provider failed after exhausting retries");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void shouldReturn500ForUnhandledException() {
        RuntimeException ex = new RuntimeException("something exploded unexpectedly");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

}
