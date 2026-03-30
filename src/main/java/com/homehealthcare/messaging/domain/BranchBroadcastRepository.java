package com.homehealthcare.messaging.domain;

import com.homehealthcare.messaging.foundation.BranchBroadcastStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchBroadcastRepository extends JpaRepository<BranchBroadcast, UUID> {

    Optional<BranchBroadcast> findByIdAndAgency_Id(UUID broadcastId, UUID agencyId);

    List<BranchBroadcast> findAllByAgency_IdOrderByCreatedAtDesc(UUID agencyId);

    long countByAgency_IdAndStatus(UUID agencyId, BranchBroadcastStatus status);
}
