package com.tenpo.challenge.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CallHistoryReader {

    Page<CallHistoryEntity> findAll(Pageable pageable);

}
