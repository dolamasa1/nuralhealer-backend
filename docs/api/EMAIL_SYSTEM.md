# Email System Documentation

The NeuralHealer email system provides two primary methods for delivering emails: **Direct Delivery** (immediate) and **Queue-Based Delivery** (notifications).

## 1. Architecture Overview

### Direct Email Flow (Immediate)
Used for critical, time-sensitive emails like password resets and verification codes. These bypass the database queue and are sent immediately via the SMTP server.

```mermaid
sequenceDiagram
    participant App as Application Service
    participant DirectSvc as DirectEmailService
    participant SmtpSvc as GmailSmtpService
    participant Gmail as Gmail SMTP API
    
    App->>DirectSvc: sendVerificationEmail(email, code)
    DirectSvc->>DirectSvc: Load HTML Template
    DirectSvc->>DirectSvc: Replace Placeholders ({CODE})
    DirectSvc->>SmtpSvc: sendEmail(to, subject, htmlBody)
    SmtpSvc->>Gmail: Send via SMTP.gmail.com:587
    alt Success
        Gmail-->>SmtpSvc: 250 OK
        SmtpSvc-->>DirectSvc: true
    else Failure
        Gmail-->>SmtpSvc: Error
        SmtpSvc-->>DirectSvc: false
        DirectSvc-->>App: Throw EmailSendException
    end
```

### Queue-Based Email Flow (Asynchronous)
Used for non-critical notifications (lifecycle events, welcome messages, etc.) to ensure the main application logic remains fast and scales well.

```mermaid
graph TD
    A[Trigger Event] --> B[UserActivityNotificationJob]
    B --> C[(Postgres: message_queues)]
    
    subgraph Scheduled Processing (Every 1 Minute)
    D[EmailQueueProcessor] -- 1. Query Pending --> C
    D -- 2. Send via SMTP --> E[GmailSmtpService]
    E -- 3. Update Status --> C
    end
    
    C -- status: 'completed' --> F[Final State]
    C -- status: 'failed' (after 3 retries) --> G[Error Log]
```

---

## 2. API Reference (Testing)

Simple endpoints for testing email templates.

### A. Test Verification Email

**Endpoint**: `POST /test/email/verification`

**Request Body**:
```json
{
  "email": "test@example.com",
  "code": "987654"
}
```

**Response**:
```json
{
  "message": "Verification email sent to test@example.com"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/test/email/verification \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"987654"}'
```

---

### B. Test Password Reset Email

**Endpoint**: `POST /test/email/password-reset`

**Request Body**:
```json
{
  "email": "test@example.com",
  "token": "my-secret-token"
}
```

**Response**:
```json
{
  "message": "Password reset email sent to test@example.com"
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8080/api/test/email/password-reset \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","token":"my-secret-token"}'
```

---

## 3. Configuration

The system uses the following environment variables:
- `GMAIL_USERNAME`: Your Gmail address.
- `GMAIL_APP_PASSWORD`: 16-character App Password (with no spaces).

## 4. Troubleshooting

| Issue | Possible Cause | Solution |
|-------|----------------|----------|
| `AuthenticationFailedException` | Invalid App Password | Re-generate App Password in Google Settings. |
| `MessagingException: Could not connect` | Firewall or Port 587 blocked | Ensure port 587 is open for outbound traffic. |
| Noise in Terminal | DEBUG logging level | Check `application.yml` and ensure `logging.level.com.neuralhealer.backend.integration.gmail` is set to `INFO`. |
