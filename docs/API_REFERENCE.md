# API Reference - NeuralHealer

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

## 🔌 4. WebSockets (STOMP)

Endpoint: `ws://localhost:8080/ws`

### Topics (Subscribe)
- `/topic/engagement/{id}`: Live chat and status updates.
- `/topic/user/{userId}`: Personal notifications.

### Destinations (Send)
- `/app/engagement/{id}/message`: Send chat message.
- `/app/engagement/{id}/typing`: Send typing indicator.

---

## 🤖 5. AI Chatbot

Absolute configuration values for AI integration.

### A. Health Check (REST)
**URL**: `http://localhost:8080/api/ai/health`
**Method**: `GET`
**Auth**: None (Public)
**Response**:
```json
{
  "connected": true,
  "message": "AI Service available",
  "lastChecked": "2026-01-08T03:00:00"
}
```

### B. Ask Question (REST)
**URL**: `http://localhost:8080/api/ai/ask`
**Method**: `POST`
**Auth**: Bearer Token or Cookie
**Body**:
```json
{
  "question": "Hello AI"
}
```

### C. Ask Question (WebSocket)
**1. Connect**
*   **URL**: `ws://localhost:8080/api/ws`
*   **Protocol**: STOMP

**2. Listen (Subscribe)**
*   **Destination**: `/user/queue/ai`
*   *(Receives answers here)*

**3. Send**
*   **Destination**: `/app/ai/ask`
*   **Body**:
    ```json
    {
      "question": "Hello via WebSocket"
    }
    ```

