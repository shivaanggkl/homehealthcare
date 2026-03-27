package com.homehealthcare.membership.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AgencyMembershipRepository extends JpaRepository<AgencyMembership, UUID>, JpaSpecificationExecutor<AgencyMembership> {

    boolean existsByUser_IdAndAgency_Id(UUID userId, UUID agencyId);

    boolean existsByUser_IdAndAgency_IdAndStatus(UUID userId, UUID agencyId, AgencyMembershipStatus status);

    Optional<AgencyMembership> findByUser_IdAndAgency_Id(UUID userId, UUID agencyId);

    List<AgencyMembership> findAllByUser_IdAndStatus(UUID userId, AgencyMembershipStatus status);
}
