# NeuralHealer — Feature-Based Structure Plan

> **Purpose**: Blueprint for reorganizing the project from layer-based (`controller/`, `service/`, `repository/`) to feature-based packages. No code is changed — this is a reference doc.

---

## Current Structure (Layer-Based)

```
com.neuralhealer.backend
├── config/           (11 files)
├── controller/       (11 files)
├── exception/        (13 files)
├── handler/          (2 files)
├── mapper/           (1 file)
├── model/
│   ├── dto/          (32 files)
│   ├── entity/       (16 files)
│   └── enums/        (9 files)
├── repository/       (15 + 1 spec)
├── security/         (5 files)
├── service/          (12 + 3 impl)
├── util/             (1 file)
├── validator/        (1 file)
├── activities/quizzes/  (12 files)
├── integration/gmail/   (5 files)
├── notification/        (13 files)
└── diagnostic/          (1 file)
```

---

## Identified Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **auth** | Registration, login, JWT, OTP email verification |
| 2 | **ai** | AI chatbot (REST + STOMP), chat history & storage |
| 3 | **doctor** | Doctor profile, lobby, verification questions |
| 4 | **patient** | Patient profile (thin now — kept separate for future growth: medical history, settings, etc.) |
| 5 | **engagement** | Doctor-patient engagements, messaging, access rules |
| 6 | **notification** | In-app notifications, SSE push, cleanup jobs |
| 7 | **email** | Email queue, Gmail SMTP integration, templates |
| 8 | **quiz** | IPIP-50, IPIP-120, PHQ-9 quizzes |
| — | **shared** | Config, security, WebSocket infra, exceptions, utils (cross-cutting) |

---

## File → Feature Mapping

### 1. `auth`
| Current Path | Type |
|---|---|
| `controller/AuthController.java` | Controller |
| `service/AuthService.java` | Service |
| `service/OtpService.java` | Service |
| `repository/UserRepository.java` | Repository |
| `repository/EmailVerificationOtpRepository.java` | Repository |
| `model/entity/EmailVerificationOtp.java` | Entity |
| `model/dto/LoginRequest.java` | DTO |
| `model/dto/RegisterRequest.java` | DTO |
| `model/dto/AuthResponse.java` | DTO |
| `model/dto/TokenResponse.java` | DTO |
| `model/dto/VerifyEmailRequest.java` | DTO |
| `model/dto/ResendOtpRequest.java` | DTO |
| `model/enums/UserRole.java` | Enum |

### 2. `ai`
| Current Path | Type |
|---|---|
| `controller/AiChatbotController.java` | Controller |
| `controller/AiStompController.java` | Controller |
| `controller/ChatHistoryController.java` | Controller |
| `service/AiChatbotService.java` | Service |
| `service/ChatStorageService.java` | Service |
| `repository/AiChatMessageRepository.java` | Repository |
| `repository/AiChatSessionRepository.java` | Repository |
| `model/entity/AiChatMessage.java` | Entity |
| `model/entity/AiChatSession.java` | Entity |
| `model/dto/AiChatRequest.java` | DTO |
| `model/dto/AiChatResponse.java` | DTO |
| `model/dto/AiHealthResponse.java` | DTO |
| `model/dto/AiSessionChatResponse.java` | DTO |
| `model/enums/ChatSenderType.java` | Enum |
| `config/AiChatbotConfig.java` | Config |
| `diagnostic/AiHealthEndpoint.java` | Health |

