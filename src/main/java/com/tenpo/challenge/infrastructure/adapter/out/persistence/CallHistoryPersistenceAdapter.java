package com.tenpo.challenge.infrastructure.adapter.out.persistence;

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
                .params(callHistory.params())
                .response(callHistory.response())
                .error(callHistory.error())
                .status(callHistory.status())
                .build();
    }

}
