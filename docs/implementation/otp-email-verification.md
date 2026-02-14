# OTP Email Verification System - Implementation Plan

## Overview

Implement a secure OTP (One-Time Password) email verification system that requires users to verify their email address before they can access the platform. This prevents unverified accounts from using the system and ensures email validity.

## User Review Required

> [!IMPORTANT]
> **Breaking Change**: After this implementation, newly registered users will NOT be able to log in until they verify their email address. Existing users in the database will need to be handled - we can either:
> 1. Mark all existing users as verified automatically
> 2. Force all existing users to verify their email on next login
> 
> **Please confirm which approach you prefer.**

> [!WARNING]
> **Security Consideration**: The OTP system will implement rate limiting and account lockout. Default settings:
> - OTP valid for 15 minutes
> - Maximum 5 verification attempts per OTP
> - Maximum 3 OTP resend requests per hour
> - Account locked for 30 minutes after 5 failed attempts
> 
> **Please confirm if these limits are acceptable or suggest alternatives.**

---

## Proposed Changes

### Database Layer

#### [MODIFY] [DB.sql](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/resources/DB.sql)

**Changes to `users` table**:
- Add `email_verification_required` BOOLEAN field (default: true)
- Add `email_verification_sent_at` TIMESTAMP field
- Add `failed_verification_attempts` INTEGER field (default: 0)
- Add `verification_locked_until` TIMESTAMP field

**New table: `email_verification_otps`**:
```sql
CREATE TABLE email_verification_otps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  otp_code VARCHAR(6) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  expires_at TIMESTAMP NOT NULL,
  verified_at TIMESTAMP,
  attempts INTEGER DEFAULT 0,
  is_used BOOLEAN DEFAULT false,
  ip_address INET,
  user_agent TEXT,
  
  CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_otp_user_id ON email_verification_otps(user_id);
CREATE INDEX idx_otp_expires_at ON email_verification_otps(expires_at);
CREATE INDEX idx_otp_code ON email_verification_otps(otp_code);
```

**Cleanup job for expired OTPs**:
```sql
-- Delete OTPs older than 24 hours
DELETE FROM email_verification_otps 
WHERE created_at < NOW() - INTERVAL '24 hours';
```

---

### Backend - Entity Layer

#### [NEW] [EmailVerificationOtp.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/model/entity/EmailVerificationOtp.java)

JPA entity mapping to `email_verification_otps` table with:
- UUID primary key
- ManyToOne relationship to User
- OTP code (6 digits)
- Expiration tracking
- Attempt counting
- IP and user agent tracking for security

---

### Backend - Repository Layer

#### [NEW] [EmailVerificationOtpRepository.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/repository/EmailVerificationOtpRepository.java)

Repository with custom queries:
- `findByUserIdAndIsUsedFalseAndExpiresAtAfter()` - Get valid OTPs for user
- `findByOtpCodeAndIsUsedFalse()` - Find OTP by code
- `countByUserIdAndCreatedAtAfter()` - Rate limiting check
- `deleteByExpiresAtBefore()` - Cleanup expired OTPs

---

### Backend - Service Layer

#### [NEW] [OtpService.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/service/OtpService.java)

Core OTP management service with methods:

**OTP Generation**:
- `generateOtp(User user, String ipAddress, String userAgent)` - Generate 6-digit OTP
- Uses `SecureRandom` for cryptographic randomness
- Invalidates previous unused OTPs for the user
- Sets 15-minute expiration

**OTP Validation**:
- `verifyOtp(String email, String otpCode)` - Validate OTP and mark email as verified
- Checks expiration, usage status, and attempt count
- Updates `users.email_verified_at` on success
- Increments attempt counter on failure
- Locks account after 5 failed attempts

**Rate Limiting**:
- `canRequestNewOtp(User user)` - Check if user can request new OTP
- Enforces 3 requests per hour limit
- Checks account lock status

**OTP Resend**:
- `resendOtp(User user, String ipAddress, String userAgent)` - Resend OTP
- Validates rate limits
- Generates new OTP and sends email

---

#### [MODIFY] [AuthService.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/service/AuthService.java)

