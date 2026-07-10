package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryEntity;

import java.time.Instant;

public record CallHistoryResponse(
        Long id,
        Instant timestamp,
        String endpoint,
        String params,
        String response,
        String error,
        Integer status
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
