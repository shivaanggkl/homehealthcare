package com.homehealthcare.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, UUID> {

    List<ReviewDecision> findAllByWorkItem_IdOrderByDecidedAtAsc(UUID workItemId);
}
