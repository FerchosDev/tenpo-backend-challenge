package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.exception.InvalidPaginationException;
import com.tenpo.challenge.domain.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

// Única fuente de verdad para el mapeo excepción -> HttpStatus
public final class ApiExceptionStatusResolver {

    private ApiExceptionStatusResolver() {
    }

    public static HttpStatus resolve(Throwable ex) {
        if (ex instanceof MethodArgumentNotValidException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof InvalidPaginationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof NoHandlerFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof RateLimitExceededException) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (ex instanceof ExternalServiceException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
