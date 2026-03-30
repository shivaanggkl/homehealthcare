package com.homehealthcare.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissingFieldResultRepository extends JpaRepository<MissingFieldResult, UUID> {

    List<MissingFieldResult> findAllByWorkItem_IdOrderByFieldPathAsc(UUID workItemId);

    List<MissingFieldResult> findAllByCompletenessCheckResult_IdOrderByFieldPathAsc(UUID completenessCheckResultId);
}
