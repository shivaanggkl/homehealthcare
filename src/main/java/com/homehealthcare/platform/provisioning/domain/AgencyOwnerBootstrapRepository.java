package com.homehealthcare.platform.provisioning.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyOwnerBootstrapRepository extends JpaRepository<AgencyOwnerBootstrap, UUID> {

    List<AgencyOwnerBootstrap> findAllByAgency_IdOrderByCreatedAtAsc(UUID agencyId);

    Optional<AgencyOwnerBootstrap> findFirstByAgency_IdAndStatusOrderByCreatedAtAsc(
            UUID agencyId,
            AgencyOwnerBootstrapStatus status);
}
