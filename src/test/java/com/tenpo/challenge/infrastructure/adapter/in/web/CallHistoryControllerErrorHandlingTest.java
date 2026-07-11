package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryEntity;
import com.tenpo.challenge.infrastructure.adapter.out.persistence.CallHistoryReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CallHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CallHistoryControllerErrorHandlingTest {

    private static final String ENDPOINT = "/api/v1/history";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CallHistoryReader callHistoryReader;

    @MockBean
    private CallHistoryRecorder callHistoryRecorder;

    @Test
    void shouldReturn400ForNegativePage() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("page")))
                .andExpect(jsonPath("$.path").value(ENDPOINT));
    }

    @Test
    void shouldReturn400ForZeroSize() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size")));
    }

    @Test
    void shouldReturn400ForExcessiveSize() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("size", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("size")));
    }

    @Test
    void shouldReturn400ForNonNumericPage() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("page")));
    }

    @Test
    void shouldReturn200ForValidPagination() throws Exception {
        when(callHistoryReader.findAll(any())).thenReturn(Page.<CallHistoryEntity>empty());

        mockMvc.perform(get(ENDPOINT).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

}
