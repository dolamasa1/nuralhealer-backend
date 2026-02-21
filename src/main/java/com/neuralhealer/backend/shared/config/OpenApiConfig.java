package com.neuralhealer.backend.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Access docs at:
 * - Swagger UI: /swagger
 * - OpenAPI JSON: /docs
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "NeuralHealer API", version = "0.5.0", description = "Healthcare Platform Backend API - Doctor-Patient Engagements & AI Chat", contact = @Contact(name = "NeuralHealer Team", email = "support@neuralhealer.com"), license = @License(name = "Proprietary", url = "https://neuralhealer.com/license")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token authentication. Use the token from /auth/login response.")
public class OpenApiConfig {
    // Configuration is done via annotations
}
