package com.homehealthcare.review.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnForFixEventRepository extends JpaRepository<ReturnForFixEvent, UUID> {

    List<ReturnForFixEvent> findAllByWorkItem_IdOrderByReturnedAtAsc(UUID workItemId);

    Optional<ReturnForFixEvent> findFirstByWorkItem_IdAndResolvedAtIsNullOrderByReturnedAtDesc(UUID workItemId);
}
