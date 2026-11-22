package com.expensetracker.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class MeIntegrationTest extends IntegrationTest {

    private static final String ME = "/api/v1/me";

    @Test
    void returnsTheAuthenticatedUser() {
        String email = uniqueEmail();
        MvcTestResult registered = registerUser(email, "correct-horse");
        String token = jsonValue(registered, "$.accessToken");
        String userId = jsonValue(registered, "$.user.id");

        assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id": "%s", "email": "%s", "name": "Test User", "currency": "AZN"}
                        """.formatted(userId, email));
    }

    @Test
    void withoutTokenReturns401() {
        assertThat(mvc.get().uri(ME))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void validTokenForDeletedOrUnknownUserReturns401() {
        String tokenForNobody = accessToken(UUID.randomUUID().toString());

        assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenForNobody))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void tokenWhoseSubjectIsNotAUserIdReturns401() {
        assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("not-a-uuid")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
