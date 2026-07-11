package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.exception.InvalidPaginationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/api/v1/calculate";

    @Mock
    private CallHistoryRecorder callHistoryRecorder;

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(callHistoryRecorder);
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

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(callHistoryRecorder).record(eq(REQUEST_PATH), eq(target), isNull(), messageCaptor.capture(), eq(400));
        assertThat(messageCaptor.getValue()).contains("num2", "num2 is required");
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

        verifyNoInteractions(callHistoryRecorder);
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

        verifyNoInteractions(callHistoryRecorder);
    }

    @Test
    void shouldReturn400WithGenericMessageForUnparseableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", mock(HttpInputMessage.class));

        ResponseEntity<ErrorResponse> response = handler.handleMalformedBody(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed JSON request body");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_PATH);
    }

    @Test
    void shouldReturn400NamingTheFieldForWrongTypeInBody() {
        InvalidFormatException invalidFormat = InvalidFormatException.from(null, "bad value", "abc", java.math.BigDecimal.class);
        invalidFormat.prependPath(CalculationRequest.class, "num1");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", invalidFormat, mock(HttpInputMessage.class));

        ResponseEntity<ErrorResponse> response = handler.handleMalformedBody(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for field 'num1': expected BigDecimal");
    }

    @Test
    void shouldReturn400ForTypeMismatchInRequestParam() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", mock(MethodParameter.class), new NumberFormatException("not a number"));

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid value for parameter 'page': expected Integer");
    }

    @Test
    void shouldReturn400ForInvalidPagination() {
        InvalidPaginationException ex = new InvalidPaginationException("size must not exceed 100, but was 5000");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidPagination(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("size must not exceed 100, but was 5000");
    }

    @Test
    void shouldReturn404ForUnknownRoute() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/v1/does-not-exist",
                org.springframework.http.HttpHeaders.EMPTY);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("/api/v1/does-not-exist");
    }

}
