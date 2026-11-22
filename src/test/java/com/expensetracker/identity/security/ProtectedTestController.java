package com.expensetracker.identity.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only protected endpoint; echoes the token's subject so tests can see who was authenticated. */
@RestController
class ProtectedTestController {

    @GetMapping("/api/test/protected")
    String whoAmI(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getSubject();
    }
}
