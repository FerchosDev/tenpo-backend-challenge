package com.tenpo.challenge.infrastructure.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record RateLimitExceededResponse(
        @Schema(description = "Human-readable description of the rate limit error") String message,
        @Schema(description = "Date and time when the limit was exceeded") Instant timestamp,
        @Schema(description = "Seconds to wait before the rate limit resets", example = "42") long retryAfter
) {
}
