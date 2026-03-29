package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileQuickNoteEntryRepository extends JpaRepository<MobileQuickNoteEntry, UUID> {

    List<MobileQuickNoteEntry> findAllByExecutionSession_IdOrderByAuthoredAtAsc(UUID executionSessionId);
}
