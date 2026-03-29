package com.homehealthcare.mobile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileMessageThreadRepository extends JpaRepository<MobileMessageThread, UUID> {

    Optional<MobileMessageThread> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<MobileMessageThread> findAllByCaregiverProfile_IdOrderByLastMessageAtDescCreatedAtDesc(UUID caregiverProfileId);
}
