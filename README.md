# NeuralHealer Backend

**Version:** 0.1.0  
**Status:** In Development (Phase 3 Completed)

## 📋 Overview

NeuralHealer is an advanced healthcare platform designed to facilitate secure, regulated, and AI-enhanced engagements between doctors and patients. The backend is built with **Spring Boot 3**, leveraging a robust **PostgreSQL** database with complex PL/pgSQL triggers to handle business logic consistency at the data layer.

This repository contains the monolithic backend service which manages:
- User Authentication & Authorization (JWT)
- Profile Management (Doctor & Patient)
- Engagement Lifecycle (Initiation, 2FA Verification, Termination)
- Secure Messaging with Access Control

---

## 🛠 Technology Stack

### Core
- **Java 21**: Core language.
- **Spring Boot 3.2.5**: Application framework.
- **Spring Security 6**: Authentication & Authorization.
- **Spring Data JPA**: Database abstraction.

### Database
- **PostgreSQL 15**: Primary data store.
- **Liquibase / Schema.sql**: Database initialization (using `schema.sql` with PL/pgSQL support).
- **Triggers**: Automated valid logic (e.g., `update_relationship_status_on_engagement`, `generate_engagement_id`).

### Infrastructure & Tools
- **Docker & Docker Compose**: Database containerization.
- **Maven**: Build tool.
- **OpenAPI / Swagger UI**: API documentation (`/api/swagger-ui/index.html`).

---

## 🚀 Features & Modules

### 1. Authentication System ✅
- **Role-Based Access**: `DOCTOR`, `PATIENT`, `ADMIN`.
- **JWT Security**: Stateless authentication with bearer tokens.
- **Registration**: Separate flows for Doctors (with professional details) and Patients (with medical history placeholders).

### 2. Engagement System (Phase 3) ✅
A regulated workflow for doctor-patient interactions:
1.  **Initiation**: Doctor requests an engagement with a specific access rule (e.g., `FULL_ACCESS`).
2.  **2FA Verification**: System generates a verification token. Patient verifies this token to Activate the engagement.
3.  **Automated Updates**: Database triggers automatically update the `doctor_patients` relationship table when engagement status changes to `active`.
4.  **Secure Messaging**: Messages can only be exchanged between parties with an active engagement or sufficient access rights.
5.  **Termination**: Controlled ending of engagements with retention policy enforcement (e.g., reverting to `READ_ONLY_ACCESS`).

---

## 🔌 API Documentation

Base URL: `http://localhost:8080/api`

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register new user (Doctor/Patient) | No |
| `POST` | `/auth/login` | Login and receive JWT | No |
| `GET` | `/users/me` | Get current user profile | **Yes** |

### Engagement Endpoints

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/engagements/initiate` | Doctor starts engagement. Returns Token. | **Yes (Doctor)** |
| `POST` | `/engagements/verify-start` | Patient confirms start using Token. | **Yes (Patient)** |
| `POST` | `/engagements/{id}/messages` | Send message in active engagement. | **Yes** |
| `GET` | `/engagements/{id}/messages` | Get message history. | **Yes** |
| `POST` | `/engagements/{id}/end-request` | Request to end engagement. Returns Token. | **Yes** |
| `POST` | `/engagements/{id}/verify-end` | Confirm end using Token. | **Yes** |

---

## 🐳 Docker Integration

The project currently uses Docker for the persistence layer.

### Current Setup
- **File**: `docker-compose.yml`
- **Services**:
    - `neuralhealer-db`: PostgreSQL 15 container.
    - **Port**: Maps host `5432` to container `5432`.
    - **Volume**: Persists data to `./data` (host) or Docker volume.
    - **Healthcheck**: Ensures DB is ready before app connection.

### Usage
```bash
# Start Database
docker-compose up -d

# Stop Database
docker-compose down
```

---

## 🔮 Future Architecture & Golang Integration

As NeuralHealer scales, high-throughput components are planned to be offloaded to **Go (Golang)** microservices to ensure low latency and high concurrency.

### Potential Integration Points

1.  **Real-Time Chat Functionality (WebSocket Hub)**
    *   **Current**: HTTP Polling (Java).
    *   **Future (Go)**: A dedicated Go service handling WebSocket connections (`ws://`). Go's lightweight goroutines are ideal for maintaining thousands of persistent connections for live typing indicators and instant message delivery.

2.  **AI Inference Gateway**
    *   **Purpose**: Processing patient data against AI models.
    *   **Future (Go)**: A high-performance gateway that aggregates data from the Java backend and streams it to AI inference engines (Python/TensorFlow), handling timeouts and retries efficiently.

3.  **Audit Logging Sidecar**
    *   **Purpose**: Rapid ingestion of compliance logs.
    *   **Future (Go)**: Asynchronous log collector that writes to immutable storage / Blockchains without blocking the main Java thread.

### Proposed Architecture Change
```mermaid
graph LR
    Client --> API_Gateway
    API_Gateway --> |REST/JSON| SpringBoot_Backend[Java Backend (You are here)]
    API_Gateway --> |WebSocket| Go_Chat_Service[Golang Chat Service]
    SpringBoot_Backend --> PostgresDB[(PostgreSQL)]
    Go_Chat_Service --> Redis[(Redis Pub/Sub)]
    Go_Chat_Service --> PostgresDB
```

---

## 🏃‍♂️ Setup & Installation

### Prerequisites
- Java 21 SDK
- Docker & Docker Compose
- Maven (wrapper included)

### Steps

1.  **Start Database**:
    ```bash
    docker-compose up -d
    ```

2.  **Configure Environment**:
    Ensure `src/main/resources/application.yml` points to the correct DB credentials (default: `postgres`/`aaa`).

3.  **Build & Run**:
    ```bash
    ./mvnw spring-boot:run
    ```
    *First run will initialize the database schema automatically via `schema.sql`.*

4.  **Verify**:
    Access the health endpoint:
    ```bash
    curl http://localhost:8080/api/actuator/health
    ```

---

**© 2026 NeuralHealer Team**
