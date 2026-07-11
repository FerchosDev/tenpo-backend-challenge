package com.tenpo.challenge.domain.model;

import java.time.Instant;

public record CallHistory(
        Long id,
        Instant timestamp,
        String endpoint,
        String params,
        String response,
        String error,
        Integer status
) {
}
