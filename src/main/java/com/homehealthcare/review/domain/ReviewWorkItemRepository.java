package com.homehealthcare.review.domain;

import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewSourceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewWorkItemRepository extends JpaRepository<ReviewWorkItem, UUID> {

    Optional<ReviewWorkItem> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByAgency_IdAndSourceTypeAndSourceRecordIdAndStatusIn(
            UUID agencyId,
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            Collection<ReviewLifecycleStatus> statuses);

    List<ReviewWorkItem> findAllByAgency_IdOrderByEnteredQueueAtDesc(UUID agencyId);
}
