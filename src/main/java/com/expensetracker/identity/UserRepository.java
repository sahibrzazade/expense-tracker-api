package com.expensetracker.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation, including queries derived from method names. */
interface UserRepository extends JpaRepository<User, UUID> {

    /** Becomes {@code SELECT ... WHERE email = ?}; callers pass an already normalized email. */
    boolean existsByEmail(String email);

    /** Empty when no account uses this (normalized) email. */
    Optional<User> findByEmail(String email);
}
