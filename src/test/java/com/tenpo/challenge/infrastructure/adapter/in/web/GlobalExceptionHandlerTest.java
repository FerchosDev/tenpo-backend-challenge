package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.model.CallHistory;
import com.tenpo.challenge.domain.port.out.CallHistoryPort;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/calculate";

    @Mock
    private CallHistoryPort callHistoryPort;

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(callHistoryPort, new ObjectMapper());
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(REQUEST_PATH);
    }

    @Test
    void shouldReturn400WithFieldErrorsOnValidationFailure() {
        CalculationRequest target = new CalculationRequest(java.math.BigDecimal.valueOf(5), null);
        BindingResult bindingResult = new BeanPropertyBindingResult(target, "calculationRequest");
        bindingResult.addError(new FieldError("calculationRequest", "num2", "num2 is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("num2", "num2 is required");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
        assertThat(response.getBody().timestamp()).isNotNull();

        ArgumentCaptor<CallHistory> historyCaptor = ArgumentCaptor.forClass(CallHistory.class);
        verify(callHistoryPort).save(historyCaptor.capture());
        CallHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.endpoint()).isEqualTo(REQUEST_PATH);
        assertThat(savedHistory.status()).isEqualTo(400);
        assertThat(savedHistory.error()).contains("num2 is required");
        assertThat(savedHistory.params()).contains("\"num1\":5");
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

        // Este caso ya se loguea en CallHistoryLoggingAspect (con acceso a
        // los params reales del request), así que el handler no debe
        // duplicar el registro.
        verify(callHistoryPort, never()).save(org.mockito.ArgumentMatchers.any());
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

        verify(callHistoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

}
