# AI Chat Storage API Documentation

This document details the new APIs implemented for the persistent AI Chat Storage system.

## 1. Patient APIs
**Base Path:** `/api/chats`  
**Access:** Authenticated Users (Patients)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chats` | Retrieve all chat sessions for the current user, ordered by most recent. |
| `GET` | `/api/chats/with-doctors` | **NEW** Optimized endpoint returning sessions with embedded authorized doctors list methods. |
| `GET` | `/api/chats/search?q={query}` | Search sessions by title or message content. |
| `GET` | `/api/chats/authorized-doctors` | List doctors who have permission to view your chat history. |
| `GET` | `/api/chats/{sessionId}/messages` | Retrieve full message history for a specific session. |
| `PUT` | `/api/chats/{sessionId}/title` | Rename a chat session (e.g., "Anxiety Consultation"). |

### Examples

**Get All Sessions**
```http
GET /api/chats
Authorization: Bearer <token>
```

**Update Session Title**
```http
PUT /api/chats/{sessionId}/title
Content-Type: text/plain

My New Session Title
```

**Get Sessions with Authorized Doctors (Optimized)**
```http
GET /api/chats/with-doctors
Authorization: Bearer <token>
```
*Returns sessions with an embedded list of doctors who have access permission.*

**Get Authorized Doctors List**
```http
GET /api/chats/authorized-doctors
Authorization: Bearer <token>
```

**Search Sessions**
```http
GET /api/chats/search?q=anxiety
Authorization: Bearer <token>
```

---

## 2. Doctor APIs
**Base Path:** `/api/doctors`  
**Access:** Authenticated Doctors (Must have valid relationship with patient)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/doctors/patients/{patientId}/chats` | View all chat sessions for a specific patient. |
| `GET` | `/api/doctors/patients/{patientId}/chats/{sessionId}/messages` | View message details for a specific patient session. |

### Security Note
- Doctors can **strictly** only access data for patients they have an active or historical relationship with in the `doctor_patients` table.
- Attempts to access unauthorized patients result in `403 Forbidden`.

---

## 3. WebSocket Integration
**Protocol:** STOMP over WebSocket  
**Access:** Authenticated Users

The existing AI chat flow has been enhanced to **automatically persist** all interactions.

| Direction | Destination | Action | Persistence |
|-----------|-------------|--------|-------------|
| **Send** | `/app/ai/ask` | User sends a question | ✅ User message saved asynchronously |
| **Receive** | `/user/queue/ai` | AI sends response | ✅ AI response saved asynchronously |

### Data Flow
1. **User sends:** `{"question": "How do I manage stress?"}` to `/app/ai/ask`
2. **System:**
   - Finds active session OR creates new one.
   - Saves User message to DB (Async).
   - Sends "Typing..." status.
   - Calls AI Service.
   - Sends AI Response to `/user/queue/ai`.
   - Saves AI Response to DB (Async).

---

## 4. Data Models

### AiChatSession
```json
{
  "id": "uuid",
  "patientId": "uuid",
  "sessionTitle": "General Chat",
  "startedAt": "2023-10-27T10:00:00",
  "isActive": true,
  "messageCount": 5
}
```

### AiChatMessage
```json
{
  "id": "uuid",
  "sessionId": "uuid",
  "senderType": "PATIENT | AI",
  "content": "Hello, I need help.",
  "sentAt": "2023-10-27T10:00:05"
}
```
### AuthorizedDoctorResponse
```json
{
  "doctorId": "uuid",
  "fullName": "Dr. Sarah Johnson",
  "title": "Clinical Psychologist",
  "specialities": ["CBT", "Anxiety Disorders"],
  "accessLevel": "Full Access",
  "isCurrentlyActive": true
}
```

### SessionWithDoctorsResponse (Enriched)
```json
{
  "sessionId": "uuid",
  "sessionTitle": "Anxiety Management",
  "sessionType": "general",
  "startedAt": "2023-10-27T10:00:00",
  "endedAt": null,
  "isActive": true,
  "messageCount": 12,
  "authorizedDoctors": [
    {
      "doctorId": "uuid",
      "fullName": "Dr. Sarah Johnson",
      "title": "Clinical Psychologist",
      "specialities": ["CBT"],
      "accessLevel": "Full Access",
      "isCurrentlyActive": true
    }
  ]
}
```
