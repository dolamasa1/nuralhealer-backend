package com.neuralhealer.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration enabling audit features.
 * Automatically populates @CreatedDate and @LastModifiedDate fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing is enabled via @EnableJpaAuditing
    // This allows @CreatedDate and @LastModifiedDate annotations to work
}
