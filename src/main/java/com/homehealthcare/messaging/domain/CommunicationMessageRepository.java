package com.homehealthcare.messaging.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage, UUID> {

    Optional<CommunicationMessage> findByIdAndAgency_Id(UUID messageId, UUID agencyId);

    List<CommunicationMessage> findAllByThread_IdOrderByCreatedAtAtSourceAsc(UUID threadId);
}
