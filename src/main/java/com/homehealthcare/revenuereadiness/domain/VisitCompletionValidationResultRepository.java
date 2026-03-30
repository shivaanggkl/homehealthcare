package com.homehealthcare.revenuereadiness.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitCompletionValidationResultRepository extends JpaRepository<VisitCompletionValidationResult, UUID> {

    Optional<VisitCompletionValidationResult> findByVisitOccurrence_Id(UUID visitOccurrenceId);
}
