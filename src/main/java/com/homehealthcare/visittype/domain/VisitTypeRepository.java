package com.homehealthcare.visittype.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitTypeRepository extends JpaRepository<VisitType, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    boolean existsByAgency_IdAndNameAndIdNot(UUID agencyId, String name, UUID id);

    boolean existsByAgency_IdAndCodeAndIdNot(UUID agencyId, String code, UUID id);

    List<VisitType> findAllByAgency_IdOrderByDisplayOrderAscNameAsc(UUID agencyId);

    Optional<VisitType> findByAgency_IdAndCode(UUID agencyId, String code);
}
