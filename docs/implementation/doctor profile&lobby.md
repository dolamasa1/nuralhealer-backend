PHASE 1: File Storage Infrastructure ⏱️ 4 hours
1.1 Environment Configuration
Add to .env:
properties# File Storage
BACKEND_ROOT_URL=http://localhost:8080
FILE_STORAGE_BASE_PATH=backend/storage
PROFILE_PICTURES_PATH=doctors/profiles
MAX_FILE_SIZE_MB=5
ALLOWED_IMAGE_FORMATS=jpg,jpeg,png,webp
PROFILE_IMAGE_MIN_DIMENSION=512
PROFILE_IMAGE_MAX_DIMENSION=2048
1.2 Create Storage Service
File: backend/src/main/java/com/neuralhealer/service/FileStorageService.java
Responsibilities:

Initialize directory structure on app startup
Validate uploaded files (size, format, dimensions, aspect ratio)
Resize/crop images to perfect 1:1 square
Generate unique filenames: {doctorId}_profile_{timestamp}.jpg
Delete old profile pictures when new one uploaded
Return DB-storable path: doctors/profiles/{doctorId}/profile.jpg

Key Methods:
javapublic String saveProfilePicture(MultipartFile file, UUID doctorId)
public void deleteProfilePicture(String relativePath)
public void validateImage(MultipartFile file)
public BufferedImage ensureSquareRatio(BufferedImage image)
public String getPublicUrl(String relativePath)
1.3 Static File Serving Configuration
File: backend/src/main/java/com/neuralhealer/config/StaticResourceConfig.java
Setup:

