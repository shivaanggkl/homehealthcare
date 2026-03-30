package com.homehealthcare.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletenessCheckResultRepository extends JpaRepository<CompletenessCheckResult, UUID> {

    List<CompletenessCheckResult> findAllByWorkItem_IdOrderByRunNumberDesc(UUID workItemId);
}
