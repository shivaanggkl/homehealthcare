package com.homehealthcare.branch.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    boolean existsByIdAndAgency_Id(UUID branchId, UUID agencyId);

    Optional<Branch> findByIdAndAgency_Id(UUID branchId, UUID agencyId);

    List<Branch> findAllByAgency_IdOrderByNameAsc(UUID agencyId);

    List<Branch> findAllByAgency_IdAndIdInOrderByNameAsc(UUID agencyId, Collection<UUID> branchIds);

    Optional<Branch> findByAgency_IdAndCode(UUID agencyId, String code);
}
