package com.neuralhealer.backend.repository.specification;

import com.neuralhealer.backend.model.dto.DoctorLobbyFilterRequest;
import com.neuralhealer.backend.model.entity.DoctorProfile;
import com.neuralhealer.backend.model.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DoctorProfileSpecifications {

    public static Specification<DoctorProfile> buildFilters(DoctorLobbyFilterRequest filters) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(filters.getSpecialization())) {
                predicates.add(cb.equal(root.get("specialization"), filters.getSpecialization()));
            }

            if (StringUtils.hasText(filters.getVerificationStatus())) {
                predicates.add(cb.equal(root.get("verificationStatus"), filters.getVerificationStatus()));
            }

            if (StringUtils.hasText(filters.getAvailabilityStatus())) {
                predicates.add(cb.equal(root.get("availabilityStatus"), filters.getAvailabilityStatus()));
            }

            if (filters.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filters.getMinRating()));
            }

            if (StringUtils.hasText(filters.getLocation())) {
                predicates.add(
                        cb.like(cb.lower(root.get("locationCity")), "%" + filters.getLocation().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static Specification<DoctorProfile> searchByQuery(String query) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(query)) {
                return null;
            }

            String searchPattern = "%" + query.toLowerCase() + "%";
            Join<DoctorProfile, User> userJoin = root.join("user");

            return cb.or(
                    cb.like(cb.lower(userJoin.get("firstName")), searchPattern),
                    cb.like(cb.lower(userJoin.get("lastName")), searchPattern),
                    cb.like(cb.lower(root.get("title")), searchPattern),
                    cb.like(cb.lower(root.get("bio")), searchPattern),
                    cb.like(cb.lower(root.get("specialization")), searchPattern));
        };
    }
}
