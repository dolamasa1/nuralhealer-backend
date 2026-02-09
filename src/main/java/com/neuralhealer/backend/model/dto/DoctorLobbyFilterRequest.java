package com.neuralhealer.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorLobbyFilterRequest {

    private String specialization;
    private String verificationStatus;
    private String availabilityStatus;
    private Double minRating;
    private String location;

    @Builder.Default
    private String sortBy = "rating";

    @Builder.Default
    private String sortDirection = "desc";

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

}
