package com.expensetracker.identity;

import com.expensetracker.common.error.ConflictException;
import com.expensetracker.identity.TokenService.AccessToken;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account use cases: registration now; login, refresh and logout follow. */
@Service
class AuthService {

    private static final String EMAIL_TAKEN = "An account with this email already exists";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_TAKEN);
        }

        User user = new User(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password()),
                Currency.valueOf(request.currency()));
        try {
            // Flush now so a concurrent registration with the same email fails here, not after we return
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(EMAIL_TAKEN);
        }
        return new AuthResult(user, tokenService.issueAccessToken(user));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** What a successful register/login produces; the controller turns it into the HTTP response. */
    record AuthResult(User user, AccessToken accessToken) {}
}
