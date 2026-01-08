# Deployment & Setup - NeuralHealer
**Version:** 0.4

This guide covers the requirements and steps to deploy NeuralHealer in various environments.

---

## 🐳 1. Docker Setup (Recommended)

The easiest way to start the persistence layer is via Docker Compose.

### Quick Start
```bash
docker-compose up -d
```
This starts:
- **PostgreSQL 15**: Primary database on port `5432`.
- **Healthchecks**: Ensures the DB is ready before usage.

---

## 💻 2. Local Development

### Prerequisites
- **Java 21**
- **Maven**
- **Docker**

### Run Steps
1. Start the DB: `docker-compose up -d`.
2. Configure `src/main/resources/application.yml` (Credentials).
3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🚀 3. Production Guidelines

1. **Environment Variables**: Use env vars for sensitive data like `JWT_SECRET` and `DB_PASSWORD`.
2. **HTTPS**: Mandatory. Spring Boot should be behind a reverse proxy (NGINX/Caddy) handled TLS.
3. **Database**: Use a managed RDS instance for production instead of a single Docker container.
4. **CORS**: Set `ALLOWED_ORIGINS` to your production domain.
