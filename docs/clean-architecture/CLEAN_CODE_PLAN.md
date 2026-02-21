# Clean Architecture & Clean Code Plan

> **Scope**: Internal code quality — no endpoint changes, no feature output changes, no new dependencies.

---

## 1. Replace Raw `RuntimeException` with Custom Exceptions

**Problem**: 9 places throw generic `RuntimeException`, which the `GlobalExceptionHandler` catches as a 500 Internal Server Error — even when they represent 400/429 client errors.

| File | Line | Message | Should Be |
|---|---|---|---|
| `OtpService.java` | 54 | "Too many OTP requests..." | `BadRequestException` (or a new `RateLimitException` → 429) |
| `OtpService.java` | 96 | "User not found" | `ResourceNotFoundException` |
| `OtpService.java` | 101 | "Account temporarily locked..." | `ForbiddenException` |
| `OtpService.java` | 125 | "Invalid or expired verification code" | `BadRequestException` |
| `OtpService.java` | 171 | "Failed to send verification email" | `BadRequestException` |
| `FileStorageService.java` | 44 | "Could not initialize storage directory" | Keep as `RuntimeException` (startup failure) |
| `FileStorageService.java` | 75 | "Could not save profile picture" | `BadRequestException` |
| `GmailSmtpService.java` | 46, 51, 54 | SMTP errors | Keep wrapping as `RuntimeException` (infra failure) |

**Action**: Replace the 5 OtpService + 1 FileStorageService throws with the appropriate custom exception from `shared/exception/`.

---

## 2. Extract Duplicated Cookie Logic

**Problem**: Cookie creation is copy-pasted in 3 places with identical settings:
- `AuthService.register()` (line 123-128)
- `AuthService.login()` (line 192-197)
- `AuthController.logout()` (line 112)

**Action**: Create a private helper method in `AuthService`:

```java
private void setAuthCookie(HttpServletResponse response, String token, int maxAge) {
    Cookie cookie = new Cookie(COOKIE_NAME, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(false); // TODO: make configurable via property
    cookie.setPath("/api");
    cookie.setMaxAge(maxAge);
    response.addCookie(cookie);
}
```

This also fixes the stale `// TODO: Set to true in production` comments in both places.

---

## 3. Extract Cookie Name Constant

**Problem**: The string `"neuralhealer_token"` is hardcoded in **5 separate files**:
- `AuthService.java` (2×)
- `AuthController.java` (1×)
- `JwtAuthFilter.java` (1×)
- `WebSocketAuthInterceptor.java` (1×)

**Action**: Add a constant in `shared/security/`:

```java
public final class SecurityConstants {
    public static final String AUTH_COOKIE_NAME = "neuralhealer_token";
    private SecurityConstants() {}
}
```

Replace all 5 occurrences with `SecurityConstants.AUTH_COOKIE_NAME`.

---

## 4. Stop Logging OTP Codes in Plain Text

**Problem**: `OtpService.java` logs the actual OTP code in multiple places — **this is a security risk in production**:
- Line 63: `log.info("Generated OTP code: {} for user: {}", code, ...)`
- Line 74: `log.info("Saving OTP to database - Code: {}, ...")`
- Line 77: `log.info("OTP SAVED TO DATABASE - ID: {}, Code: {}, ...")`
- Line 87: `log.info("=== OTP GENERATION COMPLETE === User: {}, Code: {}", ...)`
- Line 112-114: Logs stored code and match result

**Action**:
- Remove all OTP code values from log statements.
- Keep structural logs (e.g., "OTP generated for user: {}", "OTP saved with ID: {}") but strip the code itself.
- Delete the diagnostic logging block at lines 107-114 (it was debugging-only).

---

## 5. Remove `ex.printStackTrace()` from GlobalExceptionHandler

**Problem**: Line 343 in `GlobalExceptionHandler.java`:
```java
ex.printStackTrace(); // FORCE PRINT STACK TRACE FOR DEBUGGING
```

This is redundant — `log.error()` on the line above already logs the full stack trace (the third arg `ex` passes it). Double-printing clutters logs.

**Action**: Delete line 343.

---

## 6. Fix Cross-Feature Import Violation: `OtpService` → `NotificationRepository`

**Problem**: `OtpService` (in `feature/auth/`) directly imports and uses `NotificationRepository` and `Notification` entity from `feature/notification/`. This violates the cross-feature boundary rule from `FEATURE_BASED_STRUCTURE.md`.

**Action**: Replace the direct repository access with a call to `NotificationCreatorService` (which already exists in `feature/notification/`):

