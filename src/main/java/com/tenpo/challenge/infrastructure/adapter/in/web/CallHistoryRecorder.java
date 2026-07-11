package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.domain.model.CallHistory;
import com.tenpo.challenge.domain.port.out.CallHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CallHistoryRecorder {

    private final CallHistoryPort callHistoryPort;
    private final ObjectMapper objectMapper;

    public void record(String endpoint, Object params, Object response, String error, int status) {
        callHistoryPort.save(new CallHistory(
                null,
                Instant.now(),
                endpoint,
                toJson(params),
                toJson(response),
                error,
                status
        ));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

}
