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
