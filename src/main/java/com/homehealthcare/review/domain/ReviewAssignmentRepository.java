package com.homehealthcare.review.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, UUID> {

    Optional<ReviewAssignment> findFirstByWorkItem_IdAndReleasedAtIsNull(UUID workItemId);

    List<ReviewAssignment> findAllByWorkItem_IdOrderByAssignedAtAsc(UUID workItemId);
}
