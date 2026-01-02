package com.neuralhealer.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Access docs at:
 * - Swagger UI: http://localhost:8080/api/swagger
 * - OpenAPI JSON: http://localhost:8080/api/docs
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "NeuralHealer API", version = "0.1.0", description = "Healthcare Platform Backend API - Doctor-Patient Engagements & AI Chat", contact = @Contact(name = "NeuralHealer Team", email = "support@neuralhealer.com"), license = @License(name = "Proprietary", url = "https://neuralhealer.com/license")), servers = {
        @Server(url = "http://localhost:8080/api", description = "Development Server")
})
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token authentication. Use the token from /auth/login response.")
public class OpenApiConfig {
    // Configuration is done via annotations
}
