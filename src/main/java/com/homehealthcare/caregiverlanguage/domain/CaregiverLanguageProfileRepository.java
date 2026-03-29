package com.homehealthcare.caregiverlanguage.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverLanguageProfileRepository extends JpaRepository<CaregiverLanguageProfile, UUID> {

    boolean existsByCaregiverProfile_IdAndLanguageCode(UUID caregiverProfileId, String languageCode);

    boolean existsByCaregiverProfile_IdAndLanguageCodeAndIdNot(UUID caregiverProfileId, String languageCode, UUID id);

    Optional<CaregiverLanguageProfile> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverLanguageProfile> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
