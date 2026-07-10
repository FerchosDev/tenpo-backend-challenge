package com.tenpo.challenge.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "call_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String params;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private Integer status;

}
