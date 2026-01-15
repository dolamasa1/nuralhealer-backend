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

## 🤝 2. Engagements

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/engagements/initiate` | Start Pending Engagement | Doctor |
| `POST` | `/engagements/verify-start` | Activate Engagement | Patient |
| `GET` | `/engagements/my-engagements` | List My Engagements | Yes |
| `DELETE` | `/engagements/{id}` | Cancel Pending Engagement | Doctor |
| `POST` | `/engagements/{id}/end-request` | Request Engagement End | Yes |
| `POST` | `/engagements/{id}/verify-end` | Conclude Engagement | Yes |

---

## 💬 3. Messaging (REST Fallback)

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/engagements/{id}/messages` | Send Message (REST) | Yes |
| `GET` | `/engagements/{id}/messages` | Get History | Yes |

---

## 🔌 4. WebSockets

NeuralHealer supports two WebSocket protocols for different real-time Paradigms.

### A. Managed Broker (STOMP)
**Endpoint**: `ws://localhost:8080/ws`  
**Purpose**: High-reliability, bi-directional communication (Chat, AI).

#### Topics (Subscribe)
- `/topic/engagement/{id}`: Live chat and status updates.
- `/topic/user/{userId}`: Personal notifications.
- `/user/queue/ai`: Direct AI responses (Session-specific).

#### Destinations (Send)
- `/app/engagement/{id}/message`: Send chat message.
- `/app/ai/ask`: Ask AI a question.

---

### B. Raw Pathway (Standard WS)
**Endpoint**: `ws://localhost:8080/notifications`  
**Purpose**: Lightweight broadcasts for future features (Appointment alerts).

---

## 🤖 5. AI Chatbot

AI integration is now unified under the STOMP broker for production readiness.

- **STOMP Destination**: `/app/ai/ask`
- **STOMP Queue**: `/user/queue/ai`
- **Documentation**: See [AI_WEBSOCKET_API.md](AI_WEBSOCKET_API.md) for payload details.

