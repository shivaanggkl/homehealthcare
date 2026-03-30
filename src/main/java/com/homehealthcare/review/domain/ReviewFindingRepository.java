package com.homehealthcare.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, UUID> {

    List<ReviewFinding> findAllByWorkItem_IdOrderByEvaluatedAtAscRuleCodeAsc(UUID workItemId);

    List<ReviewFinding> findAllByCompletenessCheckResult_IdOrderByRuleCodeAsc(UUID completenessCheckResultId);
}
