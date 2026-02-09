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
         * Eagerly fetches profiles to determine roles during authentication.
         */
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "doctorProfile", "patientProfile" })
        Optional<User> findByEmailAndDeletedAtIsNull(String email);

        /**
         * Find active users who haven't logged in for exactly N days (to avoid
         * duplicate notifications) and haven't been notified in the last 7 days.
         */
        @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM users u " +
                        "WHERE activity_status = 'active' " +
                        "AND last_login_at < (NOW() - CAST(?1 || ' days' AS INTERVAL)) " +
                        "AND last_login_at >= (NOW() - CAST((?1 + 1) || ' days' AS INTERVAL)) " +
                        "AND deleted_at IS NULL " +
                        "AND NOT EXISTS (" +
                        "  SELECT 1 FROM notifications n " +
                        "  WHERE n.user_id = u.id " +
                        "  AND n.type = 'USER_REENGAGE_ACTIVE' " +
                        "  AND n.sent_at > NOW() - INTERVAL '7 days'" +
                        ")", nativeQuery = true)
        java.util.List<User> findUsersForReEngagement(int days);

        /**
         * Find users who haven't logged in for exactly N days and haven't been warned
         * yet, with a 7-day throttling window.
         */
        @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM users u " +
                        "WHERE last_login_at < (NOW() - CAST(?1 || ' days' AS INTERVAL)) " +
                        "AND last_login_at >= (NOW() - CAST((?1 + 1) || ' days' AS INTERVAL)) " +
                        "AND activity_status != 'inactive' " +
                        "AND deleted_at IS NULL " +
                        "AND NOT EXISTS (" +
                        "  SELECT 1 FROM notifications n " +
                        "  WHERE n.user_id = u.id " +
                        "  AND n.type = 'USER_INACTIVITY_WARNING' " +
                        "  AND n.sent_at > NOW() - INTERVAL '7 days'" +
                        ")", nativeQuery = true)
        java.util.List<User> findUsersForInactivityWarning(int days);

        /**
         * Find users past the final inactivity threshold to mark them as inactive.
         */
        @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM users " +
                        "WHERE last_login_at < (NOW() - CAST(?1 || ' days' AS INTERVAL)) " +
                        "AND activity_status != 'inactive' " +
                        "AND deleted_at IS NULL", nativeQuery = true)
        java.util.List<User> findUsersToBeMarkedInactive(int totalDays);
}
