package com.expensetracker.common.error;

/** Thrown when credentials (password, refresh token) are wrong or unusable. Mapped to 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
