package com.expensetracker.support;

import com.expensetracker.identity.AuthProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Base class for integration tests: full application context, MockMvc and a real PostgreSQL.
 *
 * <p>All subclasses share the same configuration, so Spring caches one context and Testcontainers
 * starts a single PostgreSQL container for the whole test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    /** A valid access token for the given subject, signed with the application's key. */
    protected String accessToken(String subject) {
        Instant now = Instant.now();
        return accessToken(subject, now, now.plus(Duration.ofMinutes(15)));
    }

    /** An access token with explicit timestamps, e.g. an already expired one. */
    protected String accessToken(String subject, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(AuthProperties.JWT_ISSUER)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
