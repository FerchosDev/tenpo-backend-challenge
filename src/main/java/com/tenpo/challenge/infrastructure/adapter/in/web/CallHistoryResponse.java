package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CallHistoryResponse(
        @Schema(description = "History record id", example = "1") Long id,
        @Schema(description = "Date and time when the endpoint was invoked") Instant timestamp,
        @Schema(description = "Invoked endpoint path", example = "/api/v1/calculate") String endpoint,
        @Schema(description = "Request parameters, as a nested JSON object", type = "object") JsonNode params,
        @Schema(description = "Response body, as a nested JSON object (null if the call failed)", type = "object") JsonNode response,
        @Schema(description = "Error message (null if the call succeeded)") String error,
        @Schema(description = "Resulting HTTP status code", example = "200") Integer status
) {

    public static CallHistoryResponse from(CallHistoryEntity entity) {
        return new CallHistoryResponse(
                entity.getId(),
                entity.getTimestamp(),
                entity.getEndpoint(),
                entity.getParams(),
                entity.getResponse(),
                entity.getError(),
                entity.getStatus()
        );
    }

}
