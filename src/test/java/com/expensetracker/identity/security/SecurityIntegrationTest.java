package com.expensetracker.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.identity.AuthProperties;
import com.expensetracker.support.IntegrationTest;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class SecurityIntegrationTest extends IntegrationTest {

    private static final String PROTECTED = "/api/test/protected";

    @Test
    void validTokenGrantsAccess() {
        assertThat(mvc.get().uri(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("user-123")))
                .hasStatusOk()
                .hasBodyTextEqualTo("user-123");
    }

    @Test
    void missingTokenReturns401ProblemDetail() {
        assertThat(mvc.get().uri(PROTECTED))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 401, "detail": "Missing or invalid access token", "instance": "/api/test/protected"}
                        """);
    }

    @Test
    void malformedTokenReturns401() {
        assertThat(mvc.get().uri(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void expiredTokenReturns401() {
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        String expired = accessToken("user-123", twoHoursAgo, twoHoursAgo.plus(Duration.ofMinutes(15)));

        assertThat(mvc.get().uri(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void tokenSignedWithAnotherKeyReturns401() {
        var foreignKey = new SecretKeySpec(
                "some-other-secret-that-is-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var foreignEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(foreignKey));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(AuthProperties.JWT_ISSUER)
                .subject("attacker")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .build();
        String forged = foreignEncoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThat(mvc.get().uri(PROTECTED).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownApiRouteWithoutTokenReturns401() {
        assertThat(mvc.get().uri("/api/v1/does-not-exist")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicEndpointsNeedNoToken() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }
}
