package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileVisitTaskChecklistEntryRepository extends JpaRepository<MobileVisitTaskChecklistEntry, UUID> {

    List<MobileVisitTaskChecklistEntry> findAllByExecutionSession_IdOrderBySortOrderAscCreatedAtAsc(UUID executionSessionId);

    Optional<MobileVisitTaskChecklistEntry> findByIdAndAgency_Id(UUID id, UUID agencyId);
}