```java
// Before (in OtpService):
Notification notification = Notification.builder()...build();
notificationRepository.save(notification);

// After:
notificationCreatorService.createSystemNotification(
    user.getId(),
    "Verify Your Email Address",
    "Your verification code is: " + code,
    NotificationPriority.high
);
```

This removes the `NotificationRepository` and `Notification.*` imports from `OtpService`.

---

## 7. Deduplicate Social Media Map Conversion

**Problem**: `DoctorProfileServiceImpl` has the social-media-to-map conversion logic written **twice** — once in `updateProfile()` (lines 74-97) and again in `updateSocialMedia()` (lines 165-172). They do the same thing with slightly different null checks.

**Action**: Extract into a private helper:

```java
private Map<String, String> toSocialMediaMap(SocialMediaDTO dto) {
    return Map.of(
        "linkedin",  dto.getLinkedin()  != null ? dto.getLinkedin()  : "",
        "twitter",   dto.getTwitter()   != null ? dto.getTwitter()   : "",
        "facebook",  dto.getFacebook()  != null ? dto.getFacebook()  : "",
        "instagram", dto.getInstagram() != null ? dto.getInstagram() : "",
        "website",   dto.getWebsite()   != null ? dto.getWebsite()   : "",
        "whatsapp",  dto.getWhatsapp()  != null ? dto.getWhatsapp()  : "",
        "phone",     dto.getPhone()     != null ? dto.getPhone()     : ""
    );
}
```

Call `profile.setSocialMedia(toSocialMediaMap(request.getSocialMedia()))` in both methods.

---

## 8. Use `Map.of()` Instead of Manual JSON Strings in EngagementService

**Problem**: `EngagementService` builds JSON payloads via string concatenation in 4 places, e.g.:

```java
"{\"initiatorRole\":\"" + initiator.getRole() + "\", \"initiatedBy\":\"" + initiatedBy + "\"}"
```

This is fragile (breaks if values contain quotes) and hard to read.

**Action**: Use the existing `saveEvent()` helper but pass a `Map`, then serialize:

```java
// In saveEvent, accept Map<String, Object> instead of String:
private void saveEvent(UUID engagementId, String type, UUID userId, Map<String, Object> payload) {
    EngagementEvent event = EngagementEvent.builder()
        .engagementId(engagementId)
        .eventType(type)
        .triggeredBy(userId)
        .payload(new ObjectMapper().writeValueAsString(payload))
        .build();
    eventRepository.save(event);
}
```

Or, if the `payload` column is already JSONB, use the JPA converter directly.

---

## 9. Clean Up Redundant Method Overloads in NotificationService

**Problem**: Several methods have two overloads that exist only to convert `User` → `UUID`:

```java
public void markAsRead(UUID notificationId, User user) {
    markAsRead(notificationId, user.getId());  // single-line delegation
}

public void markAllAsRead(User user) {
    markAllAsRead(user.getId());  // single-line delegation
}
```

**Action**: Keep only the `UUID` versions. Update callers to pass `user.getId()` directly. This is 2 methods to remove and a few caller adjustments.

---

## 10. Inline FQN Imports

**Problem**: Several files use fully-qualified class names inline instead of proper imports:

| File | Example |
|---|---|
| `AuthService.java` | `jakarta.servlet.http.Cookie`, `jakarta.servlet.http.HttpServletResponse`, `org.springframework.util.StringUtils` |
| `DoctorProfileServiceImpl.java` | `java.util.Map`, `java.util.concurrent.ConcurrentHashMap`, `java.time.LocalDateTime`, `java.util.List` |

**Action**: Move these to proper `import` statements at the top of each file.

---

## Summary

| # | Issue | Files Affected | Risk | Effort |
|---|---|---|---|---|
| 1 | Raw `RuntimeException` | 3 files, 9 throws | Low | Small |
| 2 | Duplicated cookie logic | 2 files, 3 places | Low | Small |
| 3 | Hardcoded cookie name | 5 files | Low | Small |
| 4 | OTP codes in logs | 1 file, 5+ lines | **Medium** (security) | Small |
| 5 | `ex.printStackTrace()` | 1 file, 1 line | Low | Tiny |
| 6 | Cross-feature import | 1 file | Low | Small |
| 7 | Duplicated social media map | 1 file, 2 methods | Low | Small |
| 8 | Manual JSON strings | 1 file, 4 places | Low | Small |
| 9 | Redundant overloads | 1 file, 2 methods | Low | Small |
| 10 | Inline FQN imports | 2 files | Low | Tiny |

**Total**: ~10 focused changes. No new packages, no new abstractions, no endpoint changes.
