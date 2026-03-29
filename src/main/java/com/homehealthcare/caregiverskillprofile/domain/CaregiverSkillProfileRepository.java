package com.homehealthcare.caregiverskillprofile.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverSkillProfileRepository extends JpaRepository<CaregiverSkillProfile, UUID> {

    boolean existsByCaregiverProfile_IdAndSkill_Id(UUID caregiverProfileId, UUID skillId);

    boolean existsByCaregiverProfile_IdAndSkill_IdAndIdNot(UUID caregiverProfileId, UUID skillId, UUID id);

    Optional<CaregiverSkillProfile> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverSkillProfile> findAllByCaregiverProfile_IdOrderByCreatedAtAsc(UUID caregiverProfileId);
}
