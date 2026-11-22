package com.expensetracker.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class LoginIntegrationTest extends IntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String PASSWORD = "correct-horse";

    @Test
    void correctCredentialsReturnUsableAccessToken() {
        String email = uniqueEmail();
        String userId = jsonValue(registerUser(email, PASSWORD), "$.user.id");

        MvcTestResult result = login(email, PASSWORD);

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"tokenType": "Bearer", "expiresIn": 900, "user": {"id": "%s", "email": "%s"}}
                """.formatted(userId, email));
        String token = jsonValue(result, "$.accessToken");
        assertThat(mvc.get().uri("/api/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .hasBodyTextEqualTo(userId);
    }

    @Test
    void emailIsMatchedIgnoringCaseAndSurroundingSpaces() {
        String email = uniqueEmail();
        registerUser(email, PASSWORD);

        assertThat(login("  " + email.toUpperCase() + " ", PASSWORD)).hasStatusOk();
    }

    @Test
    void wrongPasswordReturns401() {
        String email = uniqueEmail();
        registerUser(email, PASSWORD);

        assertThat(login(email, "wrong-password"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 401, "detail": "Invalid email or password", "instance": "/api/v1/auth/login"}
                        """);
    }

    @Test
    void unknownEmailAndWrongPasswordGetIdenticalResponses() throws Exception {
        String email = uniqueEmail();
        registerUser(email, PASSWORD);

        MvcTestResult wrongPassword = login(email, "wrong-password");
        MvcTestResult unknownEmail = login(uniqueEmail(), PASSWORD);

        assertThat(unknownEmail).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void missingFieldsReturn400() {
        assertThat(postJson(LOGIN, "{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("email", "password");
    }

    private MvcTestResult login(String email, String password) {
        return postJson(LOGIN, """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }
}
