package com.homehealthcare.agencyprofile.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyProfileRepository extends JpaRepository<AgencyProfile, UUID> {

    Optional<AgencyProfile> findByAgency_Id(UUID agencyId);

    boolean existsByAgency_Id(UUID agencyId);
}
