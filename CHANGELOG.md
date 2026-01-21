# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-01-21

### Added
- Created root `VERSION` file as the single source of truth for project versioning.
- Added project metadata and versioning to Spring Boot Actuator `/info` endpoint.
- Created `application-dev.yml` and `application-prod.yml` profiles.
- Added exhaustive `Environment Configuration` section to README.
- Created `.env.example` for environment variable management.

### Changed
- **Unified Versioning**: Synchronized version 0.5.0 across all 13+ documentation files and `pom.xml`.
- **Java Upgrade**: Aligned project to Java 21 across code and documentation.
- **Security Hardening**: Externalized all secrets (DB, JWT, AI Service) to environment variables in `application.yml`.
- Updated Swagger UI path to `/api/swagger` and API docs to `/api/docs`.

### Fixed
- Fixed numerous version inconsistencies across the project documentation.
