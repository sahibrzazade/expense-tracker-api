package com.expensetracker.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Plain unit test: no Spring context, no database. */
class AuthPropertiesTest {

    private static final Duration TTL = Duration.ofMinutes(15);

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> new AuthProperties(null, TTL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsEmptySecret() {
        assertThatThrownBy(() -> new AuthProperties("", TTL)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new AuthProperties("x".repeat(31), TTL)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsSecretOf32Bytes() {
        assertThat(new AuthProperties("x".repeat(32), TTL).jwtSecret()).hasSize(32);
    }
}
