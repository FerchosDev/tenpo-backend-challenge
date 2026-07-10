package com.tenpo.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ErrorResponse(
        @Schema(description = "Date and time when the error occurred") Instant timestamp,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "HTTP status reason phrase", example = "Bad Request") String error,
        @Schema(description = "Human-readable description of the error") String message,
        @Schema(description = "Request path that triggered the error", example = "/api/v1/calculate") String path
) {
}
