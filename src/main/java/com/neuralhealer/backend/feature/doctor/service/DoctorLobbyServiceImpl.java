package com.neuralhealer.backend.feature.doctor.service;

import com.neuralhealer.backend.shared.mapper.DoctorMapper;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyCardDTO;
import com.neuralhealer.backend.feature.doctor.dto.DoctorLobbyFilterRequest;
import com.neuralhealer.backend.feature.doctor.entity.DoctorProfile;
import com.neuralhealer.backend.feature.doctor.repository.DoctorProfileRepository;
import com.neuralhealer.backend.feature.doctor.repository.specification.DoctorProfileSpecifications;
import com.neuralhealer.backend.feature.doctor.service.DoctorLobbyService;
import com.neuralhealer.backend.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorLobbyServiceImpl implements DoctorLobbyService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final FileStorageService fileStorageService;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "doctorLobbyCache")
    public Page<DoctorLobbyCardDTO> getDoctorLobby(DoctorLobbyFilterRequest filters) {
        Specification<DoctorProfile> spec = DoctorProfileSpecifications.buildFilters(filters);

        Sort sort = Sort.by(Sort.Direction.fromString(filters.getSortDirection()), filters.getSortBy());
        if (filters.getSortBy().equals("rating")) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "totalReviews"));
        }

        Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize(), sort);
        Page<DoctorProfile> profilesPage = doctorProfileRepository.findAll(spec, pageable);

        return profilesPage.map(profile -> {
            String thumbUrl = fileStorageService.getPublicUrl(profile.getProfilePicturePath(), true);
            return doctorMapper.toLobbyCardDTO(profile, profile.getUser(), thumbUrl);
        });
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "doctorLobbyCache")
    public Page<DoctorLobbyCardDTO> searchDoctors(String query, Pageable pageable) {
        Specification<DoctorProfile> spec = DoctorProfileSpecifications.searchByQuery(query);
        Page<DoctorProfile> profilesPage = doctorProfileRepository.findAll(spec, pageable);

        return profilesPage.map(profile -> {
            String thumbUrl = fileStorageService.getPublicUrl(profile.getProfilePicturePath(), true);
            return doctorMapper.toLobbyCardDTO(profile, profile.getUser(), thumbUrl);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorLobbyCardDTO> getNearbyDoctors(double lat, double lng, int radiusKm) {
        // This requires PostGIS. For now, we'll provide a simplified version or
        // a placeholder that returns doctors from the same city if location matches.
        // In a real production app with PostGIS, we would use a native query.

        // Since we want to avoid over-engineering and haven't confirmed PostGIS setup,
        // we will stick to basic filtering for now or return an empty list with a note.
        // Actually, let's implement a basic version that filters by coordinates if they
        // exist.

        // Simplified bounding box approach (very rough approximation)
        double latRange = radiusKm / 111.0;
        double lngRange = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        Specification<DoctorProfile> spec = (root, query, cb) -> cb.and(
                cb.between(root.get("latitude"), lat - latRange, lat + latRange),
                cb.between(root.get("longitude"), lng - lngRange, lng + lngRange));

        List<DoctorProfile> profiles = doctorProfileRepository.findAll(spec);

        return profiles.stream()
                .map(profile -> {
                    String thumbUrl = fileStorageService.getPublicUrl(profile.getProfilePicturePath(), true);
                    return doctorMapper.toLobbyCardDTO(profile, profile.getUser(), thumbUrl);
                })
                .collect(Collectors.toList());
    }
}
