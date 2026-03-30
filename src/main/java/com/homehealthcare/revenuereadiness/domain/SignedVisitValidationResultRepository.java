package com.homehealthcare.revenuereadiness.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignedVisitValidationResultRepository extends JpaRepository<SignedVisitValidationResult, UUID> {

    Optional<SignedVisitValidationResult> findByVisitOccurrence_Id(UUID visitOccurrenceId);
}
