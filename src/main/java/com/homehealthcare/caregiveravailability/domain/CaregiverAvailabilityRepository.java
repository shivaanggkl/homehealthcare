package com.homehealthcare.caregiveravailability.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverAvailabilityRepository extends JpaRepository<CaregiverAvailability, UUID> {

    Optional<CaregiverAvailability> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverAvailability> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
