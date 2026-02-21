package com.neuralhealer.backend.feature.doctor.service;

import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyCardDTO;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoctorLobbyService {
    Page<DoctorLobbyCardDTO> getDoctorLobby(DoctorLobbyFilterRequest filters);

    Page<DoctorLobbyCardDTO> searchDoctors(String query, Pageable pageable);

    List<DoctorLobbyCardDTO> getNearbyDoctors(double lat, double lng, int radiusKm);
}
