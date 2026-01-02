package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 * Provides CRUD and custom query methods for users table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     * Used for authentication and duplicate checking.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     * Used during registration to prevent duplicates.
     */
    boolean existsByEmail(String email);

    /**
     * Find active user by email (not soft-deleted).
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