### 3. `doctor`
| Current Path | Type |
|---|---|
| `controller/DoctorController.java` | Controller |
| `controller/DoctorProfileController.java` | Controller |
| `controller/DoctorLobbyController.java` | Controller |
| `controller/DoctorVerificationController.java` | Controller |
| `service/impl/DoctorProfileServiceImpl.java` | Service |
| `service/impl/DoctorLobbyServiceImpl.java` | Service |
| `service/impl/DoctorVerificationServiceImpl.java` | Service |
| `service/DoctorProfileService.java` | Interface |
| `service/DoctorLobbyService.java` | Interface |
| `service/DoctorVerificationService.java` | Interface |
| `service/VerificationService.java` | Service |
| `repository/DoctorProfileRepository.java` | Repository |
| `repository/DoctorPatientRepository.java` | Repository |
| `repository/DoctorVerificationQuestionRepository.java` | Repository |
| `model/entity/DoctorProfile.java` | Entity |
| `model/entity/DoctorPatient.java` | Entity |
| `model/entity/DoctorPatientId.java` | Entity |
| `model/entity/DoctorVerificationQuestion.java` | Entity |
| `model/dto/DoctorResponse.java` | DTO |
| `model/dto/DoctorProfileFullDTO.java` | DTO |
| `model/dto/DoctorLobbyCardDTO.java` | DTO |
| `model/dto/DoctorLobbyFilterRequest.java` | DTO |
| `model/dto/UpdateDoctorProfileRequest.java` | DTO |
| `model/dto/AuthorizedDoctorResponse.java` | DTO |
| `model/dto/SocialMediaDTO.java` | DTO |
| `model/dto/VerificationSubmissionRequest.java` | DTO |
| `model/enums/VerificationType.java` | Enum |
| `repository/specification/` | Specification |

### 4. `patient`
| Current Path | Type |
|---|---|
| `controller/UserController.java` | Controller |
| `repository/PatientProfileRepository.java` | Repository |
| `model/entity/PatientProfile.java` | Entity |
| `model/dto/SessionWithDoctorsResponse.java` | DTO |

### 5. `engagement`
| Current Path | Type |
|---|---|
| `controller/EngagementController.java` | Controller |
| `service/EngagementService.java` | Service |
| `service/EngagementMessageService.java` | Service |
| `repository/EngagementRepository.java` | Repository |
| `repository/EngagementEventRepository.java` | Repository |
| `repository/EngagementMessageRepository.java` | Repository |
| `repository/EngagementAccessRuleRepository.java` | Repository |
| `repository/EngagementVerificationTokenRepository.java` | Repository |
| `model/entity/Engagement.java` | Entity |
| `model/entity/EngagementEvent.java` | Entity |
| `model/entity/EngagementMessage.java` | Entity |
| `model/entity/EngagementAccessRule.java` | Entity |
| `model/entity/EngagementVerificationToken.java` | Entity |
| `model/dto/StartEngagementRequest.java` | DTO |
| `model/dto/StartEngagementResponse.java` | DTO |
| `model/dto/EngagementResponse.java` | DTO |
| `model/dto/EndEngagementRequest.java` | DTO |
| `model/dto/CancelEngagementRequest.java` | DTO |
| `model/dto/VerifyEngagementRequest.java` | DTO |
| `model/dto/SendMessageRequest.java` | DTO |
| `model/dto/MessageResponse.java` | DTO |
| `model/enums/EngagementStatus.java` | Enum |
| `model/enums/EngagementAction.java` | Enum |
| `model/enums/RelationshipStatus.java` | Enum |
| `model/enums/CancellationRole.java` | Enum |
| `model/enums/TokenStatus.java` | Enum |

### 6. `notification`
Already feature-based ✅
| Current Path | Type |
|---|---|
| `notification/controller/NotificationRestController.java` | Controller |
| `notification/service/NotificationService.java` | Service |
| `notification/service/NotificationCreatorService.java` | Service |
| `notification/service/SseEmitterRegistry.java` | Service |
| `notification/service/NotificationCleanupJob.java` | Job |
| `notification/service/UserActivityNotificationJob.java` | Job |
| `notification/service/SystemAlertService.java` | Service |
| `notification/repository/NotificationRepository.java` | Repository |
| `notification/entity/Notification.java` | Entity |
| `notification/entity/NotificationPriority.java` | Enum |
| `notification/entity/NotificationSource.java` | Enum |
| `notification/entity/NotificationType.java` | Enum |
| `notification/model/LocalizedMessage.java` | Model |
| `model/dto/NotificationResponse.java` | DTO |
| `model/dto/NotificationCountResponse.java` | DTO |

### 7. `email`
| Current Path | Type |
|---|---|
| `integration/gmail/*` (5 files) | Gmail SMTP |
| `repository/MessageQueueRepository.java` | Repository |
| `model/entity/MessageQueue.java` | Entity |
| `model/entity/SystemSetting.java` | Entity |
| `repository/SystemSettingRepository.java` | Repository |

