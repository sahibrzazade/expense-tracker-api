package com.expensetracker.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class CorsIntegrationTest extends IntegrationTest {

    private static final String PROBE = "/api/test/cors-probe";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Test
    void preflightFromAllowedOriginIsAccepted() {
        assertThat(mvc.options()
                        .uri(PROBE)
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .hasStatusOk()
                .headers()
                .hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
    }

    @Test
    void preflightFromUnlistedOriginIsRejected() {
        assertThat(mvc.options()
                        .uri(PROBE)
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .headers()
                .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void actualRequestFromAllowedOriginGetsAllowOriginHeader() {
        assertThat(mvc.get().uri(PROBE).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .hasStatusOk()
                .headers()
                .hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
    }
}
