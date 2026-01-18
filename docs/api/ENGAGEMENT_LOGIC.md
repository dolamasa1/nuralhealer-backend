# Engagement System Logic - NeuralHealer

---
**Last Updated:** 2025-01-15
**Version:** 0.6
**Changes:** 
- Documentation cleanup
- Clarified WebSocket event paths within the STOMP broker
- Reflected the removal of QR codes definitively
---

This document defines the core logic, state machine, and API protocols for the NeuralHealer Engagement system.

---

## 🏗️ 1. Conceptual Overview
An **Engagement** is a secure, documented, and time-bound interaction between a **Doctor** and a **Patient**. It is governed by an **Access Rule** that defines what data the doctor can access during and after the engagement.

### Key Components:
- **Engagement**: The central entity tracking the relationship status.
- **Verification Token**: A secure 2FA token used to transition between critical states (Start/End).
- **Access Rule**: A policy-based permission sets (e.g., `FULL_ACCESS`, `READ_ONLY`).
- **Doctor-Patient Relationship**: A semi-permanent link updated by engagement lifecycle events.

---

## 🔄 2. Engagement State Machine

The engagement lifecycle flows through several states, ensuring mutual consent and data protection.

```mermaid
stateDiagram-v2
    [*] --> pending: Doctor initiates
    pending --> active: Patient verifies (Start Token)
    pending --> cancelled: Doctor cancels
    active --> ended: Token Verification (End Flow)
    ended --> archived: Retention/Archive Policy
    ended --> [*]
    cancelled --> [*]
```

### State Definitions:

| Status | Description | Transitions |
| :--- | :--- | :--- |
| **Pending** | Engagement created but not yet active. | `active` (verify), `cancelled` (delete) |
| **Active** | Live engagement. Messages and AI interactions allowed. | `ended` (verify-end) |
| **Ended** | Engagement concluded. Access restricted based on rules. | `archived` (auto-trigger) |
| **Cancelled** | Aborted before activation. | None |
| **Archived** | Historic data processed for long-term storage. | None |

---

## 📡 3. Engagement Protocols

### 3.1 Initiation & Activation Flow
The Doctor starts the engagement, and the Patient must verify it using a secure token.

```mermaid
sequenceDiagram
    participant D as Doctor
    participant API as Backend
    participant DB as Database
    participant P as Patient

    D->>API: POST /engagements/initiate {patientId, ruleName}
    API->>DB: Create Engagement (status=pending)
    API->>API: Generate Verification Token (Type: START)
    API-->>D: Return Token String
    
    Note over D,P: Doctor shares Token with Patient (manually/verbal)
    
    P->>API: POST /engagements/verify-start {token}
    API->>DB: Validate Token -> status=active
    DB-->>DB: Trigger: Create/Update Doctor-Patient Relation
    API->>API: WebSocket broadcast (started)
    API-->>P: Success (Engagement Active)
    API-->>D: Notification: "Patient shared access"
```

> [!IMPORTANT]
> **Change Log (v0.5)**: QR Code verification has been removed in favor of direct token input for improved accessibility and simplicity.

### 3.2 Termination Flow (Ending an Engagement)
Either party can request to end an engagement. The other party must verify the termination.

```mermaid
sequenceDiagram
    participant U1 as Initiator (U1)
    participant API
    participant DB
    participant U2 as Verifier (U2)

    U1->>API: POST /engagements/{id}/end-request
    API->>DB: Generate Verification Token (Type: END)
    API->>API: WebSocket broadcast (end requested)
    API-->>U2: Notification: "User 1 wants to end engagement"
    
    U2->>API: POST /engagements/{id}/verify-end {token}
    API->>DB: UPDATE status=ENDED
    DB->>DB: Trigger: Archive messages (retention policy)
    API->>API: WebSocket broadcast (ended)
    API-->>U1: Engagement ended
    API-->>U2: Engagement ended
```

---

## 🛠️ 4. API Reference & Response Examples

### 4.1 Initiate Engagement
**Endpoint**: `POST /api/engagements/initiate`  
**Role**: Doctor  
**Request Body**:
```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440000",
  "accessRuleName": "FULL_ACCESS"
}
```

**Success Response (200 OK)**:
```json
{
  "engagementId": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "status": "PENDING",
  "verification": {
    "token": "NH-123456",
    "expiresAt": "2025-01-15T04:00:00"
  }
}
```

---

### 4.2 Verify Start (Activation)
**Endpoint**: `POST /api/engagements/verify-start`  
**Role**: Patient  
**Request Body**:
```json
{
  "token": "NH-123456"
}
```

**Success Response (200 OK)**:
```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "engagementId": "ENG-2025-001",
  "status": "ACTIVE",
  "doctor": {
    "id": "d1e2f3g4...",
    "firstName": "Ahmed",
    "lastName": "Raafat",
    "email": "doctor@neuralhealer.com"
  },
  "patient": {
    "id": "p1q2r3s4...",
    "firstName": "Sara",
    "lastName": "Ali",
    "email": "patient@neuralhealer.com"
  },
  "accessRule": "FULL_ACCESS",
  "startAt": "2025-01-15T03:00:00"
}
```

---

### 4.3 Request Termination
**Endpoint**: `POST /api/engagements/{id}/end-request`  
**Role**: Any Participant  
**Request Body**:
```json
{
  "reason": "Treatment completed"
}
```

**Success Response (200 OK)**:
```json
{
  "engagementId": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "status": "END_REQUESTED",
  "verification": {
    "token": "END-987654",
    "expiresAt": "2025-01-15T03:30:00"
  }
}
```

---

### 4.4 Verify End (Termination)
**Endpoint**: `POST /api/engagements/{id}/verify-end`  
**Role**: Counter-party  
**Request Body**:
```json
{
  "token": "END-987654"
}
```

**Success Response (200 OK)**:
```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "status": "ENDED",
  "endAt": "2025-01-15T03:15:00"
}
```

---

## 🔔 5. WebSocket Event Schema

Topic: `/topic/engagement/{id}`

| Event Type | Payload Category | Description |
| :--- | :--- | :--- |
| `ENGAGEMENT_STATUS` | `active` | Engagement is now live. |
| `ENGAGEMENT_STATUS` | `end_requested` | A termination request is pending verification. |
| `ENGAGEMENT_STATUS` | `ended` | Engagement has concluded. |
| `ENGAGEMENT_STATUS` | `cancelled` | Pending engagement was aborted. |

---

## 📂 6. Data Archiving & Retention
When an engagement transitions to `ENDED`, the following occurs:
1.  **Relationship Update**: The `doctor_patients` entry is updated with the end date.
2.  **Access Revocation**: If the rule is `NO_ACCESS`, the relationship `is_active` becomes `false`.
3.  **Message Scoping**: Messages are no longer returned in active chat queries but are accessible through the `get_accessible_messages` audit function based on retention rules.
