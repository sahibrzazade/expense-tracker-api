package com.expensetracker.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.expensetracker.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ErrorHandlingIntegrationTest extends IntegrationTest {

    @Test
    void invalidRequestBodyReturns400WithFieldErrors() {
        assertThat(mvc.post()
                        .with(jwt())
                        .uri("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Request validation failed",
                          "instance": "/test/errors/validation",
                          "errors": [{"field": "name", "message": "must not be blank"}]
                        }
                        """);
    }

    @Test
    void invalidRequestParamReturns400WithFieldErrors() {
        assertThat(mvc.get().with(jwt()).uri("/test/errors/validation?page=0"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "detail": "Request validation failed",
                          "errors": [{"field": "page", "message": "must be greater than or equal to 1"}]
                        }
                        """);
    }

    @Test
    void malformedJsonReturns400WithoutFieldErrors() {
        MvcTestResult result = mvc.post()
                .with(jwt())
                .uri("/test/errors/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().doesNotHavePath("$.errors");
    }

    @Test
    void notFoundExceptionReturns404WithMessage() {
        assertThat(mvc.get().with(jwt()).uri("/test/errors/not-found"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 404, "detail": "Item 42 not found"}
                        """);
    }

    @Test
    void unknownRouteReturns404ProblemDetail() {
        assertThat(mvc.get().with(jwt()).uri("/api/v1/does-not-exist"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void wrongHttpMethodReturns405ProblemDetail() {
        assertThat(mvc.post().with(jwt()).uri("/test/errors/not-found"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void conflictExceptionReturns409WithMessage() {
        assertThat(mvc.get().with(jwt()).uri("/test/errors/conflict"))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 409, "detail": "Item is still in use"}
                        """);
    }

    @Test
    void accessDeniedReturns403ProblemDetail() {
        assertThat(mvc.get().with(jwt()).uri("/test/errors/forbidden"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 403, "detail": "Access denied"}
                        """);
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternals() {
        MvcTestResult result =
                mvc.get().with(jwt()).uri("/test/errors/unexpected").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"status": 500, "detail": "An unexpected error occurred"}
                        """);
        assertThat(result).bodyText().doesNotContain("secret internal detail", "IllegalStateException", "at com.");
    }
}
