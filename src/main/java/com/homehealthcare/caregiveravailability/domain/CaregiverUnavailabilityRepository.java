package com.homehealthcare.caregiveravailability.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverUnavailabilityRepository extends JpaRepository<CaregiverUnavailability, UUID> {

    Optional<CaregiverUnavailability> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverUnavailability> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
