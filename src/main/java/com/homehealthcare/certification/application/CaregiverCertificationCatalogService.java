package com.homehealthcare.certification.application;

import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
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
public class CaregiverCertificationCatalogService {

    private final CaregiverCertificationRepository caregiverCertificationRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public CaregiverCertification create(@NotNull AgencyMembership actorMembership, @Valid ManageCaregiverCertificationCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);

        CaregiverCertification saved = caregiverCertificationRepository.saveAndFlush(CaregiverCertification.create(
                actorMembership.getAgency(),
                command.name(),
                command.code(),
                command.description(),
                command.expirationRequired()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_CERTIFICATION,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public CaregiverCertification update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID certificationId,
            @Valid ManageCaregiverCertificationCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        CaregiverCertification certification = caregiverCertificationRepository.findById(certificationId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("CaregiverCertification", certificationId));
        assertSameAgency(actorMembership.getAgencyId(), certification.getAgencyId(), "CaregiverCertification", certificationId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), certificationId);

        certification.updateDetails(command.name(), command.code(), command.description(), command.expirationRequired());
        certification.activate();
        CaregiverCertification saved = caregiverCertificationRepository.saveAndFlush(certification);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_CERTIFICATION,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public CaregiverCertification deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID certificationId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        CaregiverCertification certification = caregiverCertificationRepository.findById(certificationId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("CaregiverCertification", certificationId));
        assertSameAgency(actorMembership.getAgencyId(), certification.getAgencyId(), "CaregiverCertification", certificationId);

        certification.deactivate();
        CaregiverCertification saved = caregiverCertificationRepository.saveAndFlush(certification);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.CAREGIVER_CERTIFICATION,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? caregiverCertificationRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : caregiverCertificationRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("CaregiverCertification", agencyId, "name", name);
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        boolean duplicateCode = existingId == null
                ? caregiverCertificationRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : caregiverCertificationRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("CaregiverCertification", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(CaregiverCertification certification) {
        return "{\"name\":\"" + certification.getName()
                + "\",\"code\":\"" + certification.getCode()
                + "\",\"expirationRequired\":" + certification.isExpirationRequired() + "}";
    }

    public record ManageCaregiverCertificationCommand(
            @NotBlank String name,
            @NotBlank String code,
            String description,
            boolean expirationRequired) {
    }
}