### 8. `quiz`
Already feature-based ✅
| Current Path | Type |
|---|---|
| `activities/quizzes/*` (12 files) | Full feature |

### Shared (cross-cutting + WebSocket infrastructure)

WebSocket is infrastructure, not a domain feature — it has no business logic of its own. STOMP controllers are co-located with the features they serve (e.g. `AiStompController` → `ai/`).

| Current Path | Type | Destination |
|---|---|---|
| `model/entity/User.java` | Entity | `shared/entity/` (referenced by half the codebase) |
| `model/enums/UserRole.java` | Enum | `shared/entity/` |
| `config/SecurityConfig.java` | Config | `shared/config/` |
| `config/JpaConfig.java` | Config | `shared/config/` |
| `config/AsyncConfig.java` | Config | `shared/config/` |
| `config/OpenApiConfig.java` | Config | `shared/config/` |
| `config/StaticResourceConfig.java` | Config | `shared/config/` |
| `config/FileStorageProperties.java` | Config | `shared/config/` |
| `config/DatabaseConnectionLogger.java` | Config | `shared/config/` |
| `config/NotificationChannelsMigration.java` | Migration | `shared/config/` |
| `config/WebSocketConfig.java` | Config | `shared/websocket/` |
| `config/RawWebSocketConfig.java` | Config | `shared/websocket/` |
| `controller/WebSocketController.java` | Controller | `shared/websocket/` |
| `service/WebSocketService.java` | Service | `shared/websocket/` |
| `handler/` (2 files) | Handlers | `shared/websocket/` |
| `model/dto/WebSocketMessage.java` | DTO | `shared/websocket/` |
| `model/dto/TypingIndicator.java` | DTO | `shared/websocket/` |
| `model/enums/WebSocketMessageType.java` | Enum | `shared/websocket/` |
| `security/WebSocketAuthInterceptor.java` | Security | `shared/websocket/` |
| `security/WebSocketEventListener.java` | Listener | `shared/websocket/` |
| `security/JwtAuthFilter.java` | Security | `shared/security/` |
| `security/JwtService.java` | Security | `shared/security/` |
| `security/UserDetailsServiceImpl.java` | Security | `shared/security/` |
| `service/FileStorageService.java` | Service | `shared/service/` |
| `exception/*` (13 files) | Exceptions | `shared/exception/` |
| `mapper/` (1 file) | Mapper | `shared/mapper/` |
| `util/` (1 file) | Utility | `shared/util/` |
| `validator/` (1 file) | Validator | `shared/validator/` |
| `model/dto/ErrorResponse.java` | DTO | `shared/dto/` |

---

## Proposed Feature-Based Structure

