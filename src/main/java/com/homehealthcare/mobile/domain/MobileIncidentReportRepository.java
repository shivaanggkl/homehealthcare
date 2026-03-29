package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileIncidentReportRepository extends JpaRepository<MobileIncidentReport, UUID> {

    Optional<MobileIncidentReport> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<MobileIncidentReport> findAllByExecutionSession_IdOrderByReportedAtDesc(UUID executionSessionId);
}
