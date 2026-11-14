package com.expensetracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class FoundationIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void healthEndpointReportsUpWhenDatabaseIsReachable() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        // Anonymous callers are stopped by security; even an authenticated user finds nothing there
        assertThat(mvc.get().uri("/actuator/info")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().with(jwt()).uri("/actuator/info")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void flywayAppliesBaselineMigration() {
        Boolean applied = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);

        assertThat(applied).isTrue();
    }
}
