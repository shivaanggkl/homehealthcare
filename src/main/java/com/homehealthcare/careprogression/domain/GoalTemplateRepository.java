package com.homehealthcare.careprogression.domain;

import com.homehealthcare.careprogression.foundation.GoalTemplateLifecycleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalTemplateRepository extends JpaRepository<GoalTemplate, UUID> {

    List<GoalTemplate> findAllByAgency_IdOrderByNameAsc(UUID agencyId);

    List<GoalTemplate> findAllByAgency_IdAndStatusOrderByNameAsc(UUID agencyId, GoalTemplateLifecycleStatus status);
}
