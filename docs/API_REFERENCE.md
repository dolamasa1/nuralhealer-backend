# API Reference - NeuralHealer

---
**Last Updated:** 2025-01-15
**Version:** 0.5
**Changes:** 
- Unified Real-Time Plane: Separated STOMP (/ws) and Raw (/notifications)
- AI Integration: Updated to use STOMP destinations
---

Base URL: `http://localhost:8080/api`

---

## 🔑 1. Authentication

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register new User | No |
| `POST` | `/auth/login` | Login & Get JWT Cookie | No |
| `POST` | `/auth/logout` | Revoke Session | Yes |
| `GET` | `/users/me` | Current Profile | Yes |

---

## 🤝 2. Engagements (Lifecycle)

| Method | Endpoint | Description | Auth | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/engagements/initiate` | Start Pending Engagement | Doctor | Verification Token |
| `POST` | `/engagements/verify-start` | Activate Engagement | Patient | Engagement Object |
| `GET` | `/engagements/my-engagements` | List My Engagements | Yes | Array of Engagements |
| `DELETE` | `/engagements/{id}` | Cancel Pending Engagement | Doctor | 204 No Content |
| `POST` | `/engagements/{id}/end-request` | Request Engagement End | Yes | Termination Token |
| `POST` | `/engagements/{id}/verify-end` | Conclude Engagement | Yes | Status Object |

> [!TIP]
> **Detailed Documentation**: See [ENGAGEMENT_LOGIC.md](ENGAGEMENT_LOGIC.md) for full JSON request/response examples for each transition.

---

## 💬 3. Messaging (REST Fallback)

| Method | Endpoint | Description | Auth | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/engagements/{id}/messages` | Send Message (REST) | Yes | Message Object |
| `GET` | `/engagements/{id}/messages` | Get History | Yes | Array of Messages |

---

## 🔌 4. WebSockets

NeuralHealer supports two WebSocket protocols for different real-time Paradigms.

### A. Managed Broker (STOMP)
**Endpoint**: `ws://localhost:8080/ws`  
**Purpose**: High-reliability, bi-directional communication (Chat, AI).

#### Topics (Subscribe)
- `/topic/engagement/{id}`: Live chat and status updates.
- `/topic/user/{userId}`: Personal notifications.
- `/user/queue/ai`: AI-specific events.

#### Destinations (Send)
- `/app/engagement/{id}/message`: Send chat message.
- `/app/ai/ask`: Ask AI a question.

> [!TIP]
> **AI Documentation**: See [AI_SUBSCRIPTION.md](AI_SUBSCRIPTION.md) for detailed JSON payloads for questions, typing, and answers.

---

### B. Raw Pathway (Standard WS)
**Endpoint**: `ws://localhost:8080/notifications`  
**Purpose**: Lightweight broadcasts for future features (Appointment alerts).

---

## 🤖 5. AI Chatbot

AI integration is now unified under the STOMP broker for production readiness.

- **STOMP Destination**: `/app/ai/ask`
- **STOMP Queue**: `/user/queue/ai`
- **Documentation**: See [AI_SUBSCRIPTION.md](AI_SUBSCRIPTION.md) for payload and subscription details.

