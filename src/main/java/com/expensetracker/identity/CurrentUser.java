package com.expensetracker.identity;

import com.expensetracker.common.error.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Who is calling, for any module: inject it and call {@link #id()} (cheap, from the token) or {@link #get()} (loads
 * the user, e.g. to read the currency).
 */
@Component
public class CurrentUser {

    private static final String INVALID_TOKEN = "Missing or invalid access token";

    private final UserRepository userRepository;

    CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The authenticated user's id, taken from the access token's {@code sub} claim. */
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new UnauthorizedException(INVALID_TOKEN);
        }
        try {
            return UUID.fromString(token.getName());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException(INVALID_TOKEN);
        }
    }

    /** The authenticated user; 401 if the account no longer exists even though the token is still valid. */
    public User get() {
        return userRepository.findById(id()).orElseThrow(() -> new UnauthorizedException(INVALID_TOKEN));
    }
}
