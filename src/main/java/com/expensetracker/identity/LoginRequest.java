package com.expensetracker.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/auth/login}. Only presence and size are checked: a wrong-looking email simply fails
 * to log in, like any other wrong credential.
 */
record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String password) {}
