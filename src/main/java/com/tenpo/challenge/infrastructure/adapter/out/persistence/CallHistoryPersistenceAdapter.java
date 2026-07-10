package com.tenpo.challenge.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tenpo.challenge.domain.model.CallHistory;
import com.tenpo.challenge.domain.port.out.CallHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CallHistoryPersistenceAdapter implements CallHistoryPort, CallHistoryReader {

    private final CallHistoryJpaRepository callHistoryJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public CompletableFuture<Void> save(CallHistory callHistory) {
        callHistoryJpaRepository.save(toEntity(callHistory));
        return CompletableFuture.completedFuture(null);
    }

    public Page<CallHistoryEntity> findAll(Pageable pageable) {
        return callHistoryJpaRepository.findAll(pageable);
    }

    private CallHistoryEntity toEntity(CallHistory callHistory) {
        return CallHistoryEntity.builder()
                .timestamp(callHistory.timestamp())
                .endpoint(callHistory.endpoint())
                .params(toJsonNode(callHistory.params()))
                .response(toJsonNode(callHistory.response()))
                .error(callHistory.error())
                .status(callHistory.status())
                .build();
    }

    /**
     * CallHistory (dominio) guarda params/response como texto JSON crudo,
     * para no filtrar tipos de Jackson en el dominio. Acá se parsea a
     * JsonNode recién en el borde de infraestructura, así Hibernate lo
     * persiste como un objeto jsonb real y no como un string escapado.
     */
    private JsonNode toJsonNode(String rawJson) {
        if (rawJson == null) {
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            return TextNode.valueOf(rawJson);
        }
    }

}
