package com.homehealthcare.tasktemplate.application;

import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.ConfigurationEntityNotFoundException;
import com.homehealthcare.configuration.foundation.DuplicateConfigurationException;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class TaskTemplateCatalogService {

    private final TaskTemplateRepository taskTemplateRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public TaskTemplate create(@NotNull AgencyMembership actorMembership, @Valid ManageTaskTemplateCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        VisitType visitType = resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId());

        TaskTemplate saved = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                actorMembership.getAgency(),
                serviceLine,
                visitType,
                command.name(),
                command.code(),
                command.description(),
                command.category(),
                command.displayOrder()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.TASK_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public TaskTemplate update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID taskTemplateId,
            @Valid ManageTaskTemplateCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        TaskTemplate template = taskTemplateRepository.findById(taskTemplateId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("TaskTemplate", taskTemplateId));
        assertSameAgency(actorMembership.getAgencyId(), template.getAgencyId(), "TaskTemplate", taskTemplateId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), taskTemplateId);
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        VisitType visitType = resolveVisitType(actorMembership.getAgencyId(), command.visitTypeId());

        template.updateDetails(
                serviceLine,
                visitType,
                command.name(),
                command.code(),
                command.description(),
                command.category(),
                command.displayOrder());
        template.activate();
        TaskTemplate saved = taskTemplateRepository.saveAndFlush(template);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.TASK_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public TaskTemplate deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID taskTemplateId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        TaskTemplate template = taskTemplateRepository.findById(taskTemplateId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("TaskTemplate", taskTemplateId));
        assertSameAgency(actorMembership.getAgencyId(), template.getAgencyId(), "TaskTemplate", taskTemplateId);
        template.deactivate();
        TaskTemplate saved = taskTemplateRepository.saveAndFlush(template);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.TASK_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> agencyId.equals(serviceLine.getAgencyId()))
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private VisitType resolveVisitType(UUID agencyId, UUID visitTypeId) {
        if (visitTypeId == null) {
            return null;
        }
        return visitTypeRepository.findById(visitTypeId)
                .filter(visitType -> agencyId.equals(visitType.getAgencyId()))
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("VisitType", visitTypeId));
    }

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? taskTemplateRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : taskTemplateRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("TaskTemplate", agencyId, "name", name);
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        boolean duplicateCode = existingId == null
                ? taskTemplateRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : taskTemplateRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("TaskTemplate", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(TaskTemplate template) {
        return "{\"name\":\"" + template.getName() + "\",\"code\":\"" + template.getCode()
                + "\",\"category\":\"" + template.getCategory().name() + "\"}";
    }

    public record ManageTaskTemplateCommand(
            UUID serviceLineId,
            UUID visitTypeId,
            @NotBlank String name,
            @NotBlank String code,
            String description,
            @NotNull TaskTemplateCategory category,
            int displayOrder) {
    }
}
