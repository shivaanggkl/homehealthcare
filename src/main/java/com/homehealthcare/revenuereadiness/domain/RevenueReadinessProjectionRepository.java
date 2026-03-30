package com.homehealthcare.revenuereadiness.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueReadinessProjectionRepository extends JpaRepository<RevenueReadinessProjection, UUID> {

    Optional<RevenueReadinessProjection> findByVisitOccurrence_Id(UUID visitOccurrenceId);

    List<RevenueReadinessProjection> findAllByAgency_IdOrderByEvaluatedAtDesc(UUID agencyId);
}
