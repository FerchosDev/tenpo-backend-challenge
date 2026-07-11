package com.tenpo.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;

// Formato único de error de la API: toda respuesta 4XX/5XX.
public record ErrorResponse(
        @Schema(description = "Date and time when the error occurred") Instant timestamp,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "HTTP status reason phrase", example = "Bad Request") String error,
        @Schema(description = "Human-readable description of the error") String message,
        @Schema(description = "Request path that triggered the error", example = "/api/v1/calculate") String path
) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }

}
