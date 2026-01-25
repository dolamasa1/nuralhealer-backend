# Quiz API Reference

Documentation for the Personality Assessment Quiz APIs in NeuralHealer.

---
**Last Updated:** 2026-01-21
**Version:** 0.5.0
---

## 🧩 1. IPIP-120 Quiz
Standard 120-question personality assessment.

**Base URL**: `/api/quizzes/ipip120`

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/start` | Start a new session. Returns `sessionId`. | No |
| `GET` | `/questions` | Get all questions or a specific one with `?questionId=X`. | Session |
| `GET` | `/responses` | Get all saved responses for the current session. | Session |
| `POST` | `/submit-question` | Save response for a single question. | Session |
| `POST` | `/submit-quiz` | Final submission and scoring. | Session |

### Headers
- `X-Quiz-Session-120`: The sessionId returned from `/start`.
- `X-Quiz-Session`: Fallback header for session tracking.

---

## 🧩 2. IPIP-50 Quiz
Standard 50-question personality assessment (shorter version).

**Base URL**: `/api/quizzes/ipip50`

| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/start` | Start a new session. Returns `sessionId`. | No |
| `GET` | `/questions` | Get all questions or a specific one with `?questionId=X`. | Session |
| `GET` | `/progress` | Get current progress (percentage and count). | Session |
| `GET` | `/responses` | Get all saved responses with question text. | Session |
| `POST` | `/submit-question` | Save response for a single question. | Session |
| `POST` | `/submit-quiz` | Final submission and scoring. | Session |
| `DELETE` | `/reset` | Clear all responses and session data. | Session |

### Headers
- `X-Quiz-Session`: The sessionId returned from `/start`.

---

## 📊 Shared Models

### Quiz Response
```json
{
  "questionId": 1,
  "score": 5
}
```

### Quiz Result Summary
```json
{
  "result": {
    "userId": "user-123",
    "scores": [
      {
        "trait": "Extraversion",
        "score": 45,
        "interpretation": "High score implies..."
      }
    ],
    "summary": "Full report text..."
  },
  "sessionId": "abc-123",
  "completionDate": "2025-01-18T..."
}
```

> [!NOTE]
> Sessions for IPIP-120 expire in 2 hours, while IPIP-50 sessions expire in 1 hour.
