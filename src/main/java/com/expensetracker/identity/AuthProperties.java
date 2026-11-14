package com.expensetracker.identity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed view of the {@code app.auth.*} settings. Construction fails (and so does application startup) if the JWT
 * secret is missing or too short.
 *
 * @param jwtSecret HMAC key for signing access tokens; at least {@value #MIN_SECRET_BYTES} bytes
 * @param accessTokenTtl how long an access token stays valid
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(
        String jwtSecret, @DefaultValue("15m") Duration accessTokenTtl) {

    /** Value of the {@code iss} claim in every access token. */
    public static final String JWT_ISSUER = "expense-tracker-api";

    /** HS256 needs a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.auth.jwt-secret (env JWT_SECRET) must be set and at least "
                    + MIN_SECRET_BYTES + " bytes long");
        }
    }
}
