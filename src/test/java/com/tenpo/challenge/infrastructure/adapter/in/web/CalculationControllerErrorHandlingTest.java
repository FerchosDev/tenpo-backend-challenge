package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.exception.ExternalServiceException;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CalculationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalculationControllerErrorHandlingTest {

    private static final String ENDPOINT = "/api/v1/calculate";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculationUseCase calculationUseCase;

    @MockBean
    private CallHistoryRecorder callHistoryRecorder;

    @Test
    void shouldReturn400WithErrorDtoWhenFieldsAreNull() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num1\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("num2")))
                .andExpect(jsonPath("$.path").value(ENDPOINT))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400ForMalformedJsonBody() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    @Test
    void shouldReturn400NamingFieldForWrongType() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num1\":\"not-a-number\",\"num2\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("num1")));
    }

    @Test
    void shouldReturn503WhenExternalServiceFailsAfterRetries() throws Exception {
        when(calculationUseCase.calculate(any(), any()))
                .thenThrow(new ExternalServiceException("Percentage provider failed after 3 retries", new RuntimeException("timeout")));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num1\":5,\"num2\":5}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Percentage provider failed after 3 retries"));
    }

    @Test
    void shouldReturn500WithoutLeakingInternalDetails() throws Exception {
        when(calculationUseCase.calculate(any(), any()))
                .thenThrow(new RuntimeException("org.postgresql.util.PSQLException: connection to internal-db:5432 refused"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"num1\":5,\"num2\":5}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(containsString("PSQLException"))))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(containsString("internal-db"))));
    }

    @Test
    void shouldReturn404WithErrorDtoForUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

}
