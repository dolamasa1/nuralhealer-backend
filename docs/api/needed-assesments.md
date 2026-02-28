# NeuralHealer — Assessment Platform Plan
> Stack: Spring Boot + PostgreSQL
> Assessments: PHQ-9, IPIP-50, IPIP-120

---

## Table of Contents

1. [What Already Exists vs What to Create](#1-what-already-exists-vs-what-to-create)
2. [Database — Full Declaration](#2-database--full-declaration)
3. [Test Quality Controls](#3-test-quality-controls)
4. [API Design](#4-api-design)
5. [Question Flow Logic](#5-question-flow-logic)
6. [Resume & Restart Logic](#6-resume--restart-logic)
7. [Scoring & Interpretation](#7-scoring--interpretation)
8. [Tracking & History](#8-tracking--history)
9. [Notifications Integration](#9-notifications-integration)
10. [Spring Boot Layer](#10-spring-boot-layer)
11. [Migration Checklist](#11-migration-checklist)

---

## 1. What Already Exists vs What to Create

### ✅ Already Exists — Do NOT Touch

| What | Where | Used For |
|---|---|---|
| `users` table | DB | Base user identity |
| `patient_profiles` table | DB | Assessment attempts link here (same as `ai_chat_sessions`) |
| `doctor_profiles` table | DB | Therapist access |
| `doctor_patients` table | DB | Doctor ↔ patient relationship |
| `engagement_access_rules` table | DB | Controls what doctor can see (FULL_ACCESS, READ_ONLY, etc.) |
| `notifications` table | DB | Stores all notifications |
| `notification_message_templates` table | DB | Template definitions for all notifications |
| `message_queues` table | DB | Email job queue (triggered automatically) |
| `create_system_notification()` function | DB (PG function) | Used as-is for assessment alerts |
| `update_updated_at_column()` function | DB (PG function) | Used as-is on new tables |
| `trigger_queue_email_job()` function | DB (PG function) | Auto-fires on notification insert |
| Static JSON files (`questions.json`, `interpretations.json`) | `src/main/resources/quizzes/` | Question bank — stays here |
| `QuizService` (current) | Spring Boot | Will be **replaced**, not extended |

---

### 🆕 Needs to Be Created

#### Database (New)

| What | Type | Purpose |
|---|---|---|
| `assessment_attempts` | New table | One row per user per test session |
| `assessment_answers` | New table | One row per question per attempt |
| `assessment_results` | New table | One row per trait per completed attempt |
| `quality_flags` column | On `assessment_attempts` | Stores timing/trap/straight-line results as JSONB |
| `is_valid` column | On `assessment_attempts` | Boolean flag — false if critical quality rule failed |
| `ASSESSMENT_COMPLETE` | 2 rows in `notification_message_templates` (EN + AR) | Notification when test finishes |
| `ASSESSMENT_CRITICAL_ALERT` | 2 rows in `notification_message_templates` (EN + AR) | Notification when PHQ-9 Item 9 ≥ 1 |
| Trigger on `assessment_attempts` | Trigger | Auto-update `updated_at` (reuses existing function) |

#### Spring Boot (New)

| What | Replaces / Extends |
|---|---|
| `AssessmentSessionService` | Replaces `QuizService` entirely |
| `AssessmentCompleteService` | New — handles scoring + quality + persist |
| `AssessmentQualityService` | New — timing, trap, straight-line checks |
| `TrackingService` | New — history, trends, curriculum average |
| `AssessmentController` | Replaces current quiz controllers |
| `TrackingController` | New |
| `AssessmentAttempt` entity | New |
| `AssessmentAnswer` entity | New |
| `AssessmentResult` entity | New |
| `QuestionBankService` | Replaces current JSON loading logic — `@PostConstruct` |

#### Static JSON (Additions Only)

| What | Change |
|---|---|
| IPIP-50 questions JSON | Add 2 trap questions (id: 51, 52) |
| IPIP-120 questions JSON | Add 2 trap questions |
| PHQ-9 questions JSON | Add 1 trap question |
| Each question object | Add fields: `isTrap`, `expectedScore`, `reversed`, `trait` |

---

## 2. Database — Full Declaration

### 🆕 CREATE: `assessment_attempts`

```sql
CREATE TABLE public.assessment_attempts (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_id      UUID NOT NULL REFERENCES public.patient_profiles(id) ON DELETE CASCADE,
    quiz_type       VARCHAR(20) NOT NULL,       -- 'PHQ9', 'IPIP50', 'IPIP120'
    status          VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                    -- CREATED | IN_PROGRESS | COMPLETED | ABANDONED
    question_order  JSONB NULL,                 -- frozen array of question IDs, set at creation
    quality_flags   JSONB DEFAULT '{}'::jsonb,  -- timing / trap / straight-line results
    is_valid        BOOLEAN DEFAULT TRUE,        -- false if any CRITICAL quality rule fails
    started_at      TIMESTAMP DEFAULT NOW(),
    completed_at    TIMESTAMP NULL,
    abandoned_at    TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),

    CONSTRAINT chk_attempt_status
        CHECK (status IN ('CREATED','IN_PROGRESS','COMPLETED','ABANDONED'))
);

CREATE INDEX idx_attempts_patient   ON assessment_attempts(patient_id, quiz_type, status);
CREATE INDEX idx_attempts_completed ON assessment_attempts(patient_id, completed_at DESC);

-- ✅ Reuses existing function — no new function needed
CREATE TRIGGER update_assessment_attempts_updated_at
    BEFORE UPDATE ON assessment_attempts
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
```

---

### 🆕 CREATE: `assessment_answers`

```sql
CREATE TABLE public.assessment_answers (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id      UUID NOT NULL REFERENCES public.assessment_attempts(id) ON DELETE CASCADE,
    question_id     INT NOT NULL,
    score           INT NOT NULL,
    answered_at     TIMESTAMP DEFAULT NOW(),    -- used for timing quality check

    UNIQUE (attempt_id, question_id)            -- prevents duplicate answers per question
);

CREATE INDEX idx_answers_attempt ON assessment_answers(attempt_id);
```

> No `updated_at` here — answers are write-once. If the user corrects an answer, it's an upsert (ON CONFLICT UPDATE score, answered_at).

---

### 🆕 CREATE: `assessment_results`

```sql
CREATE TABLE public.assessment_results (
    id                  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attempt_id          UUID NOT NULL REFERENCES public.assessment_attempts(id) ON DELETE CASCADE,
    patient_id          UUID NOT NULL REFERENCES public.patient_profiles(id),  -- denormalized
    quiz_type           VARCHAR(20) NOT NULL,                                   -- denormalized
    trait               VARCHAR(100) NOT NULL,
    raw_score           INT NOT NULL,
    normalized_score    FLOAT NULL,             -- 0–100 for cross-quiz comparison
    level               VARCHAR(50) NOT NULL,
    description         TEXT,
    has_critical_alert  BOOLEAN NOT NULL DEFAULT FALSE,
    alert_message_en    TEXT NULL,
    alert_message_ar    TEXT NULL,
    completed_at        TIMESTAMP NOT NULL,                                     -- denormalized

    UNIQUE (attempt_id, trait)
);

CREATE INDEX idx_results_patient_trait ON assessment_results(patient_id, trait, completed_at DESC);
CREATE INDEX idx_results_patient_quiz  ON assessment_results(patient_id, quiz_type, completed_at DESC);
```

> `patient_id`, `quiz_type`, and `completed_at` are denormalized from the attempt so history/dashboard queries hit this table alone with no joins.

---

### 🆕 INSERT: Notification Templates

```sql
-- These go into the existing notification_message_templates table
INSERT INTO notification_message_templates
  (template_key, language_code, title, message, recipient_context, default_priority, channels)
VALUES
  ('ASSESSMENT_COMPLETE', 'en',
   'Assessment Complete',
   'Your {quizType} assessment is complete. View your results.',
   'patient', 'normal',
   '{"sse": true, "push": false, "email": false}'),

  ('ASSESSMENT_COMPLETE', 'ar',
   'اكتمل التقييم',
   'تقييم {quizType} مكتمل. اعرض نتائجك.',
   'patient', 'normal',
   '{"sse": true, "push": false, "email": false}'),

  ('ASSESSMENT_CRITICAL_ALERT', 'en',
   'Important: Safety Notice',
   'Your assessment response requires attention. Please speak with a mental health professional.',
   'patient', 'critical',
   '{"sse": true, "push": false, "email": true}'),

  ('ASSESSMENT_CRITICAL_ALERT', 'ar',
   'مهم: تنبيه السلامة',
   'استجابتك في التقييم تستدعي اهتمامًا. يرجى التحدث مع متخصص.',
   'patient', 'critical',
   '{"sse": true, "push": false, "email": true}');
```

> The existing `trg_auto_queue_email` trigger fires automatically on insert — no changes needed. Email jobs for `ASSESSMENT_CRITICAL_ALERT` will queue themselves.

---

## 3. Test Quality Controls

All quality checks run at `/complete` time. Results stored in `quality_flags` JSONB on the attempt. None require extra tables.

### 3.1 Timing Validation — CRITICAL

Uses `answered_at` per answer (already stored) and `started_at` on the attempt (already stored).

**Rules:**
- Per-question minimum: 2 seconds. Anything faster = user didn't read it.
- Per-question maximum: 30 seconds. Anything slower = distracted or gaming.
- Total test minimum: 100 seconds for a 50-item test.

**Outcome:** Total time < 100 seconds → `is_valid = false`.

```java
// AssessmentQualityService.java
public TimingResult validateTiming(AssessmentAttempt attempt, List<AssessmentAnswer> answers) {
    long totalSeconds = Duration.between(attempt.getStartedAt(),
                            answers.getLast().getAnsweredAt()).getSeconds();
    int tooFastCount = 0;
    int tooSlowCount = 0;

    for (int i = 0; i < answers.size(); i++) {
        Instant prev = (i == 0) ? attempt.getStartedAt() : answers.get(i-1).getAnsweredAt();
        long secs = Duration.between(prev, answers.get(i).getAnsweredAt()).getSeconds();
        if (secs < 2)  tooFastCount++;
        if (secs > 30) tooSlowCount++;
    }

    return TimingResult.builder()
        .totalSeconds(totalSeconds)
        .tooFastCount(tooFastCount)
        .tooSlowCount(tooSlowCount)
        .failed(totalSeconds < 100)
        .build();
}
```

**Stored in `quality_flags`:**
```json
"timing": { "total_seconds": 87, "too_fast_answers": 12, "failed": true }
```

---

### 3.2 Trap Question Validation — HIGH

**Where trap questions are defined:** In the static JSON files — no DB table needed.

```json
{
  "id": 51,
  "isTrap": true,
  "textEn": "To confirm you are paying attention, please select 'Strongly Disagree'.",
  "textAr": "للتأكد من انتباهك، يرجى اختيار 'أعارض بشدة' لهذا السؤال.",
  "expectedScore": 1
}
```

**Injection positions:**
- IPIP-50: inject 2 traps at positions ~20 and ~40.
- IPIP-120: inject 2 traps at positions ~40 and ~80.
- PHQ-9: inject 1 trap at position ~5.

**Rules:**
- Fail 1 trap → `warning: true`, result still valid.
- Fail 2 traps → `is_valid = false`.

**Trap answers are excluded from scoring.**

```java
public TrapResult validateTraps(List<AssessmentAnswer> answers, List<Question> questions) {
    long failed = questions.stream()
        .filter(Question::isTrap)
        .filter(t -> answers.stream().anyMatch(a ->
            a.getQuestionId() == t.getId() && a.getScore() != t.getExpectedScore()))
        .count();

    return TrapResult.builder()
        .failedTraps((int) failed)
        .warning(failed == 1)
        .isInvalid(failed >= 2)
        .build();
}
```

**Stored in `quality_flags`:**
```json
"traps": { "failed": 1, "warning": true, "invalid": false }
```

---

### 3.3 Straight-Line Detection — MEDIUM

**Rule:** 7 or more identical consecutive answers = satisficing. Flagged for therapist, does NOT set `is_valid = false`.

```java
public StraightLineResult detectStraightLining(List<AssessmentAnswer> answers) {
    int maxRun = 1, currentRun = 1;
    for (int i = 1; i < answers.size(); i++) {
        currentRun = answers.get(i).getScore() == answers.get(i-1).getScore()
            ? currentRun + 1 : 1;
        maxRun = Math.max(maxRun, currentRun);
    }
    return StraightLineResult.builder()
        .longestRun(maxRun)
        .flagged(maxRun >= 7)
        .build();
}
```

**Stored in `quality_flags`:**
```json
"straight_lining": { "longest_run": 9, "flagged": true }
```

---

### 3.4 Progress Bar — LOW (Frontend Only)

Backend already provides the data. Every `/next-question` response includes:
```json
"progress": { "answered": 34, "total": 52, "percentComplete": 65.4 }
```

Frontend renders progress bar from this. For IPIP-50/120: paginate 10 questions per page (client buffers 10 calls to `/next-question`, renders page, advances). For PHQ-9: single page.

---

## 4. API Design

Base: `/api/assessments/{quizType}` — quizType is `phq9`, `ipip50`, or `ipip120`.

| Method | Endpoint | Status |
|---|---|---|
| `POST` | `/start` | 🆕 New |
| `POST` | `/start/resume` | 🆕 New |
| `POST` | `/start/restart` | 🆕 New |
| `GET`  | `/next-question?attemptId=` | 🆕 Replaces `/questions` |
| `POST` | `/answer` | 🆕 Replaces `/submit-question` |
| `POST` | `/complete` | 🆕 Replaces `/submit-quiz` |
| `GET`  | `/result/{attemptId}` | 🆕 New |
| `GET`  | `/history` | 🆕 New |
| `GET`  | `/tracking` | 🆕 New |

**Removed:**
- `GET /questions` — replaced by `/next-question`
- `POST /submit-question` — replaced by `/answer`
- `POST /submit-quiz` — replaced by `/complete`
- `GET /responses` — no longer needed (DB-backed)

---

### Key Contracts

**POST /start**
```json
// New attempt
{ "status": "NEW", "attemptId": "uuid", "totalQuestions": 52 }

// Existing in-progress found
{ "status": "EXISTING", "attemptId": "uuid", "answeredCount": 23, "totalQuestions": 52, "progressPercent": 44.2 }
```
> `totalQuestions` includes trap questions. IPIP-50 = 52, PHQ-9 = 10.

**GET /next-question**
```json
{
  "complete": false,
  "question": {
    "id": 21,
    "textEn": "...", "textAr": "...",
    "isTrap": false,
    "scaleMin": 1, "scaleMax": 5,
    "labelsEn": ["Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"],
    "labelsAr": ["أعارض بشدة", "أعارض", "محايد", "أوافق", "أوافق بشدة"]
  },
  "progress": { "answered": 20, "total": 52, "percentComplete": 38.5 }
}
```

**POST /answer**
```json
{ "attemptId": "uuid", "questionId": 21, "score": 3 }
```

**POST /complete — Response**
```json
{
  "attemptId": "uuid",
  "quizType": "IPIP50",
  "completedAt": "2025-03-15T14:23:00Z",
  "isValid": true,
  "qualitySummary": {
    "timing":         { "totalSeconds": 312, "failed": false },
    "traps":          { "failed": 0, "warning": false },
    "straightLining": { "longestRun": 3, "flagged": false }
  },
  "scores": [
    { "trait": "Openness",          "rawScore": 38, "normalizedScore": 76.0, "level": "High" },
    { "trait": "Conscientiousness", "rawScore": 29, "normalizedScore": 58.0, "level": "Medium" },
    { "trait": "Extraversion",      "rawScore": 22, "normalizedScore": 44.0, "level": "Medium" },
    { "trait": "Agreeableness",     "rawScore": 41, "normalizedScore": 82.0, "level": "High" },
    { "trait": "Neuroticism",       "rawScore": 18, "normalizedScore": 36.0, "level": "Low" }
  ]
}
```

---

## 5. Question Flow Logic

### Attempt Creation

```java
// On POST /start — order decided once and frozen
QuizDefinition def = questionBankService.getDefinition(quizType);
List<Integer> order;

if (def.isOrdered()) {
    // PHQ-9: fixed order, trap injected at fixed position 5
    order = def.getFixedOrderWithTraps();
} else {
    // IPIP-50/120: shuffle normal questions, inject traps at ~40% and ~80%
    order = def.getShuffledOrderWithTraps();
}

attempt.setQuestionOrder(objectMapper.writeValueAsString(order));
// Order never changes after this — resume uses the same frozen order
```

### Next Question

```java
List<Integer> order   = parseOrder(attempt.getQuestionOrder());
Set<Integer>  answered = answerRepo.findAnsweredIds(attemptId);

return order.stream()
    .filter(id -> !answered.contains(id))
    .findFirst()
    .map(id -> buildResponse(attempt, id, answered.size(), order.size()))
    .orElse(NextQuestionResponse.complete());
```

---

## 6. Resume & Restart Logic

```
POST /start { quizType }
  │
  ├─ IN_PROGRESS attempt exists for this patient + quizType?
  │
  │   YES → return { status: "EXISTING", progress: 44% }
  │         Client shows: "Resume or Restart?"
  │
  │         RESUME  → POST /start/resume { attemptId }
  │                   Server returns existing attemptId. Client calls /next-question.
  │
  │         RESTART → POST /start/restart { attemptId }
  │                   Server: mark old attempt ABANDONED (kept — clinically meaningful)
  │                   Server: create new attempt → return new attemptId
  │
  └─ NO → create new attempt → return { status: "NEW" }
```

Abandoned attempts are **never deleted**. Consistent abandonment at the same question (e.g. PHQ-9 Item 9) is a clinical signal visible to therapists.

---

## 7. Scoring & Interpretation

### PHQ-9
- Single trait: **Depression Severity**
- Trap question excluded from scoring.
- Score = sum of 9 items (0–3). Range: 0–27.

| Score | Level |
|---|---|
| 0–4 | Minimal |
| 5–9 | Mild |
| 10–14 | Moderate |
| 15–19 | Moderately Severe |
| 20–27 | Severe |

**Critical Alert:** PHQ-9 Item 9 score ≥ 1 → `has_critical_alert = true` → triggers `ASSESSMENT_CRITICAL_ALERT` notification.

### IPIP-50 / IPIP-120
- Five traits: Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism.
- Trap questions excluded. Reversed items handled per JSON flag.

```java
int score = traitQuestions.stream()
    .filter(q -> !q.isTrap())
    .mapToInt(q -> q.isReversed()
        ? (scaleMax + 1 - answers.get(q.getId()))
        : answers.get(q.getId()))
    .sum();

float normalized = ((float)(score - minPossible) / (maxPossible - minPossible)) * 100;
```

---

## 8. Tracking & History

### History (direct DB query — no extra logic)

```sql
SELECT trait, raw_score, normalized_score, level,
       has_critical_alert, completed_at, is_valid
FROM assessment_results ar
JOIN assessment_attempts aa ON aa.id = ar.attempt_id
WHERE ar.patient_id = :patientId
  AND ar.quiz_type  = :quizType
ORDER BY ar.completed_at DESC;
```

### Trend (Java — last 5 valid completed attempts)

Simple slope over time series per trait:
- Slope < -0.5 → **IMPROVING**
- Slope > +0.5 → **WORSENING**
- Otherwise → **STABLE**

Invalid attempts (`is_valid = false`) are **excluded** from trend and average calculations but **shown** in history so therapists see the full picture.

### Curriculum Average (Java)

Rolling weighted average, last 5 valid attempts, most recent weighted highest:

```java
// scores = [most recent, ..., oldest], max 5
public double curriculumAverage(List<Double> scores) {
    int n = Math.min(scores.size(), 5);
    double weightedSum = 0, totalWeight = 0;
    for (int i = 0; i < n; i++) {
        double w = n - i;   // 5, 4, 3, 2, 1
        weightedSum += scores.get(i) * w;
        totalWeight += w;
    }
    return totalWeight > 0 ? weightedSum / totalWeight : 0;
}
```

### Tracking API Response

```json
{
  "assessments": {
    "PHQ9": {
      "completedAttempts": 5,
      "abandonedAttempts": 1,
      "lastCompleted": "2025-03-15",
      "traits": {
        "Depression Severity": {
          "curriculumAverage": 16.4,
          "trend": "IMPROVING",
          "deltaPerWeek": -0.8,
          "history": [
            { "date": "2025-03-15", "score": 14, "level": "Moderate",
              "isValid": true,  "qualityFlags": {} },
            { "date": "2025-01-01", "score": 22, "level": "Severe",
              "isValid": false, "invalidReason": "timing" }
          ]
        }
      }
    }
  }
}
```

---

## 9. Notifications Integration

### ✅ Already Exists — Used As-Is

- `notifications` table
- `notification_message_templates` table
- `create_system_notification()` PG function
- `trg_auto_queue_email` trigger (auto-queues email for `email: true` templates)

### 🆕 New — 4 template rows only (SQL above in Section 2)

Called from `AssessmentCompleteService` after saving results:

```java
// Always fire on completion
notificationService.createSystemNotification(
    usersId, "ASSESSMENT_COMPLETE", Map.of("quizType", "PHQ-9"));

// Only fire when Item 9 triggered
if (hasCriticalAlert) {
    notificationService.createSystemNotification(
        userId, "ASSESSMENT_CRITICAL_ALERT", Map.of());
}
```

`notificationService.createSystemNotification()` maps to your existing `create_system_notification()` PG function — no new notification infrastructure.

---

## 10. Spring Boot Layer

### ✅ Already Exists — Reused

| Component | Used For |
|---|---|
| `NotificationService` | Calling `create_system_notification()` |
| `DoctorPatientService` | Checking doctor access before therapist queries |
| `UserService` / security context | Getting `patient_id` from logged-in user |
| JSON resource loading pattern | Extended in `QuestionBankService` |

### 🆕 New Classes

```
// Entities
AssessmentAttempt.java        → assessment_attempts
AssessmentAnswer.java         → assessment_answers
AssessmentResult.java         → assessment_results

// Repositories
AssessmentAttemptRepository.java
AssessmentAnswerRepository.java
AssessmentResultRepository.java

// Services
AssessmentSessionService.java     start / resume / restart / next-question / answer
AssessmentCompleteService.java    complete / score / quality-validate / persist
AssessmentQualityService.java     timing / trap / straight-line checks (3 methods)
TrackingService.java              history / trends / curriculum average
QuestionBankService.java          @PostConstruct loads JSON → in-memory (static data)

// Controllers
AssessmentController.java         /api/assessments/{quizType}/**
TrackingController.java           /api/assessments/history, /tracking
```

### ✅ Deleted

```
QuizService.java           (the ConcurrentHashMap — gone entirely)
```

### Therapist / Doctor Access

Zero new infrastructure. Uses your existing system:

```java
// @PreAuthorize("hasRole('DOCTOR')")
// TherapistAssessmentController
doctorPatientService.verifyAccess(doctorId, patientId);  // your existing check
return assessmentResultRepo.findByPatientId(patientId);  // straightforward query
```

---

## 11. Migration Checklist

### Database
- [ ] `CREATE TABLE assessment_attempts` (with `quality_flags`, `is_valid` columns)
- [ ] `CREATE TABLE assessment_answers`
- [ ] `CREATE TABLE assessment_results`
- [ ] `CREATE TRIGGER` on `assessment_attempts` (reuses `update_updated_at_column`)
- [ ] `INSERT` 4 notification template rows into existing `notification_message_templates`

### Static JSON Files
- [ ] Add trap question to `phq9.json` (1 trap at position ~5)
- [ ] Add 2 trap questions to `ipip50.json` (at positions ~20 and ~40)
- [ ] Add 2 trap questions to `ipip120.json` (at positions ~40 and ~80)
- [ ] Add `isTrap`, `expectedScore`, `reversed`, `trait` fields to all question objects

### Spring Boot
- [ ] Create 3 entities + repositories
- [ ] Create `AssessmentSessionService` (replaces `QuizService`)
- [ ] Create `AssessmentCompleteService`
- [ ] Create `AssessmentQualityService`
- [ ] Create `TrackingService`
- [ ] Create `QuestionBankService` (`@PostConstruct`)
- [ ] Create `AssessmentController` (new endpoints)
- [ ] Create `TrackingController`
- [ ] Delete `QuizService` and old quiz controllers

### Nothing to change in
- `users`, `patient_profiles`, `doctor_profiles`, `doctor_patients` tables
- `engagement_access_rules` table
- `notifications`, `notification_message_templates`, `message_queues` tables
- `create_system_notification()`, `update_updated_at_column()`, `trigger_queue_email_job()` functions
- Any existing notification or engagement logic