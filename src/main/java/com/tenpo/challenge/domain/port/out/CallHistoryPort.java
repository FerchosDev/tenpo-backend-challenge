package com.tenpo.challenge.domain.port.out;

import com.tenpo.challenge.domain.model.CallHistory;

import java.util.concurrent.CompletableFuture;

public interface CallHistoryPort {

    CompletableFuture<Void> save(CallHistory callHistory);

}
