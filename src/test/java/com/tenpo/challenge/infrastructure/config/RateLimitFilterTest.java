package com.tenpo.challenge.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DirtiesContext evita que el bucket (bean singleton) quede parcialmente
 * consumido para otros tests que puedan reutilizar este mismo contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitFilterTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnTooManyRequestsOnFourthCallWithinAMinute() {
        for (int i = 1; i <= 3; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/history?page=0&size=1", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<Map> fourthResponse = restTemplate.getForEntity("/api/v1/history?page=0&size=1", Map.class);

        assertThat(fourthResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(fourthResponse.getHeaders().getFirst("Retry-After")).isNotNull();
        assertThat(fourthResponse.getBody()).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(fourthResponse.getBody()).containsEntry("status", 429);
        assertThat(fourthResponse.getBody()).containsEntry("path", "/api/v1/history");
    }

}
