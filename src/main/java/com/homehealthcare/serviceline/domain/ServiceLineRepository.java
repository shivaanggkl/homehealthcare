package com.homehealthcare.serviceline.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceLineRepository extends JpaRepository<ServiceLine, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    boolean existsByAgency_IdAndNameAndIdNot(UUID agencyId, String name, UUID id);

    boolean existsByAgency_IdAndCodeAndIdNot(UUID agencyId, String code, UUID id);

    Optional<ServiceLine> findByAgency_IdAndCode(UUID agencyId, String code);

    List<ServiceLine> findAllByAgency_IdOrderByDisplayOrderAscNameAsc(UUID agencyId);
}
