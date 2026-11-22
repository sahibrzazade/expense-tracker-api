package com.expensetracker.identity;

import com.expensetracker.common.error.ConflictException;
import com.expensetracker.common.error.UnauthorizedException;
import com.expensetracker.identity.TokenService.AccessToken;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account use cases: registration and login; refresh and logout follow. */
@Service
class AuthService {

    private static final String EMAIL_TAKEN = "An account with this email already exists";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * A real hash of a throwaway password, made once at startup with the same parameters as user hashes. Checking
     * against it when the email is unknown makes that path as slow as a real check.
     */
    private final String dummyPasswordHash;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
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

    @Transactional(readOnly = true)
    AuthResult login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(request.email()));

        // Always run exactly one password check, so wrong-email and wrong-password take the same time
        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user.isEmpty() || !passwordMatches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return new AuthResult(user.get(), tokenService.issueAccessToken(user.get()));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** What a successful register/login produces; the controller turns it into the HTTP response. */
    record AuthResult(User user, AccessToken accessToken) {}
}
