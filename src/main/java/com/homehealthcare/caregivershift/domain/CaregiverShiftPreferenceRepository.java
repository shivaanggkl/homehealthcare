package com.homehealthcare.caregivershift.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverShiftPreferenceRepository extends JpaRepository<CaregiverShiftPreference, UUID> {

    Optional<CaregiverShiftPreference> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverShiftPreference> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
