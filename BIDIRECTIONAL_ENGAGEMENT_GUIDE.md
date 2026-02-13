# Bidirectional Engagement Flow - Quick Reference

## ✅ Compilation Error Fixed

**Problem**: `BadRequestException cannot be resolved to a type`  
**Solution**: Created `BadRequestException.java` and added handler to `GlobalExceptionHandler.java`

## 📋 How It Works

### Scenario 1: Doctor Initiates Engagement

1. **Doctor** calls `/api/engagements/initiate` with:
   ```json
   {
     "patientId": "uuid-of-patient",
     "accessRuleName": "FULL_ACCESS"
   }
   ```

2. **System** creates engagement with `initiated_by = "doctor"`

3. **Patient** receives email with 2FA token

4. **Patient** calls `/api/engagements/verify-start` with token to activate

### Scenario 2: Patient Initiates Engagement

1. **Patient** calls `/api/engagements/initiate` with:
   ```json
   {
     "doctorId": "uuid-of-doctor",
     "message": "I would like to engage with you regarding my treatment plan.",
     "accessRuleName": "FULL_ACCESS"
   }
   ```

2. **System** creates engagement with `initiated_by = "patient"`

3. **Doctor** receives email notification about patient request

4. **Doctor** calls `/api/engagements/verify-start` with token to accept

## 🔒 Security Rules

| Action | Initiator | Verifier | Token Access |
|--------|-----------|----------|--------------|
| Initiate | Doctor or Patient | - | Initiator only |
| Verify | - | Counter-party | Counter-party only |
| Get Token | Initiator only | - | Initiator only |
| Refresh Token | Initiator only | - | Initiator only |

**Key Point**: The person who initiates CANNOT verify. Only the counter-party can verify.

## 📧 Email Notifications

- **Doctor → Patient**: Standard engagement token email
- **Patient → Doctor**: Special "engagement request" email with patient's message

## 🧪 Testing with Postman

The Postman collection has been updated with:
- Bidirectional request body examples
- Updated descriptions for initiator-only endpoints
- Generic "Verify Start" endpoint (works for both directions)

## ✨ What Changed

1. ✅ `BadRequestException.java` - New exception class
2. ✅ `GlobalExceptionHandler.java` - Added HTTP 400 handler
3. ✅ `EngagementService.java` - Already has bidirectional logic
4. ✅ `Postman.json` - Updated with bidirectional examples
5. ✅ Email templates - Both doctor and patient flows supported
