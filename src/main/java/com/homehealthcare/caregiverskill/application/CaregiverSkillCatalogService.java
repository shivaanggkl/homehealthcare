package com.homehealthcare.caregiverskill.application;

import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.ConfigurationEntityNotFoundException;
import com.homehealthcare.configuration.foundation.DuplicateConfigurationException;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
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
public class CaregiverSkillCatalogService {

    private final CaregiverSkillRepository caregiverSkillRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public CaregiverSkill create(@NotNull AgencyMembership actorMembership, @Valid ManageCaregiverSkillCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);

        CaregiverSkill saved = caregiverSkillRepository.saveAndFlush(
                CaregiverSkill.create(actorMembership.getAgency(), command.name(), command.code(), command.description()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_SKILL,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public CaregiverSkill update(@NotNull AgencyMembership actorMembership, @NotNull UUID skillId, @Valid ManageCaregiverSkillCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        CaregiverSkill skill = caregiverSkillRepository.findById(skillId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("CaregiverSkill", skillId));
        assertSameAgency(actorMembership.getAgencyId(), skill.getAgencyId(), "CaregiverSkill", skillId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), skillId);

        skill.updateDetails(command.name(), command.code(), command.description());
        skill.activate();
        CaregiverSkill saved = caregiverSkillRepository.saveAndFlush(skill);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_SKILL,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public CaregiverSkill deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID skillId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        CaregiverSkill skill = caregiverSkillRepository.findById(skillId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("CaregiverSkill", skillId));
        assertSameAgency(actorMembership.getAgencyId(), skill.getAgencyId(), "CaregiverSkill", skillId);

        skill.deactivate();
        CaregiverSkill saved = caregiverSkillRepository.saveAndFlush(skill);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_SKILL,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? caregiverSkillRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : caregiverSkillRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("CaregiverSkill", agencyId, "name", name);
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        boolean duplicateCode = existingId == null
                ? caregiverSkillRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : caregiverSkillRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("CaregiverSkill", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(CaregiverSkill skill) {
        return "{\"name\":\"" + skill.getName() + "\",\"code\":\"" + skill.getCode() + "\"}";
    }

    public record ManageCaregiverSkillCommand(
            @NotBlank String name,
            @NotBlank String code,
            String description) {
    }
}