**Changes to `register()` method**:
1. After creating user, generate OTP via `otpService.generateOtp()`
2. Send OTP email via `directEmailService.sendOtpEmail()`
3. Set `email_verification_required = true`
4. Do NOT set `email_verified_at` (remains null)
5. Return response indicating verification email sent

**Changes to `login()` method**:
1. After authentication, check if `email_verified_at` is null
2. If unverified, throw `EmailNotVerifiedException` with message
3. Return 403 status with instructions to verify email
4. Allow login only if `email_verified_at` is not null

---

### Backend - Controller Layer

#### [MODIFY] [AuthController.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/controller/AuthController.java)

**New endpoints**:

```java
@PostMapping("/verify-email")
public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request)
// Validates OTP and marks email as verified

@PostMapping("/resend-otp")
public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request)
// Resends OTP to user's email

@GetMapping("/verification-status/{email}")
public ResponseEntity<?> checkVerificationStatus(@PathVariable String email)
// Check if email is verified (for frontend polling)
```

**Exception handling**:
- Catch `EmailNotVerifiedException` and return 403 with clear message
- Catch `OtpExpiredException` and return 400 with resend option
- Catch `AccountLockedException` and return 429 with lockout time

---

### Backend - DTO Layer

#### [NEW] [VerifyEmailRequest.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/model/dto/VerifyEmailRequest.java)

```java
public record VerifyEmailRequest(
    @NotBlank String email,
    @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otpCode
) {}
```

#### [NEW] [ResendOtpRequest.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/model/dto/ResendOtpRequest.java)

```java
public record ResendOtpRequest(
    @NotBlank @Email String email
) {}
```

---

### Email Template

#### [MODIFY] [OTP.html](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/resources/templates/emails/OTP.html)

Create a professional, modern HTML email template with:
- NeuralHealer branding
- Large, centered 6-digit OTP code
- Clear instructions
- Expiration time (15 minutes)
- Security warning (don't share code)
- Resend link/instructions
- Responsive design for mobile

**Placeholders**:
- `{USER_NAME}` - User's first name
- `{OTP_CODE}` - 6-digit verification code
- `{EXPIRY_MINUTES}` - Expiration time in minutes
- `{SUPPORT_EMAIL}` - Support contact

---

#### [MODIFY] [DirectEmailService.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/integration/gmail/DirectEmailService.java)

**New method**:
```java
public void sendOtpEmail(String email, String userName, String otpCode, int expiryMinutes)
```

Renders OTP.html template with placeholders and sends via Gmail SMTP.

**Fallback template** (if OTP.html missing):
- Simple, clean HTML with OTP code
- Basic styling for readability
- All essential information

---

### Security Layer

#### [NEW] [EmailVerificationFilter.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/security/EmailVerificationFilter.java)

Optional: Servlet filter to block unverified users from accessing protected endpoints.

**Logic**:
1. Check if user is authenticated
2. Check if email is verified
3. If not verified, return 403 with verification required message
4. Whitelist verification endpoints (`/auth/verify-email`, `/auth/resend-otp`)

**Alternative approach**: Handle verification check in `AuthService.login()` only (simpler, recommended).

---

### Configuration

#### [MODIFY] [SecurityConfig.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/security/SecurityConfig.java)

**Permit new endpoints**:
```java
.requestMatchers("/api/auth/verify-email").permitAll()
.requestMatchers("/api/auth/resend-otp").permitAll()
.requestMatchers("/api/auth/verification-status/**").permitAll()
```

---

### Scheduled Jobs

#### [NEW] [OtpCleanupJob.java](file:///f:/documents/Nuralhealer-main/Nuralhealer/backend/backend/src/main/java/com/neuralhealer/backend/scheduled/OtpCleanupJob.java)

Scheduled task to clean up expired OTPs:
- Runs every hour
- Deletes OTPs older than 24 hours
- Logs cleanup statistics

```java
@Scheduled(cron = "0 0 * * * *") // Every hour
public void cleanupExpiredOtps()
```

---

## Verification Plan

### Automated Tests

#### Unit Tests

**[NEW] OtpServiceTest.java**
```bash
# Run specific test class
./mvnw test -Dtest=OtpServiceTest

# Tests to implement:
- testGenerateOtp_Success()
- testGenerateOtp_InvalidatesPreviousOtp()
- testVerifyOtp_Success()
- testVerifyOtp_ExpiredOtp()
- testVerifyOtp_InvalidCode()
- testVerifyOtp_MaxAttemptsExceeded()
- testResendOtp_RateLimitExceeded()
- testCanRequestNewOtp_AccountLocked()
```

**[NEW] AuthServiceOtpIntegrationTest.java**
```bash
# Run integration tests
./mvnw test -Dtest=AuthServiceOtpIntegrationTest

# Tests to implement:
- testRegister_SendsOtpEmail()
- testLogin_UnverifiedEmail_ThrowsException()
- testLogin_VerifiedEmail_Success()
```

### Manual Testing

#### Test Case 1: Complete Registration and Verification Flow

1. **Register new user**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "Test123!",
       "firstName": "Test",
       "lastName": "User",
       "role": "PATIENT"
     }'
   ```
   **Expected**: Response indicates verification email sent

2. **Check email inbox**: Verify OTP email received with 6-digit code

3. **Verify email with OTP**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/verify-email \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "otpCode": "123456"
     }'
   ```
   **Expected**: Success response, email marked as verified

