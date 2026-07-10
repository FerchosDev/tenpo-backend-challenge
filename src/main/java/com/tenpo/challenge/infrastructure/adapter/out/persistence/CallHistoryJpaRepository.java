package com.tenpo.challenge.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryJpaRepository extends JpaRepository<CallHistoryEntity, Long> {
}
