package com.homehealthcare.tasktemplate.domain;

import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, UUID> {

    boolean existsByAgency_IdAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndCode(UUID agencyId, String code);

    boolean existsByAgency_IdAndNameAndIdNot(UUID agencyId, String name, UUID id);

    boolean existsByAgency_IdAndCodeAndIdNot(UUID agencyId, String code, UUID id);

    List<TaskTemplate> findAllByAgency_IdOrderByDisplayOrderAscNameAsc(UUID agencyId);

    boolean existsByServiceLine_IdAndStatus(UUID serviceLineId, ConfigurationStatus status);

    boolean existsByVisitType_IdAndStatus(UUID visitTypeId, ConfigurationStatus status);
}
