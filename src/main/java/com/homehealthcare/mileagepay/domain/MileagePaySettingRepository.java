package com.homehealthcare.mileagepay.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MileagePaySettingRepository extends JpaRepository<MileagePaySetting, UUID> {

    boolean existsByAgency_IdAndBranchIsNull(UUID agencyId);

    boolean existsByAgency_IdAndBranch_Id(UUID agencyId, UUID branchId);

    boolean existsByAgency_IdAndBranchIsNullAndIdNot(UUID agencyId, UUID id);

    boolean existsByAgency_IdAndBranch_IdAndIdNot(UUID agencyId, UUID branchId, UUID id);
}
