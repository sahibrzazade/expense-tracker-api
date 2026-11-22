package com.expensetracker.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class RegistrationIntegrationTest extends IntegrationTest {

    private static final String REGISTER = "/api/v1/auth/register";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerCreatesUserAndReturnsUsableAccessToken() {
        String email = uniqueEmail();

        MvcTestResult result = register(email, "correct-horse", "  Sahib  ", "AZN");

        assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
                        {
                          "tokenType": "Bearer",
                          "expiresIn": 900,
                          "user": {"email": "%s", "name": "Sahib", "currency": "AZN"}
                        }
                        """.formatted(email));

        String token = jsonValue(result, "$.accessToken");
        String userId = jsonValue(result, "$.user.id");
        assertThat(mvc.get().uri("/api/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .hasBodyTextEqualTo(userId);
    }

    @Test
    void passwordIsStoredOnlyAsArgon2idHash() {
        String email = uniqueEmail();
        register(email, "correct-horse", "Sahib", "USD");

        String hash =
                jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);

        assertThat(hash).startsWith("$argon2id$").doesNotContain("correct-horse");
    }

    @Test
    void responseNeverContainsPasswordOrHash() {
        assertThat(register(uniqueEmail(), "correct-horse", "Sahib", "USD"))
                .bodyText()
                .doesNotContain("correct-horse", "password", "argon2");
    }

    @Test
    void emailIsStoredLowerCase() {
        String email = uniqueEmail();

        assertThat(register(email.toUpperCase(), "correct-horse", "Sahib", "AZN"))
                .bodyJson()
                .extractingPath("$.user.email")
                .isEqualTo(email);
    }

    @Test
    void duplicateEmailInAnyLetterCaseReturns409() {
        String email = uniqueEmail();
        register(email, "correct-horse", "Sahib", "AZN");

        assertThat(register(email.toUpperCase(), "another-password", "Someone", "USD"))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("An account with this email already exists");
    }

    @Test
    void invalidFieldsReturn400WithOneErrorPerField() {
        assertThat(register("not-an-email", "short", " ", "EUR"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("email", "password", "name", "currency");
    }

    @Test
    void longUnicodePassphraseIsAccepted() {
        // 128 characters, 256 bytes in UTF-8: fine for Argon2id
        assertThat(register(uniqueEmail(), "ə".repeat(128), "Sahib", "AZN")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void passwordLongerThan128CharactersIsRejected() {
        assertThat(register(uniqueEmail(), "a".repeat(129), "Sahib", "AZN"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("password");
    }

    @Test
    void registerIsPublic() {
        // No Authorization header anywhere in this class: register must not require a token
        assertThat(register(uniqueEmail(), "correct-horse", "Sahib", "AZN")).hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult register(String email, String password, String name, String currency) {
        return postJson(REGISTER, """
                {"email": "%s", "password": "%s", "name": "%s", "currency": "%s"}
                """.formatted(email, password, name, currency));
    }
}
