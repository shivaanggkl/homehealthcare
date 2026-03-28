package com.homehealthcare.documentationtemplate.application;

import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.ConfigurationEntityNotFoundException;
import com.homehealthcare.configuration.foundation.DuplicateConfigurationException;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
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
public class DocumentationTemplateCatalogService {

    private final DocumentationTemplateRepository documentationTemplateRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public DocumentationTemplate createDraft(
            @NotNull AgencyMembership actorMembership,
            @Valid ManageDocumentationTemplateCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);

        DocumentationTemplate saved = documentationTemplateRepository.saveAndFlush(DocumentationTemplate.createDraft(
                actorMembership.getAgency(),
                command.name(),
                command.code(),
                command.templateType(),
                command.structuredDefinitionJson(),
                command.displayOrder()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.DOCUMENTATION_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public DocumentationTemplate updateDraft(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID templateId,
            @Valid ManageDocumentationTemplateCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        DocumentationTemplate template = documentationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("DocumentationTemplate", templateId));
        assertSameAgency(actorMembership.getAgencyId(), template.getAgencyId(), "DocumentationTemplate", templateId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), templateId);

        template.updateDraft(
                command.name(),
                command.code(),
                command.templateType(),
                command.structuredDefinitionJson(),
                command.displayOrder());
        DocumentationTemplate saved = documentationTemplateRepository.saveAndFlush(template);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.DOCUMENTATION_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public DocumentationTemplate publish(@NotNull AgencyMembership actorMembership, @NotNull UUID templateId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        DocumentationTemplate template = documentationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("DocumentationTemplate", templateId));
        assertSameAgency(actorMembership.getAgencyId(), template.getAgencyId(), "DocumentationTemplate", templateId);
        template.publish();
        DocumentationTemplate saved = documentationTemplateRepository.saveAndFlush(template);
        configurationAuditService.recordPublished(
                actorMembership,
                Epic2ConfigurationTargetType.DOCUMENTATION_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public DocumentationTemplate deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID templateId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        DocumentationTemplate template = documentationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("DocumentationTemplate", templateId));
        assertSameAgency(actorMembership.getAgencyId(), template.getAgencyId(), "DocumentationTemplate", templateId);
        template.deactivate();
        DocumentationTemplate saved = documentationTemplateRepository.saveAndFlush(template);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.DOCUMENTATION_TEMPLATE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? documentationTemplateRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : documentationTemplateRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("DocumentationTemplate", agencyId, "name", name);
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        boolean duplicateCode = existingId == null
                ? documentationTemplateRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : documentationTemplateRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("DocumentationTemplate", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(DocumentationTemplate template) {
        return "{\"name\":\"" + template.getName() + "\",\"code\":\"" + template.getCode()
                + "\",\"version\":" + template.getVersion() + "}";
    }

    public record ManageDocumentationTemplateCommand(
            @NotBlank String name,
            @NotBlank String code,
            @NotNull DocumentationTemplateType templateType,
            @NotBlank String structuredDefinitionJson,
            int displayOrder) {
    }
}
