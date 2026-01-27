# 📬 Notification System - API Guide
## NeuralHealer Platform

---
**Audience:** Frontend Developers, API Consumers, Integration Teams  
**Version:** 3.0.0  
**Last Updated:** January 27, 2026  
**Status:** ✅ Production Ready

---

## 📋 Table of Contents

1. [System Overview](#1-system-overview)
2. [Dual-Brain Architecture](#2-dual-brain-architecture)
3. [Notification Types](#3-notification-types)
4. [Real-Time Delivery (SSE)](#4-real-time-delivery-sse)
5. [API Endpoints](#5-api-endpoints)
6. [Notification Templates](#6-notification-templates)
7. [Integration Guide](#7-integration-guide)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. System Overview

### 1.1 What is the Notification System?

The Notification System delivers **real-time alerts** about important platform events using Server-Sent Events (SSE) for instant delivery.

### 1.2 Key Features

| Feature | Description |
|---------|-------------|
| ⚡ Real-time Push | Instant delivery via SSE (no polling) |
| 🔄 Auto Reconnection | Automatic catch-up after network issues |
| 📊 Full History | Persistent notification records |
| 🎯 Priority-based | HIGH/NORMAL/LOW with different UI treatment |
| 📧 Multi-channel | SSE (primary) + Email (backup for HIGH priority) |

### 1.3 Core Entities

| Entity | Purpose |
|--------|---------|
| `notifications` | Stores all notification records |
| `message_queues` | Async email delivery queue |
| `notification_templates` | I18n message templates |

---

## 2. Dual-Brain Architecture

### 2.1 Architecture Concept

The system uses a **hybridized approach** where notifications are created by TWO intelligent components:

```mermaid
graph TB
    subgraph "TRIGGER SOURCES"
        T1[User Signup]
        T2[Engagement Status Change]
        T3[New Message]
        T4[User Inactive 3 Days]
        T5[User Inactive 14 Days]
    end

    subgraph "🧠 BRAIN 1: DATABASE"
        DB1[PostgreSQL Triggers]
        DB2[send_welcome_notification]
        DB3[create_engagement_notification]
        DB4[get_notification_message<br/>I18n Template Engine]
    end

    subgraph "🧠 BRAIN 2: BACKEND"
        BE1[Scheduled Jobs]
        BE2[NotificationCreatorService]
        BE3[Real-time Services]
    end

    subgraph "STORAGE"
        ST1[(notifications table)]
        ST2[(message_queues table)]
    end

    subgraph "DELIVERY"
        D1[SSE Stream]
        D2[Email Job Processor]
    end

    T1 --> DB1
    T2 --> DB1
    T3 --> BE3
    T4 --> BE1
    T5 --> BE1

    DB1 --> DB2
    DB1 --> DB3
    DB2 --> DB4
    DB3 --> DB4

    BE1 --> BE2
    BE3 --> BE2

    DB4 --> ST1
    BE2 --> ST1
    ST1 --> ST2

    ST1 --> D1
    ST2 --> D2

    D1 --> U[👤 User Browser]
    D2 --> U

    style DB1 fill:#4CAF50,color:#fff
    style DB2 fill:#4CAF50,color:#fff
    style DB3 fill:#4CAF50,color:#fff
    style DB4 fill:#4CAF50,color:#fff
    style BE1 fill:#2196F3,color:#fff
    style BE2 fill:#2196F3,color:#fff
    style BE3 fill:#2196F3,color:#fff
```

### 2.2 Responsibility Matrix

| Logic Layer | Responsibility | Component |
|-------------|----------------|-----------|
| **Main Brain (DB)** | Engagement State & Lifecycle | `create_engagement_notification()` (Trigger) |
| **Lifecycle Logic (DB)** | Welcome Messages | `send_welcome_notification()` (Trigger) |
| **Time-Based Logic (App)** | Inactivity (3d, 14d) | `UserActivityNotificationJob` (Spring) |
| **Logic Layer (API)** | Real-time Operations & AI | `NotificationCreatorService` (Spring) |
| **I18n Engine** | Centralized Templates & Rendering | `get_notification_message()` (SQL Helper) |

### 2.3 Why Two Brains?

✅ **Database Triggers** react instantly to data changes (microseconds)  
✅ **Backend Services** handle complex logic and scheduled tasks  
✅ **Combined** they provide reliability and flexibility

---

## 3. Notification Types

### 3.1 Type Hierarchy

```mermaid
graph TD
    A[Notification Types] --> B[ENGAGEMENT]
    A --> C[MESSAGE]
    A --> D[SYSTEM]
    A --> E[AI]

    B --> B1[ENGAGEMENT_PENDING]
    B --> B2[ENGAGEMENT_STARTED]
    B --> B3[ENGAGEMENT_CANCELLED]
    B --> B4[ENGAGEMENT_ENDED]

    C --> C1[MESSAGE_RECEIVED]
    C --> C2[ATTACHMENT_RECEIVED]

    D --> D1[SECURITY_ALERT]
    D --> D2[ACCOUNT_UPDATE]
    D --> D3[USER_REENGAGE]
    D --> D4[USER_INACTIVE_WARNING]

    E --> E1[AI_RESPONSE_READY]
    E --> E2[ANALYSIS_COMPLETE]

    style B fill:#FFC107
    style C fill:#2196F3
    style D fill:#F44336
    style E fill:#9C27B0
```

### 3.2 Priority Mapping

| Type | Priority | SSE | Email | Sound |
|------|----------|-----|-------|-------|
| `ENGAGEMENT_*` | **HIGH** | ✅ | ✅ | ✅ |
| `SECURITY_ALERT` | **HIGH** | ✅ | ✅ | ✅ |
| `MESSAGE_RECEIVED` | NORMAL | ✅ | ❌ | ✅ |
| `USER_REENGAGE` | NORMAL | ✅ | ✅ | ❌ |
| `ACCOUNT_UPDATE` | LOW | ✅ | ❌ | ❌ |

---

## 4. Real-Time Delivery (SSE)

### 4.1 Connection Flow

```mermaid
sequenceDiagram
    participant Client
    participant Server
    participant DB

    Client->>Server: GET /api/notifications/stream
    Server-->>Client: 200 OK (Connection open)
    
    loop Every 30s
        Server-->>Client: ♥ heartbeat
    end

    Note over DB: New notification created
    
    DB->>Server: Notify event
    Server->>Server: Find active user connections
    Server-->>Client: Push notification
    
    Client->>Client: Show toast & update badge
```

### 4.2 Event Format

**SSE Event Structure:**
```
id: {UUID}_{EPOCH_TIMESTAMP}
event: notification
data: {JSON_PAYLOAD}
```

**Example:**
```
id: 550e8400-e29b-41d4-a716-446655440000_1737981600
event: notification
data: {"id":"550e8400-...","type":"MESSAGE_RECEIVED","title":"New Message","message":"You have a message from Dr. Smith","priority":"NORMAL","isRead":false}
```

### 4.3 Reconnection & Catch-up

```mermaid
sequenceDiagram
    participant Client
    participant Server

    Note over Client: Connection drops
    
    Client->>Server: Reconnect (Last-Event-ID: uuid_timestamp)
    Server->>Server: Parse timestamp from ID
    Server->>Server: Query missed notifications
    Server-->>Client: Replay missed events
    Server-->>Client: Resume live stream
    
    Note over Client,Server: Client is up-to-date
```

**Replay Window:** 30 minutes (configurable)

---

## 5. API Endpoints

### 5.1 Endpoint Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/notifications/stream` | Connect to SSE stream |
| `GET` | `/api/notifications` | Get notification history |
| `PUT` | `/api/notifications/{id}/read` | Mark as read |
| `GET` | `/api/notifications/unread-count` | Get unread count |

### 5.2 SSE Stream

**Request:**
```http
GET /api/notifications/stream
Authorization: Bearer {token}
Accept: text/event-stream
```

**Response:** Continuous event stream

### 5.3 Get Notification History

**Request:**
```http
GET /api/notifications?page=0&size=20&sort=sentAt,desc
Authorization: Bearer {token}
```

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "ENGAGEMENT_STARTED",
      "title": "Engagement Activated",
      "message": "Dr. Ahmed has started your engagement",
      "priority": "HIGH",
      "isRead": false,
      "sentAt": "2026-01-27T10:15:30Z",
      "payload": {
        "engagementId": "abc-123",
        "doctorName": "Ahmed Raafat"
      }
    }
  ],
  "totalElements": 42,
  "totalPages": 3
}
```

### 5.4 Mark as Read

**Request:**
```http
PUT /api/notifications/{id}/read
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "notification": {
    "id": "uuid",
    "isRead": true
  }
}
```

### 5.5 Get Unread Count

**Request:**
```http
GET /api/notifications/unread-count
Authorization: Bearer {token}
```

**Response:**
```json
{
  "count": 5
}
```

---

## 6. Notification Templates

### 6.1 Template Structure

All notification messages use **I18n templates** stored in the database via `get_notification_message()` SQL function.

### 6.2 Engagement Templates

| Type | English Template | Arabic Template |
|------|------------------|-----------------|
| `ENGAGEMENT_PENDING` | "Dr. {doctorName} wants to start an engagement with you" | "د. {doctorName} يريد بدء متابعة معك" |
| `ENGAGEMENT_STARTED` | "Your engagement with Dr. {doctorName} is now active" | "متابعتك مع د. {doctorName} نشطة الآن" |
| `ENGAGEMENT_CANCELLED` | "Dr. {doctorName} cancelled the engagement" | "د. {doctorName} ألغى المتابعة" |
| `ENGAGEMENT_ENDED` | "Your engagement with Dr. {doctorName} has ended" | "انتهت متابعتك مع د. {doctorName}" |

### 6.3 Welcome Templates

| Type | English Template | Arabic Template |
|------|------------------|-----------------|
| `WELCOME_PATIENT` | "Welcome to NeuralHealer, {firstName}! Complete your profile to get started." | "مرحباً بك في NeuralHealer، {firstName}! أكمل ملفك الشخصي للبدء." |
| `WELCOME_DOCTOR` | "Welcome Dr. {lastName}! Your account is now active." | "مرحباً د. {lastName}! حسابك نشط الآن." |

### 6.4 Re-engagement Templates

| Type | English Template | Arabic Template | Trigger |
|------|------------------|-----------------|---------|
| `USER_REENGAGE_ACTIVE` | "We miss you, {firstName}! Check your health dashboard." | "نفتقدك، {firstName}! تحقق من لوحة الصحة الخاصة بك." | 3 days inactive |
| `USER_INACTIVE_WARNING` | "Your account will be deactivated soon. Log in to keep it active." | "سيتم تعطيل حسابك قريباً. سجل الدخول للحفاظ عليه نشطاً." | 14 days inactive |

### 6.5 System Templates

| Type | English Template | Arabic Template |
|------|------------------|-----------------|
| `SECURITY_ALERT` | "New login from {location} at {time}" | "تسجيل دخول جديد من {location} في {time}" |
| `ACCOUNT_UPDATE` | "Your profile has been updated successfully" | "تم تحديث ملفك الشخصي بنجاح" |

### 6.6 Message Templates

| Type | English Template | Arabic Template |
|------|------------------|-----------------|
| `MESSAGE_RECEIVED` | "New message from {senderName}" | "رسالة جديدة من {senderName}" |
| `ATTACHMENT_RECEIVED` | "{senderName} sent you a file: {fileName}" | "{senderName} أرسل لك ملف: {fileName}" |

### 6.7 AI Templates

| Type | English Template | Arabic Template |
|------|------------------|-----------------|
| `AI_RESPONSE_READY` | "Your AI analysis is ready to view" | "تحليل الذكاء الاصطناعي الخاص بك جاهز للعرض" |
| `ANALYSIS_COMPLETE` | "Analysis of {reportName} completed" | "اكتمل تحليل {reportName}" |

### 6.8 Template Variables

Common placeholders used in templates:

| Variable | Description | Example |
|----------|-------------|---------|
| `{firstName}` | User's first name | "Ahmed" |
| `{lastName}` | User's last name | "Raafat" |
| `{doctorName}` | Full doctor name | "Dr. Ahmed Raafat" |
| `{senderName}` | Message sender name | "Dr. Smith" |
| `{location}` | Login location | "Cairo, Egypt" |
| `{time}` | Timestamp | "10:30 AM" |
| `{fileName}` | Attachment filename | "report.pdf" |
| `{reportName}` | Report title | "Blood Test Results" |

---

## 7. Integration Guide

### 7.1 Basic SSE Client

```javascript
// Connect to notification stream
const eventSource = new EventSource('/api/notifications/stream');

// Listen for notifications
eventSource.addEventListener('notification', (event) => {
  const notification = JSON.parse(event.data);
  handleNotification(notification);
});

// Handle connection errors
eventSource.onerror = () => {
  // Browser auto-reconnects with Last-Event-ID
};
```

### 7.2 Notification Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Created
    Created --> Delivered: SSE Push
    Created --> Queued: User Offline
    Queued --> Delivered: User Reconnects
    Delivered --> Read: User Clicks
    Delivered --> Archived: 90 days (unread)
    Read --> Archived: 30 days (read)
    Archived --> [*]: Nightly Cleanup
```

### 7.3 Implementation Steps

1. **Connect** to `/api/notifications/stream` on login
2. **Listen** for `notification` events
3. **Display** toast based on priority (HIGH = red, NORMAL = blue)
4. **Update** badge count
5. **Mark as read** when user interacts
6. **Close** connection on logout

---

## 8. Troubleshooting

### 8.1 Common Issues

| Problem | Solution |
|---------|----------|
| Not receiving notifications | Check SSE connection in Network tab |
| Duplicate notifications | Ensure only one EventSource instance |
| Missing after reconnect | Verify `Last-Event-ID` header is sent |
| 401 Unauthorized | Refresh auth token |

### 8.2 Debug Checklist

- [ ] SSE connection shows "open" status
- [ ] Auth token is valid
- [ ] No CORS errors in console
- [ ] Browser supports EventSource API
- [ ] Only one connection per user

### 8.3 Connection States

```mermaid
stateDiagram-v2
    [*] --> Connecting
    Connecting --> Connected: Success
    Connecting --> Failed: Error
    Connected --> Disconnected: Network Issue
    Disconnected --> Reconnecting: Auto Retry
    Reconnecting --> Connected: Success
    Reconnecting --> Failed: Max Retries
    Failed --> [*]
```

---

**Need Help?**  
📧 Backend Team: backend@neuralhealer.com  
📖 Full Spec: [Notification System Master](./notification-system-complete.md)

---

**Version:** 3.0.0  
**Last Updated:** January 27, 2026  
**Status:** ✅ Production Ready