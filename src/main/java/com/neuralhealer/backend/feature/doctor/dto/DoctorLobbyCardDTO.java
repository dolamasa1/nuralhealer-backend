package com.neuralhealer.backend.feature.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorLobbyCardDTO {

    private String id;
    private String fullName;
    private String title;
    private String specialization;
    private Integer yearsOfExperience;
    private Double rating;
    private Integer totalReviews;
    private String profilePictureThumbnailUrl;
    private String location;
    private String availabilityStatus;
    private String verificationStatus;
    private Boolean isVerified;
    private Double consultationFee;
    private Double distance; // Only present if geolocation used
}
