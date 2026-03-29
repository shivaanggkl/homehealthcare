package com.homehealthcare.caregiverprofile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverProfileRepository extends JpaRepository<CaregiverProfile, UUID> {

    boolean existsByAgency_IdAndAgencyMembership_Id(UUID agencyId, UUID agencyMembershipId);

    Optional<CaregiverProfile> findFirstByAgency_IdAndAgencyMembership_Id(UUID agencyId, UUID agencyMembershipId);

    Optional<CaregiverProfile> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverProfile> findAllByAgency_IdOrderByCreatedAtAsc(UUID agencyId);
}
