package com.expensetracker.identity;

import com.expensetracker.identity.AuthService.AuthResult;

/** Returned by register and login. */
record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken().value(),
                "Bearer",
                result.accessToken().expiresInSeconds(),
                UserResponse.from(result.user()));
    }
}
