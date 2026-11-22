package com.expensetracker.identity;

import java.util.UUID;

/** Public view of a user; never includes the password hash. */
record UserResponse(UUID id, String email, String name, Currency currency) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCurrency());
    }
}
