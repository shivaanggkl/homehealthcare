package com.homehealthcare.evv.domain;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissedVisitRecordRepository extends JpaRepository<MissedVisitRecord, UUID> {

    Optional<MissedVisitRecord> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByVisitOccurrence_IdAndStatusIn(UUID visitOccurrenceId, Collection<MissedVisitStatus> statuses);
}
