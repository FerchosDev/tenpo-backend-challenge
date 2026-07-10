package com.tenpo.challenge.infrastructure.config;

import java.time.Instant;

public record RateLimitExceededResponse(
        String message,
        Instant timestamp,
        long retryAfter
) {
}