4. **Login with verified account**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "Test123!"
     }'
   ```
   **Expected**: Login successful, JWT token returned

#### Test Case 2: Login Before Verification

1. Register new user (as above)
2. **Attempt login without verification**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "Test123!"
     }'
   ```
   **Expected**: 403 error with message "Please verify your email before logging in"

#### Test Case 3: OTP Resend

1. Register new user
2. **Request OTP resend**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/resend-otp \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com"
     }'
   ```
   **Expected**: New OTP email sent, old OTP invalidated

#### Test Case 4: Rate Limiting

1. Register new user
2. **Request OTP resend 4 times rapidly**
3. **Expected**: 4th request should fail with "Too many requests" error

#### Test Case 5: Invalid OTP

1. Register new user
2. **Submit wrong OTP 5 times**:
   ```bash
   curl -X POST http://localhost:8080/api/auth/verify-email \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "otpCode": "000000"
     }'
   ```
3. **Expected**: After 5 attempts, account locked for 30 minutes

#### Test Case 6: Expired OTP

1. Register new user
2. **Wait 16 minutes** (OTP expires after 15 minutes)
3. **Attempt verification**
4. **Expected**: Error "OTP has expired, please request a new one"

### Database Verification

**Check OTP record created**:
```sql
SELECT * FROM email_verification_otps 
WHERE user_id = (SELECT id FROM users WHERE email = 'test@example.com')
ORDER BY created_at DESC;
```

**Check email verified**:
```sql
SELECT email, email_verified_at, email_verification_required 
FROM users 
WHERE email = 'test@example.com';
```

### Email Template Testing

**Test OTP email rendering**:
```bash
curl -X POST http://localhost:8080/api/test/email/otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "userName": "Test User",
    "otpCode": "123456"
  }'
```

**Expected**: Email received with:
- Proper formatting
- OTP code clearly visible
- Expiration time shown
- NeuralHealer branding
- Mobile-responsive design

---

## Migration Strategy for Existing Users

**Option 1: Auto-verify existing users** (Recommended):
```sql
UPDATE users 
SET email_verified_at = created_at,
    email_verification_required = false
WHERE email_verified_at IS NULL 
  AND created_at < NOW();
```

**Option 2: Force verification on next login**:
- Keep existing users unverified
- On login, send OTP and require verification
- More secure but may frustrate existing users

---

## Rollback Plan

If issues arise, rollback steps:

1. **Disable email verification requirement**:
   ```sql
   UPDATE users SET email_verification_required = false;
   ```

2. **Revert AuthService changes**: Remove email verification check from login

3. **Drop new table**:
   ```sql
   DROP TABLE email_verification_otps;
   ```

4. **Revert database schema changes** to `users` table

---

## Future Enhancements

1. **SMS OTP**: Add phone verification as alternative
2. **Magic Link**: Email link-based verification (no code entry)
3. **2FA Integration**: Use OTP system for two-factor authentication
4. **Analytics**: Track verification rates and common issues
5. **Customizable Templates**: Multi-language OTP emails
6. **Backup Codes**: Generate backup codes for account recovery
