package com.neuralhealer.backend.service;

import com.neuralhealer.backend.shared.mapper.DoctorMapper;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyCardDTO;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyFilterRequest;
import com.neuralhealer.backend.feature.doctor.entity.DoctorProfile;
import com.neuralhealer.backend.shared.entity.User;
import com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;
import com.neuralhealer.backend.feature.doctor.service.DoctorLobbyServiceImpl;
import com.neuralhealer.backend.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorLobbyServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorLobbyServiceImpl doctorLobbyService;

    private DoctorProfile profile;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().firstName("John").lastName("Doe").build();
        profile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .specialization("Psychiatrist")
                .rating(4.5)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDoctorLobby_Success() {
        DoctorLobbyFilterRequest filters = DoctorLobbyFilterRequest.builder()
                .page(0)
                .size(20)
                .sortBy("rating")
                .sortDirection("desc")
                .build();

        Page<DoctorProfile> page = new PageImpl<>(List.of(profile));
        when(doctorProfileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        when(fileStorageService.getPublicUrl(any(), eq(true))).thenReturn("http://thumb.url");
        when(doctorMapper.toLobbyCardDTO(any(), any(), any())).thenReturn(DoctorLobbyCardDTO.builder().build());

        Page<DoctorLobbyCardDTO> result = doctorLobbyService.getDoctorLobby(filters);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(doctorProfileRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
