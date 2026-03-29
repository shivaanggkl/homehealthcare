package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileMessageEntryRepository extends JpaRepository<MobileMessageEntry, UUID> {

    List<MobileMessageEntry> findAllByThread_IdOrderBySentAtAsc(UUID threadId);

    long countByThread_IdAndSenderMembership_IdNot(UUID threadId, UUID senderMembershipId);
}
