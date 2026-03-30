package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueExceptionFlagRepository extends JpaRepository<RevenueExceptionFlag, UUID> {

    List<RevenueExceptionFlag> findAllByAgency_IdOrderByDetectedAtDesc(UUID agencyId);

    List<RevenueExceptionFlag> findAllByTargetTypeAndTargetIdOrderByDetectedAtDesc(RevenueExceptionTargetType targetType, UUID targetId);

    Optional<RevenueExceptionFlag> findFirstByTargetTypeAndTargetIdAndExceptionTypeAndClearedAtIsNull(
            RevenueExceptionTargetType targetType,
            UUID targetId,
            com.homehealthcare.revenuereadiness.foundation.RevenueExceptionType exceptionType);
}
