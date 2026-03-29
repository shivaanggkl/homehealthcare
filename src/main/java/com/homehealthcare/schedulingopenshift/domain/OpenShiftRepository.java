package com.homehealthcare.schedulingopenshift.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenShiftRepository extends JpaRepository<OpenShift, UUID> {

    Optional<OpenShift> findByIdAndAgency_Id(UUID id, UUID agencyId);

    Optional<OpenShift> findFirstByVisitOccurrence_IdAndStatusOrderByOpenedAtDesc(UUID visitOccurrenceId, OpenShiftStatus status);

    List<OpenShift> findAllByAgency_IdOrderByOpenedAtAsc(UUID agencyId);
}
