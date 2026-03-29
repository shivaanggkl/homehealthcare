package com.homehealthcare.caregivergeography.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverGeographyPreferenceRepository extends JpaRepository<CaregiverGeographyPreference, UUID> {

    Optional<CaregiverGeographyPreference> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverGeographyPreference> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
