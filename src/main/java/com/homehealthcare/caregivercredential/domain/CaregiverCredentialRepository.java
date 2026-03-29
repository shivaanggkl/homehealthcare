package com.homehealthcare.caregivercredential.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverCredentialRepository extends JpaRepository<CaregiverCredential, UUID> {

    boolean existsByCaregiverProfile_IdAndCredentialTypeIgnoreCaseAndLicenseNumber(UUID caregiverProfileId, String credentialType, String licenseNumber);

    boolean existsByCaregiverProfile_IdAndCredentialTypeIgnoreCaseAndLicenseNumberAndIdNot(
            UUID caregiverProfileId,
            String credentialType,
            String licenseNumber,
            UUID id);

    Optional<CaregiverCredential> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverCredential> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
