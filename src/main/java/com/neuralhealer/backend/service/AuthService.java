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
                .build();

        // Save user first to get ID
        user = userRepository.save(user);
        log.info("Created user with ID: {}", user.getId());

        // Create role-specific profile
        if (request.role() == UserRole.DOCTOR) {
            DoctorProfile doctorProfile = DoctorProfile.builder()
                    .user(user)
                    .isVerified(false)
                    .build();
            doctorProfileRepository.save(doctorProfile);
            user.setDoctorProfile(doctorProfile);
            log.info("Created doctor profile for user: {}", user.getId());
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
