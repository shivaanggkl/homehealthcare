package com.homehealthcare.messaging.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadParticipantRepository extends JpaRepository<ThreadParticipant, UUID> {

    boolean existsByThread_IdAndMembership_IdAndRemovedAtIsNull(UUID threadId, UUID membershipId);

    List<ThreadParticipant> findAllByThread_IdAndRemovedAtIsNullOrderByAddedAtAsc(UUID threadId);

    Optional<ThreadParticipant> findByThread_IdAndMembership_IdAndRemovedAtIsNull(UUID threadId, UUID membershipId);
}
