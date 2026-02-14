package com.neuralhealer.backend.service;

import com.neuralhealer.backend.model.dto.AuthResponse;
import com.neuralhealer.backend.model.dto.LoginRequest;
import com.neuralhealer.backend.model.dto.RegisterRequest;
import com.neuralhealer.backend.model.entity.DoctorProfile;
import com.neuralhealer.backend.model.entity.PatientProfile;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.model.enums.UserRole;
import com.neuralhealer.backend.repository.DoctorProfileRepository;
import com.neuralhealer.backend.repository.PatientProfileRepository;
import com.neuralhealer.backend.repository.UserRepository;
import com.neuralhealer.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication service handling user registration and login.
 * 
 * Registration flow:
 * 1. Validate email uniqueness
 * 2. Create user with hashed password
 * 3. Create role-specific profile (Doctor or Patient)
 * 4. Generate JWT token
 * 
 * Login flow:
 * 1. Authenticate credentials via AuthenticationManager
 * 2. Update last login timestamp
 * 3. Generate JWT token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final jakarta.servlet.http.HttpServletRequest httpRequest;

    /**
     * Register a new user.
     * 
     * @param request Registration details
     * @return AuthResponse with JWT token
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, jakarta.servlet.http.HttpServletResponse response) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create user entity
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .isActive(true)
                .mfaEnabled(false)
                .emailVerificationRequired(true)
                .build();

        // Save user first to get ID
        user = userRepository.save(user);
        log.info("Created user with ID: {}", user.getId());

        // Create role-specific profile
        if (request.role() == UserRole.DOCTOR) {
            int initialCompletion = 0;
            if (Boolean.TRUE.equals(request.quickSetup())) {
                if (org.springframework.util.StringUtils.hasText(request.title()))
                    initialCompletion += 10;
                if (org.springframework.util.StringUtils.hasText(request.specialization()))
                    initialCompletion += 10;
            }

            DoctorProfile doctorProfile = DoctorProfile.builder()
                    .user(user)
                    .title(Boolean.TRUE.equals(request.quickSetup()) ? request.title() : null)
                    .specialization(Boolean.TRUE.equals(request.quickSetup()) ? request.specialization() : null)
                    .verificationStatus("unverified")
                    .availabilityStatus("offline")
                    .profileCompletionPercentage(initialCompletion)
                    .rating(0.0)
                    .totalReviews(0)
                    .build();

            doctorProfileRepository.save(doctorProfile);
            user.setDoctorProfile(doctorProfile);
            log.info("Created enhanced doctor profile for user: {} (QuickSetup: {})", user.getId(),
                    request.quickSetup());
        } else if (request.role() == UserRole.PATIENT) {

            PatientProfile patientProfile = PatientProfile.builder()
                    .user(user)
                    .build();
            patientProfileRepository.save(patientProfile);
            user.setPatientProfile(patientProfile);
            log.info("Created patient profile for user: {}", user.getId());
        }

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Set HTTPOnly cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("neuralhealer_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // TODO: Set to true in production
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);

        return AuthResponse.of(
                null, // Token not returned in body
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                request.role(),
                jwtService.getExpirationTime());
    }

    /**
     * Completes registration by generating and sending OTP.
     * Note: Removed @Async to ensure transaction propagation works correctly.
     * The OTP must be saved to the database before the email is sent.
     */
    @Transactional
    public void postRegisterProcessing(User user, String ipAddress, String userAgent) {
        log.info("Post-registration processing for user: {}", user.getEmail());
        otpService.generateAndSendOtp(user, ipAddress, userAgent);
    }

    /**
     * Authenticate user and return JWT token.
     * 
     * @param request Login credentials
     * @return AuthResponse with JWT token
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request, jakarta.servlet.http.HttpServletResponse response) {
        // Authenticate using Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()));

        // Load user after successful authentication
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Check if email verification is required and completed
        if (Boolean.TRUE.equals(user.getEmailVerificationRequired()) && user.getEmailVerifiedAt() == null) {
            log.warn("Login blocked: Email not verified for user {}", user.getEmail());
            // We still return a partial response or throw exception?
            // In many systems, we allow login but restrict access.
            // But here the user asked "after registration is it verified... or what is
            // next?"
            // I'll throw an exception to force verification.
            throw new BadCredentialsException("Email not verified. Please verify your email first.");
        }

        // Determine user role
        UserRole role = determineUserRole(user);

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Set HTTPOnly cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("neuralhealer_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // TODO: Set to true in production
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.of(
                null, // Token not returned in body
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role,
                jwtService.getExpirationTime());
    }

    /**
     * Determine user role based on profile existence.
     */
    private UserRole determineUserRole(User user) {
        if (doctorProfileRepository.existsByUserId(user.getId())) {
            return UserRole.DOCTOR;
        } else if (patientProfileRepository.existsByUserId(user.getId())) {
            return UserRole.PATIENT;
        }
        return UserRole.PATIENT; // Default fallback
    }
}