Map /files/** to FILE_STORAGE_BASE_PATH
Set correct Content-Type headers
Enable caching for performance

Access URL: http://localhost:8080/files/doctors/profiles/{doctorId}/profile.jpg

PHASE 2: DTOs & Mappers ⏱️ 4 hours
2.1 Create DTOs
File: backend/src/main/java/com/neuralhealer/dto/doctor/
A) DoctorProfileFullDTO.java (for GET /doctors/{id}/profile)
java{
  // User info
  "id": UUID,
  "userId": UUID,
  "email": String,
  "firstName": String,
  "lastName": String,
  
  // Professional info
  "title": String,
  "bio": String,
  "specialization": String,  // Psychiatrist | Therapist
  "yearsOfExperience": Integer,
  "certificates": List<Certificate>,
  
  // Location
  "location": {
    "city": String,
    "country": String,
    "latitude": Double,
    "longitude": Double
  },
  
  // Visual
  "profilePictureUrl": String,  // Full URL
  
  // Status
  "verificationStatus": String,  // unverified | pending | verified
  "availabilityStatus": String,  // online | offline | busy
  
  // Metrics
  "rating": Double,
  "totalReviews": Integer,
  "profileCompletion": Integer,
  
  // Contact
  "socialMedia": {
    "linkedin": String,
    "twitter": String,
    "facebook": String,
    "instagram": String,
    "website": String,
    "whatsapp": String,
    "phone": String
  },
  
  // Verification details
  "verificationDetails": {
    "identityVerified": Boolean,
    "licenseVerified": Boolean,
    "platformApproved": Boolean
  },
  
  // Pricing
  "consultationFee": Double
}
B) DoctorLobbyCardDTO.java (for GET /doctors/lobby)
java{
  "id": UUID,
  "fullName": String,  // firstName + lastName
  "title": String,
  "specialization": String,
  "yearsOfExperience": Integer,
  "rating": Double,
  "totalReviews": Integer,
  "profilePictureUrl": String,
  "location": String,  // "Cairo, Egypt"
  "availabilityStatus": String,
  "verificationStatus": String,
  "isVerified": Boolean,  // Quick check (platform_approved)
  "consultationFee": Double,
  "distance": Double  // Only present if geolocation used
}
C) UpdateDoctorProfileRequest.java
java{
  "title": String,
  "bio": String,
  "specialization": String,
  "yearsOfExperience": Integer,
  "certificates": List<Certificate>,
  "socialMedia": SocialMediaDTO,
  "consultationFee": Double,
  "location": {
    "city": String,
    "country": String,
    "latitude": Double,
    "longitude": Double
  }
}
D) DoctorLobbyFilterRequest.java
java{
  "specialization": String,  // null = all
  "verificationStatus": String,  // null = all
  "availabilityStatus": String,  // null = all
  "minRating": Double,  // e.g., 4.0
  "location": String,  // city name
  "sortBy": String,  // rating | reviews | experience | fee
  "sortDirection": String,  // asc | desc
  "page": Integer,
  "size": Integer
}
E) VerificationSubmissionRequest.java
java{
  "answers": Map<String, String>  // {"license_number": "EG123456", ...}
}
2.2 Create Mappers
File: backend/src/main/java/com/neuralhealer/mapper/DoctorMapper.java
Methods:
javaDoctorProfileFullDTO toFullDTO(DoctorProfile profile, User user)
DoctorLobbyCardDTO toLobbyCardDTO(DoctorProfile profile, User user)
List<DoctorLobbyCardDTO> toLobbyCardDTOs(List<DoctorProfile> profiles)

PHASE 3: Service Layer ⏱️ 8 hours
3.1 DoctorProfileService
File: backend/src/main/java/com/neuralhealer/service/DoctorProfileService.java
Methods:
java// Profile retrieval
DoctorProfileFullDTO getDoctorProfile(UUID doctorId)
DoctorProfileFullDTO getMyProfile(UUID userId)

// Profile updates
DoctorProfileFullDTO updateProfile(UUID userId, UpdateDoctorProfileRequest request)
String uploadProfilePicture(UUID userId, MultipartFile file)
void deleteProfilePicture(UUID userId)
void updateAvailabilityStatus(UUID userId, String status)

// Social media
void updateSocialMedia(UUID userId, SocialMediaDTO socialMedia)

// Helper
int calculateProfileCompletion(UUID doctorId)  // Calls DB function
String buildProfilePictureUrl(String relativePath)
3.2 DoctorLobbyService
File: backend/src/main/java/com/neuralhealer/service/DoctorLobbyService.java
Methods:
java// Lobby & search
Page<DoctorLobbyCardDTO> getDoctorLobby(DoctorLobbyFilterRequest filters, Pageable pageable)
Page<DoctorLobbyCardDTO> searchDoctors(String query, Pageable pageable)

// Geolocation (advanced)
List<DoctorLobbyCardDTO> getNearbyDoctors(double lat, double lng, int radiusKm)

// Helper
Specification<DoctorProfile> buildFilterSpecification(DoctorLobbyFilterRequest filters)
Implementation Notes:

Use Spring Data JPA Specifications for dynamic filtering
Default sort: rating DESC, totalReviews DESC
Max page size: 50
Include null-safe handling for filters

3.3 DoctorVerificationService
File: backend/src/main/java/com/neuralhealer/service/DoctorVerificationService.java
Methods:
java// Questions
List<VerificationQuestion> getVerificationQuestions()  // From system_settings

// Submission
void submitVerificationAnswers(UUID userId, Map<String, String> answers)
List<DoctorVerificationQuestion> getMyVerificationAnswers(UUID userId)

// Admin approval (future)
void approveVerification(UUID doctorId, VerificationApprovalDTO approval)

PHASE 4: REST Controllers ⏱️ 6 hours
4.1 DoctorProfileController
File: backend/src/main/java/com/neuralhealer/controller/DoctorProfileController.java
Endpoints:
java// Public - anyone can view
@GetMapping("/api/doctors/{doctorId}/profile")
public DoctorProfileFullDTO getDoctorProfile(@PathVariable UUID doctorId)

// Doctor only - get own profile
@GetMapping("/api/doctors/me/profile")
@PreAuthorize("hasRole('DOCTOR')")
public DoctorProfileFullDTO getMyProfile()

// Doctor only - update own profile
@PutMapping("/api/doctors/me/profile")
@PreAuthorize("hasRole('DOCTOR')")
public DoctorProfileFullDTO updateProfile(@RequestBody UpdateDoctorProfileRequest request)

// Doctor only - upload picture
@PostMapping("/api/doctors/me/profile-picture")
@PreAuthorize("hasRole('DOCTOR')")
public Map<String, String> uploadProfilePicture(@RequestParam("file") MultipartFile file)
// Returns: {"profilePictureUrl": "http://..."}

// Doctor only - delete picture
@DeleteMapping("/api/doctors/me/profile-picture")
@PreAuthorize("hasRole('DOCTOR')")
public ResponseEntity<Void> deleteProfilePicture()

// Doctor only - update availability
@PatchMapping("/api/doctors/me/availability")
@PreAuthorize("hasRole('DOCTOR')")
public void updateAvailability(@RequestParam String status)
// Param: online | offline | busy

// Doctor only - update social media
@PutMapping("/api/doctors/me/social-media")
@PreAuthorize("hasRole('DOCTOR')")
public void updateSocialMedia(@RequestBody SocialMediaDTO socialMedia)
4.2 DoctorLobbyController
File: backend/src/main/java/com/neuralhealer/controller/DoctorLobbyController.java
Endpoints:
java// Public - browse all doctors
@GetMapping("/api/doctors/lobby")
public Page<DoctorLobbyCardDTO> getDoctorLobby(
    @RequestParam(required = false) String specialization,
    @RequestParam(required = false) String verificationStatus,
    @RequestParam(required = false) String availabilityStatus,
    @RequestParam(required = false) Double minRating,
    @RequestParam(required = false) String location,
    @RequestParam(defaultValue = "rating") String sortBy,
    @RequestParam(defaultValue = "desc") String sortDirection,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
)

// Public - search doctors
@GetMapping("/api/doctors/search")
public Page<DoctorLobbyCardDTO> searchDoctors(
    @RequestParam String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
)

// Public - nearby doctors (advanced)
@GetMapping("/api/doctors/nearby")
public List<DoctorLobbyCardDTO> getNearbyDoctors(
    @RequestParam double lat,
    @RequestParam double lng,
    @RequestParam(defaultValue = "10") int radius
)
4.3 DoctorVerificationController
File: backend/src/main/java/com/neuralhealer/controller/DoctorVerificationController.java
Endpoints:
java// Public - get verification questions
@GetMapping("/api/doctors/verification/questions")
public List<VerificationQuestion> getVerificationQuestions()

// Doctor only - submit answers
@PostMapping("/api/doctors/me/verification/submit")
@PreAuthorize("hasRole('DOCTOR')")
public Map<String, String> submitVerification(@RequestBody VerificationSubmissionRequest request)
// Returns: {"status": "pending", "message": "Submitted successfully"}

// Doctor only - get my answers
@GetMapping("/api/doctors/me/verification/answers")
@PreAuthorize("hasRole('DOCTOR')")
public List<DoctorVerificationQuestion> getMyVerificationAnswers()

PHASE 5: Validation & Error Handling ⏱️ 3 hours
5.1 File Upload Validators
File: backend/src/main/java/com/neuralhealer/validator/ImageValidator.java
Validations:

File size ≤ 5MB
MIME type: image/jpeg, image/png, image/webp
Dimensions: Min 512x512, Max 2048x2048
Aspect ratio: 1:1 (square) ±5% tolerance
Not corrupted/malicious

Exceptions:

FileSizeExceededException
InvalidImageFormatException
InvalidAspectRatioException

5.2 Request Validators
Annotations:
java@Valid on request bodies
@NotNull, @NotBlank on required fields
@Size(min, max) on strings
@Pattern(regexp) for social media URLs
@DecimalMin, @DecimalMax for ratings/fees
5.3 Custom Exceptions
javaDoctorNotFoundException
ProfilePictureNotFoundException
InvalidVerificationStatusException

PHASE 6: Enhanced Auth Registration ⏱️ 2 hours
6.1 Update Registration Endpoint
Modify: backend/src/main/java/com/neuralhealer/controller/AuthController.java
New Request Body for Doctors:
javaPOST /api/auth/register
{
  "email": "doctor@test.com",
  "password": "Test1234",
  "firstName": "Dr. Sarah",
  "lastName": "Johnson",
  "role": "DOCTOR",
  
  // New optional fields for fast setup
  "quickSetup": true,
  "doctorDetails": {
    "specialization": "Psychiatrist",
    "title": "MD, Psychiatrist",
    "yearsOfExperience": 10,
    "bio": "Specialized in anxiety treatment",
    "location": {
      "city": "Cairo",
      "country": "Egypt"
    }
  }
}
Logic:

Create user account (existing flow)
Create doctor_profile entry
If quickSetup=true, populate doctor_profiles with provided details
Calculate initial profile_completion_percentage (trigger handles this)
Return success with profile completion info


PHASE 7: Testing & Documentation ⏱️ 4 hours
7.1 Unit Tests
Files to create:

DoctorProfileServiceTest.java
DoctorLobbyServiceTest.java
FileStorageServiceTest.java
ImageValidatorTest.java

Test coverage:

File upload validation (valid/invalid cases)
Profile CRUD operations
Lobby filtering logic
Geolocation distance calculation
Profile completion calculation

7.2 Integration Tests
Scenarios:

Doctor registration → profile creation
Upload profile picture → old picture deleted
Update profile → completion percentage recalculated
Add review → doctor rating updated (trigger test)
Filter lobby → correct doctors returned

7.3 Postman Collection
Create collection with folders:

Doctor Profile Management

Get doctor profile (public)
Get my profile (doctor)
Update profile
Upload picture
Delete picture
Update availability


Doctor Lobby

Get lobby (no filters)
Get lobby (filtered)
Search doctors
Nearby doctors


Verification

Get questions
Submit answers
Get my answers


Testing

Fast doctor signup



Include:

Environment variables ({{BASE_URL}}, {{AUTH_TOKEN}})
Pre-request scripts for auth
Test assertions


PHASE 8: Performance Optimization ⏱️ 2 hours
8.1 Caching Strategy
Use Spring Cache:
java@Cacheable(value = "doctorProfiles", key = "#doctorId")
public DoctorProfileFullDTO getDoctorProfile(UUID doctorId)

@Cacheable(value = "doctorLobby", key = "#filters.hashCode()")
public Page<DoctorLobbyCardDTO> getDoctorLobby(...)

@CacheEvict(value = "doctorProfiles", key = "#userId")
public DoctorProfileFullDTO updateProfile(...)
Configuration:

TTL: 10 minutes for profiles
TTL: 5 minutes for lobby
Invalidate on update

8.2 Database Optimizations
Already done via indexes:

✅ Indexes on filter fields
✅ Composite index on location
✅ Index on rating (DESC)

Query optimizations:

Use @EntityGraph to fetch social_media JSONB efficiently
Lazy load certificates (fetch only when needed)
Pagination required (max 50/page)

8.3 Image Optimization
On upload:

Compress JPEG to 85% quality
Convert PNG to WebP (if supported)
Generate thumbnail (256x256) for lobby cards (optional advanced)


IMPLEMENTATION TIMELINE
PhaseTasksEstimated HoursPhase 1File storage infrastructure4hPhase 2DTOs & mappers4hPhase 3Service layer8hPhase 4REST controllers6hPhase 5Validation & error handling3hPhase 6Enhanced registration2hPhase 7Testing & documentation4hPhase 8Performance optimization2hTotal33 hours (~1 week sprint)

DELIVERABLES CHECKLIST
Backend Code

 FileStorageService with image validation
 DoctorProfileService (CRUD)
 DoctorLobbyService (search, filters, geolocation)
 DoctorVerificationService
 All DTOs and mappers
 3 REST controllers with endpoints
 Request validators
 Custom exceptions with handlers

Configuration

 Static resource mapping for /files/**
 Environment variables in .env
 Caching configuration
 Multipart file upload settings

Testing

 Unit tests (≥80% coverage)
 Integration tests for key flows
 Postman collection with examples

Documentation

 API documentation (Swagger/OpenAPI)
 README update with new endpoints
 Example requests/responses
