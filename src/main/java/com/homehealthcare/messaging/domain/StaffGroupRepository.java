package com.homehealthcare.messaging.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffGroupRepository extends JpaRepository<StaffGroup, UUID> {

    Optional<StaffGroup> findByIdAndAgency_Id(UUID staffGroupId, UUID agencyId);

    List<StaffGroup> findAllByAgency_IdOrderByNameAsc(UUID agencyId);
}
