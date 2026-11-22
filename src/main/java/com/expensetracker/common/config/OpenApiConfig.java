package com.expensetracker.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata for the generated OpenAPI document, the API contract consumed by the frontend repo.
 *
 * <p>Every operation requires a bearer token by default; public controllers opt out with an empty
 * {@code @SecurityRequirements}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI expenseTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense Tracker API")
                        .version("v1")
                        .description("REST API for a personal expense tracker."))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
