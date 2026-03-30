package com.homehealthcare.revenuereadiness.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationUsageSnapshotRepository extends JpaRepository<AuthorizationUsageSnapshot, UUID> {

    Optional<AuthorizationUsageSnapshot> findByAuthorization_Id(UUID authorizationId);
}
