package com.expensetracker.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/auth/register}. */
record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // NIST SP 800-63B: at least 8, allow long passphrases, no composition rules
        @NotNull @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 100) String name,
        // A String rather than the enum, so an unknown value gives a field error instead of a JSON parse error
        @NotNull @Pattern(regexp = "AZN|USD", message = "must be AZN or USD")
        String currency) {}
