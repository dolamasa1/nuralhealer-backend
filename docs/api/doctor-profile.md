# Doctor Profile & Lobby API

Endpoints for managing doctor profiles, browsing the public lobby, and handling verification.

## 👤 Profile Management
*Authenticated (ROLE_DOCTOR)*

### Get My Profile
- **GET** `/api/doctors/me/profile`
- **Response**: `DoctorProfileFullDTO`

### Update Profile
- **PUT** `/api/doctors/me/profile`
- **Body**: `UpdateDoctorProfileRequest`
- **Fields**: `title`, `bio`, `specialization`, `yearsOfExperience`, `location`, `consultationFee`

### Upload Profile Picture
- **POST** `/api/doctors/me/profile-picture`
- **Body**: `MultipartFile` (param: `file`)
- **Note**: Replaces old picture; generates 256x256 thumbnail. Enforces 1-minute cooldown.

### Update Availability
- **PATCH** `/api/doctors/me/availability`
- **Query**: `status` (online | offline | busy)

---

## 🏛️ Public Lobby
*Public Access*

### Get Doctor Lobby
- **GET** `/api/doctors/lobby`
- **Query Params**: `specialization`, `minRating`, `location`, `sortBy`, `page`, `size`
- **Response**: `Page<DoctorLobbyCardDTO>`

### Search Doctors
- **GET** `/api/doctors/search`
- **Query Params**: `q` (search string), `page`, `size`
- **Search fields**: Name, Title, Bio, Specialization.

### Get Nearby Doctors
- **GET** `/api/doctors/nearby`
- **Query Params**: `lat`, `lng`, `radius` (km)

---

## 🛡️ Verification
*Authenticated (ROLE_DOCTOR / Public for questions)*

### Get Verification Questions
- **GET** `/api/doctors/verification/questions`
- **Response**: List of objects `[{key, label, type, required}]`

### Submit Verification
- **POST** `/api/doctors/me/submit`
- **Body**: `VerificationSubmissionRequest` (Map of answers)
- **Status**: Sets profile to `pending` review.

---

## 🔑 Registration (Doctor)
- **POST** `/api/auth/register`
- **Additional Params**: 
  - `quickSetup`: `true`
  - `title`: String
  - `specialization`: "Psychiatrist" | "Therapist"
