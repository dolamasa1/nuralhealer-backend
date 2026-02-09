package com.neuralhealer.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileFullDTO {

    // User info
    private String id;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;

    // Professional info
    private String title;
    private String bio;
    private String specialization;
    private Integer yearsOfExperience;
    private List<Map<String, Object>> certificates;

    // Location
    private LocationDTO location;

    // Visual
    private String profilePictureUrl;
    private String profilePictureThumbnailUrl;

    // Status
    private String verificationStatus;
    private String availabilityStatus;

    // Metrics
    private Double rating;
    private Integer totalReviews;
    private Integer profileCompletion;

    // Contact
    private Map<String, String> socialMedia;

    // Pricing
    private Double consultationFee;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private String city;
        private String country;
        private Double latitude;
        private Double longitude;
    }
}
