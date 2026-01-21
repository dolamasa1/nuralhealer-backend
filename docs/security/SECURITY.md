# Security Manual - NeuralHealer
**Version:** 0.5.0

This document provides a deep dive into the security architecture of the NeuralHealer platform.

---

## 🛡️ 1. Authentication & Authorization

### 1.1 JWT & HTTPOnly Cookies
NeuralHealer uses **Stateful HttpOnly Cookies** to store JWTs. This prevents XSS attacks from stealing identity tokens.
- **Path**: `/api`
- **Security**: `HttpOnly`, `SameSite=Strict`, `Secure` (in prod).
- **Validation**: Performed by `JwtAuthFilter` on every request.

### 1.2 Role-Based Access Control (RBAC)
Roles are enforced at the Controller level using Spring Security's `@PreAuthorize` and manual checks in Services.
- `DOCTOR`: Can initiate and cancel pending engagements.
- `PATIENT`: Can verify and activate engagements.

---

## 🚀 2. Threat Modeling & Mitigations

| Threat | Mitigation | Status |
| :--- | :--- | :--- |
| **XSS** | HTTPOnly Cookies | ✅ Implemented |
| **CSRF** | SameSite=Strict / CSRF Filters | ✅ Implemented |
| **SQL Injection** | JPA / Parameterized Queries | ✅ Implemented |
| **Brute Force** | Rate Limiting | 🚧 Planned (Phase 7) |
| **Unauth Access** | Row-level DB Security (via Triggers) | ✅ Implemented |

---

## 🔒 3. Real-Time Security (WebSockets)

Security for WebSockets is handled via a `ChannelInterceptor`:
1. **Handshake**: The initial HTTP Upgrade request is protected by the same JWT cookie.
2. **CONNECT**: `WebSocketAuthInterceptor` validates the token before allowing the STOMP connection.
3. **Subscribing**: Users can only subscribe to topics matching their `EngagementId`.

---

## 📋 4. Compliance Roadmap

### HIPAA / GDPR Ready
- **Data Minimization**: Only essential health metadata is stored.
- **Audit Trails**: Every state change in an engagement is logged to the `engagements` table with `ended_by` and `termination_reason`.
- **Encryption**: Data in transit is encrypted via TLS (infrastructure layer).

---

🔍 **Code Reference**:
- [SecurityConfig.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/config/SecurityConfig.java)
- [WebSocketAuthInterceptor.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/security/WebSocketAuthInterceptor.java)
