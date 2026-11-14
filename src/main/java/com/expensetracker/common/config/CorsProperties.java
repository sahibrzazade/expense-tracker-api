package com.expensetracker.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code app.cors.*} settings.
 *
 * @param allowedOrigins browser origins allowed to call the API, e.g. {@code http://localhost:5173}; empty means none
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
