package com.homehealthcare.review.domain;

import com.homehealthcare.review.foundation.SignoffRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignoffRequestRepository extends JpaRepository<SignoffRequest, UUID> {

    List<SignoffRequest> findAllByWorkItem_IdOrderByRequestedAtAsc(UUID workItemId);

    Optional<SignoffRequest> findFirstByWorkItem_IdAndStatusOrderByRequestedAtDesc(UUID workItemId, SignoffRequestStatus status);
}
