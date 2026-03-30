package com.homehealthcare.messaging.domain;

import com.homehealthcare.messaging.foundation.CommunicationThreadStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationThreadRepository extends JpaRepository<CommunicationThread, UUID> {

    Optional<CommunicationThread> findByIdAndAgency_Id(UUID threadId, UUID agencyId);

    List<CommunicationThread> findAllByAgency_IdAndPatient_IdOrderByLastMessageAtDescCreatedAtDesc(UUID agencyId, UUID patientId);

    List<CommunicationThread> findAllByAgency_IdAndVisitOccurrence_IdOrderByLastMessageAtDescCreatedAtDesc(UUID agencyId, UUID visitOccurrenceId);

    List<CommunicationThread> findAllByAgency_IdOrderByLastMessageAtDescCreatedAtDesc(UUID agencyId);

    long countByAgency_IdAndEscalationStatusNot(UUID agencyId, com.homehealthcare.messaging.foundation.MessagingEscalationStatus escalationStatus);

    long countByAgency_IdAndStatus(UUID agencyId, CommunicationThreadStatus status);
}
