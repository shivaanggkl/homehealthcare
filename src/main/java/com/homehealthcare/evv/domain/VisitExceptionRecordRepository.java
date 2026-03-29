package com.homehealthcare.evv.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitExceptionRecordRepository extends JpaRepository<VisitExceptionRecord, UUID> {

    Optional<VisitExceptionRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<VisitExceptionRecord> findAllByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    long countByVerificationSession_IdAndStatusIn(UUID verificationSessionId, Collection<VisitExceptionStatus> statuses);
}
