package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileFieldArtifactRepository extends JpaRepository<MobileFieldArtifact, UUID> {

    Optional<MobileFieldArtifact> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<MobileFieldArtifact> findAllByExecutionSession_IdOrderByCreatedAtAsc(UUID executionSessionId);
}
