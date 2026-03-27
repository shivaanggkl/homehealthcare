package com.homehealthcare.branch.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    Optional<Branch> findByAgency_IdAndCode(UUID agencyId, String code);
}
