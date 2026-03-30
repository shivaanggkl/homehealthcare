package com.homehealthcare.messaging.domain;

import com.homehealthcare.messaging.foundation.CoordinationContextType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationContextLinkRepository extends JpaRepository<CommunicationContextLink, UUID> {

    List<CommunicationContextLink> findAllByThread_IdOrderByContextTypeAsc(UUID threadId);

    List<CommunicationContextLink> findAllByContextTypeAndContextId(CoordinationContextType contextType, UUID contextId);
}