```
com.neuralhealer.backend
│
├── feature/
│   ├── auth/
│   │   ├── controller/     AuthController
│   │   ├── service/        AuthService, OtpService
│   │   ├── repository/     UserRepository, EmailVerificationOtpRepository
│   │   ├── entity/         EmailVerificationOtp
│   │   └── dto/            LoginRequest, RegisterRequest, AuthResponse, TokenResponse, VerifyEmailRequest, ResendOtpRequest
│   │
│   ├── ai/
│   │   ├── controller/     AiChatbotController, AiStompController, ChatHistoryController
│   │   ├── service/        AiChatbotService, ChatStorageService
│   │   ├── repository/     AiChatMessageRepository, AiChatSessionRepository
│   │   ├── entity/         AiChatMessage, AiChatSession
│   │   ├── dto/            AiChatRequest, AiChatResponse, AiHealthResponse, AiSessionChatResponse
│   │   ├── config/         AiChatbotConfig
│   │   └── diagnostic/     AiHealthEndpoint
│   │
│   ├── doctor/
│   │   ├── controller/     DoctorController, DoctorProfileController, DoctorLobbyController, DoctorVerificationController
│   │   ├── service/        DoctorProfileService(Impl), DoctorLobbyService(Impl), DoctorVerificationService(Impl), VerificationService
│   │   ├── repository/     DoctorProfileRepository, DoctorPatientRepository, DoctorVerificationQuestionRepository, specification/
│   │   ├── entity/         DoctorProfile, DoctorPatient, DoctorPatientId, DoctorVerificationQuestion
│   │   ├── dto/            DoctorResponse, DoctorProfileFullDTO, DoctorLobbyCardDTO, etc.
│   │   └── enums/          VerificationType
│   │
│   ├── patient/
│   │   ├── controller/     UserController
│   │   ├── repository/     PatientProfileRepository
│   │   ├── entity/         PatientProfile
│   │   └── dto/            SessionWithDoctorsResponse
│   │
│   ├── engagement/
│   │   ├── controller/     EngagementController
│   │   ├── service/        EngagementService, EngagementMessageService
│   │   ├── repository/     EngagementRepository + 4 others
│   │   ├── entity/         Engagement + 4 others
│   │   ├── dto/            StartEngagementRequest, EngagementResponse, SendMessageRequest, etc.
│   │   └── enums/          EngagementStatus, EngagementAction, RelationshipStatus, CancellationRole, TokenStatus
│   │
│   ├── notification/       ← already feature-based, move as-is
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── model/
│   │
│   ├── email/
│   │   ├── gmail/          ← move from integration/gmail/
│   │   ├── repository/     MessageQueueRepository
│   │   └── entity/         MessageQueue, SystemSetting
│   │
│   └── quiz/               ← move from activities/quizzes/
│       ├── common/
│       ├── ipip50/
│       ├── ipip120/
│       └── phq9/
│
└── shared/
    ├── entity/             User, UserRole (referenced by half the codebase)
    ├── config/             SecurityConfig, JpaConfig, AsyncConfig, OpenApiConfig, etc.
    ├── security/           JwtAuthFilter, JwtService, UserDetailsServiceImpl
    ├── websocket/          WebSocketConfig, RawWebSocketConfig, WebSocketController,
    │                       WebSocketService, handlers, WebSocketAuthInterceptor,
    │                       WebSocketEventListener, WebSocketMessage, TypingIndicator,
    │                       WebSocketMessageType
    ├── exception/          GlobalExceptionHandler + all custom exceptions
    ├── service/            FileStorageService
    ├── dto/                ErrorResponse
    ├── mapper/
    ├── util/
    └── validator/
```

---

## Migration Order (recommended)

| Step | Feature | Risk | Notes |
|------|---------|------|-------|
| 1 | `shared/` | Low | Move cross-cutting + WebSocket infra + `User` entity first, update imports |
| 2 | `notification` | None | Already feature-based, just relocate package |
| 3 | `quiz` | None | Already feature-based, just relocate package |
| 4 | `email` | Low | Small surface area |
| 5 | `patient` | Low | Small surface area |
| 6 | `auth` | Medium | Depends on `User` already being in `shared/entity/` |
| 7 | `ai` | Low | Self-contained, co-locate `AiStompController` here |
| 8 | `doctor` | Medium | Many files, multiple sub-features |
| 9 | `engagement` | High | Largest feature, most cross-references |

> [!TIP]
> Use your IDE's **Move Class** refactoring (Shift+F6 / F6 in IntelliJ) to automatically update imports when moving files.

---

## Rules to Follow

1. **Each feature owns its own** controller, service, repository, entity, DTO, and enum sub-packages.
2. **No feature imports another feature's internal classes directly** — if `engagement` needs doctor data, define a minimal DTO or interface in `shared/` and have `doctor` implement it. This is the rule that makes or breaks the architecture.
3. **`shared/`** is for genuinely cross-cutting things only (security, config, WebSocket infra, exceptions, utils).
4. **`User` entity lives in `shared/entity/`** — auth, doctor, patient, engagement, and notification all reference it.
5. **STOMP controllers live with their owning feature** — e.g. `AiStompController` stays in `ai/controller/`, not in `shared/websocket/`. WebSocket infra in `shared/` is config and transport only.
6. **`patient` is intentionally thin** — kept separate to accommodate future growth (medical history, settings, etc.). If it stays at 4 files after several expansions, merge into `engagement` or a broader `user-profile` package.
