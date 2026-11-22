package com.expensetracker.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OpenApiIntegrationTest extends IntegrationTest {

    @Test
    void apiDocsReturnOpenApiDocumentWithConfiguredInfo() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"info": {"title": "Expense Tracker API", "version": "v1"}}
                        """);
    }

    @Test
    void apiDocsOnlyIncludeApiPaths() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.paths")
                .asMap()
                .isNotEmpty()
                .allSatisfy((path, operations) -> assertThat(path).startsWith("/api/"));
    }

    @Test
    void apiDocsDeclareBearerAuthAsTheDefault() {
        assertThat(mvc.get().uri("/v3/api-docs")).bodyJson().isLenientlyEqualTo("""
                {
                  "components": {"securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}}},
                  "security": [{"bearerAuth": []}]
                }
                """);
    }

    @Test
    void authEndpointsAreDocumentedAsPublic() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .bodyJson()
                .extractingPath("$.paths['/api/v1/auth/login'].post.security")
                .asArray()
                .isEmpty();
    }

    @Test
    void swaggerUiIsServed() {
        assertThat(mvc.get().uri("/swagger-ui/index.html"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
    }
}
