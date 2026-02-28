# Assessment System (Quizzes) Documentation

This document covers the API endpoints, the question-by-question logical flow, and the data layer ("DB Part") for the Assessments system (PHQ-9, IPIP-50, IPIP-120).

## 💡 Logical Flow: Question-by-Question
The system is designed for **incremental submission**. Instead of submitting all answers at once, the client sends each answer immediately as the user interacts with the UI.

1.  **Start**: Client calls `/start`. The server initializes a memory-mapped session and returns a `sessionId` (via cookie and body).
2.  **Fetch**: Client calls `/questions` to get the bank of questions.
3.  **The Loop**: For **every single question**, the client calls `POST /submit-question`.
    *   *Why?* This ensures that progress is never lost if the user closes the tab or loses connection.
4.  **Finalize**: Only after all questions are answered does the client call `POST /submit-quiz`.
    *   The server validates the answer count. If any are missing, it returns a `400` with the list of `missingQuestions`.

---

## 🛠️ Data Layer & DB Part
The Assessment system uses a hybrid data model: **Static JSON** for definitions and **In-Memory Sessions** for active progress.

### 1. Static Database (JSON Definitions)
The "Source of Truth" for quizzes is stored in `.json` files in `src/main/resources/quizzes/`. 

*   **`questions.json`**: Defines the test name, instructions (Bilingual), the scale (0-3 or 1-5), and the items.
*   **`interpretations.json`**: Defines the clinical logic.
    *   **Ranges**: Maps score totals (e.g., 10-14) to severity levels (e.g., "Moderate").
    *   **Critical Alerts**: Logic for specific dangerous answers (e.g., Item 9 suicide risk) which triggers a `hasCriticalAlert` flag in the result metadata.

### 2. Session Persistence (In-Memory)
Active sessions are NOT stored in the permanent SQL database (to prevent cluttering the DB with half-finished guest tests).
*   **`QuizService`**: Manages a `ConcurrentHashMap` of active sessions.
*   **Structure**: Each session is a map of `id -> score`.
*   **Expiration**: Sessions stay in memory for the duration of the server uptime or until cleared.

---

## 📡 API Endpoints

### Shared Base: `/api/quizzes/[phq9 | ipip50 | ipip120]`

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/start` | `POST` | Initializes session. Sets `quiz_session_[type]` cookie. |
| `/questions` | `GET` | Returns list of all questions (English and Arabic). |
| `/submit-question` | `POST` | **Called for each answer.** Body: `{ "questionId": int, "score": int }`. |
| `/responses` | `GET` | Returns all answers saved in the current session. |
| `/submit-quiz` | `POST` | Finalizes the quiz. Calculates score and returns the report. |

---

## 📊 Result Example
The final `submit-quiz` returns a report containing clinical severity and potentially safety alerts:

```json
{
  "result": {
    "userId": "guest-uuid",
    "scores": [
      {
        "trait": "Depression Severity",
        "score": 18,
        "level": "Moderately Severe",
        "description": "Active treatment with pharmacotherapy and/or psychotherapy",
        "metadata": {
          "hasCriticalAlert": true,
          "alertMessageEn": "Safety Notice: You indicated thoughts of self-harm..."
        }
      }
    ],
    "summary": "PHQ-9 Depression Screening Completed"
  }
}
```
