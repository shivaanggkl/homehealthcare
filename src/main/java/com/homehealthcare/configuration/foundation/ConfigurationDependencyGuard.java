package com.homehealthcare.configuration.foundation;

import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigurationDependencyGuard {

    private final VisitTypeRepository visitTypeRepository;
    private final TaskTemplateRepository taskTemplateRepository;

    public void assertServiceLineCanDeactivate(UUID serviceLineId) {
        if (visitTypeRepository.existsByServiceLine_IdAndStatus(serviceLineId, ConfigurationStatus.ACTIVE)) {
            throw new ConfigurationDependencyConflictException("ServiceLine", serviceLineId, "active visit types");
        }
        if (taskTemplateRepository.existsByServiceLine_IdAndStatus(serviceLineId, ConfigurationStatus.ACTIVE)) {
            throw new ConfigurationDependencyConflictException("ServiceLine", serviceLineId, "active task templates");
        }
    }

    public void assertVisitTypeCanDeactivate(UUID visitTypeId) {
        if (taskTemplateRepository.existsByVisitType_IdAndStatus(visitTypeId, ConfigurationStatus.ACTIVE)) {
            throw new ConfigurationDependencyConflictException("VisitType", visitTypeId, "active task templates");
        }
    }
}
