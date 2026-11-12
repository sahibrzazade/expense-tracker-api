package com.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// Temporary: uses the local compose database until Testcontainers replaces it (#9)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExpenseTrackerApplicationTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void healthEndpointReportsUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        assertThat(mvc.get().uri("/actuator/info")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
