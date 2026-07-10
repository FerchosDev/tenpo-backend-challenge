package com.tenpo.challenge.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración con Postgres real (TestContainers), sin mocks de
 * persistencia. Verifica el flujo completo: POST /calculate -> historial
 * guardado async -> GET /history lo expone paginado, con params/response
 * como objetos JSON anidados (no strings escapados) y sin exponer los
 * campos internos de Page (pageable, sort) en el body raíz.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CallHistoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldPersistAndRetrieveCallHistoryAfterCalculateInvocation() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"num1\":5,\"num2\":5}", headers);

        ResponseEntity<String> calculateResponse = restTemplate.postForEntity("/api/v1/calculate", request, String.class);
        assertThat(calculateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> history = awaitHistoryWithAtLeastOneEntry();

        assertThat(history).doesNotContainKeys("pageable", "sort");
        assertThat((Integer) history.get("totalElements")).isGreaterThanOrEqualTo(1);
        assertThat(history).containsKeys("content", "page", "size", "totalElements", "totalPages", "last");

        @SuppressWarnings("unchecked")
        Map<String, Object> firstEntry = ((List<Map<String, Object>>) history.get("content")).get(0);
        assertThat(firstEntry.get("endpoint")).isEqualTo("/api/v1/calculate");
        assertThat(firstEntry.get("status")).isEqualTo(200);

        assertThat(firstEntry.get("params")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) firstEntry.get("params");
        assertThat(params).containsEntry("num1", 5).containsEntry("num2", 5);

        assertThat(firstEntry.get("response")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) firstEntry.get("response");
        assertThat(responseBody).containsEntry("result", 11.00);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> awaitHistoryWithAtLeastOneEntry() throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/history?page=0&size=10", Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && ((Integer) body.get("totalElements")) >= 1) {
                return body;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Call history was not persisted within the expected time");
    }

}
