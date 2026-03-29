package com.homehealthcare.evv.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceMetadataSnapshotRepository extends JpaRepository<DeviceMetadataSnapshot, UUID> {

    Optional<DeviceMetadataSnapshot> findByClockEvent_Id(UUID clockEventId);
}
