package com.homehealthcare.visittype.application;

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
public class VisitTypeCatalogService {

    private final VisitTypeRepository visitTypeRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public VisitType create(@NotNull AgencyMembership actorMembership, @Valid ManageVisitTypeCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);

        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        VisitType saved = visitTypeRepository.saveAndFlush(VisitType.create(
                actorMembership.getAgency(),
                serviceLine,
                command.name(),
                command.code(),
                command.description(),
                command.defaultDurationMinutes(),
                command.billable(),
                command.displayOrder()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.VISIT_TYPE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public VisitType update(@NotNull AgencyMembership actorMembership, @NotNull UUID visitTypeId, @Valid ManageVisitTypeCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        VisitType visitType = visitTypeRepository.findById(visitTypeId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("VisitType", visitTypeId));
        assertSameAgency(actorMembership.getAgencyId(), visitType.getAgencyId(), "VisitType", visitTypeId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), visitTypeId);

        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        visitType.updateDetails(
                serviceLine,
                command.name(),
                command.code(),
                command.description(),
                command.defaultDurationMinutes(),
                command.billable(),
                command.displayOrder());
        visitType.activate();
        VisitType saved = visitTypeRepository.saveAndFlush(visitType);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.VISIT_TYPE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public VisitType deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID visitTypeId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        VisitType visitType = visitTypeRepository.findById(visitTypeId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("VisitType", visitTypeId));
        assertSameAgency(actorMembership.getAgencyId(), visitType.getAgencyId(), "VisitType", visitTypeId);

        visitType.deactivate();
        VisitType saved = visitTypeRepository.saveAndFlush(visitType);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.VISIT_TYPE,
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

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? visitTypeRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : visitTypeRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("VisitType", agencyId, "name", name);
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        boolean duplicateCode = existingId == null
                ? visitTypeRepository.existsByAgency_IdAndCode(agencyId, normalizedCode)
                : visitTypeRepository.existsByAgency_IdAndCodeAndIdNot(agencyId, normalizedCode, existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("VisitType", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(VisitType visitType) {
        return "{\"name\":\"" + visitType.getName()
                + "\",\"code\":\"" + visitType.getCode()
                + "\",\"billable\":" + visitType.isBillable() + "}";
    }

    public record ManageVisitTypeCommand(
            UUID serviceLineId,
            @NotBlank String name,
            @NotBlank String code,
            String description,
            int defaultDurationMinutes,
            boolean billable,
            int displayOrder) {
    }
}
