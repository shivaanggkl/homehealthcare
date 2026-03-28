package com.homehealthcare.serviceline.application;

import com.homehealthcare.agency.domain.Agency;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ServiceLineCatalogService {

    private final ServiceLineRepository serviceLineRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public ServiceLine create(@NotNull AgencyMembership actorMembership, @Valid ManageServiceLineCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        Agency agency = actorMembership.getAgency();
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), null);

        ServiceLine saved = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agency, command.name(), command.code(), command.description(), command.displayOrder()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.SERVICE_LINE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public ServiceLine update(@NotNull AgencyMembership actorMembership, @NotNull UUID serviceLineId, @Valid ManageServiceLineCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        ServiceLine serviceLine = serviceLineRepository.findById(serviceLineId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("ServiceLine", serviceLineId));
        assertSameAgency(actorMembership.getAgencyId(), serviceLine.getAgencyId(), "ServiceLine", serviceLineId);
        assertUnique(actorMembership.getAgencyId(), command.name(), command.code(), serviceLineId);

        serviceLine.updateDetails(command.name(), command.code(), command.description(), command.displayOrder());
        serviceLine.activate();
        ServiceLine saved = serviceLineRepository.saveAndFlush(serviceLine);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.SERVICE_LINE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    @Transactional
    public ServiceLine deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID serviceLineId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        ServiceLine serviceLine = serviceLineRepository.findById(serviceLineId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("ServiceLine", serviceLineId));
        assertSameAgency(actorMembership.getAgencyId(), serviceLine.getAgencyId(), "ServiceLine", serviceLineId);

        serviceLine.deactivate();
        ServiceLine saved = serviceLineRepository.saveAndFlush(serviceLine);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.SERVICE_LINE,
                saved.getId(),
                null,
                metadata(saved));
        return saved;
    }

    private void assertUnique(UUID agencyId, String name, String code, UUID existingId) {
        boolean duplicateName = existingId == null
                ? serviceLineRepository.existsByAgency_IdAndName(agencyId, name.trim())
                : serviceLineRepository.existsByAgency_IdAndNameAndIdNot(agencyId, name.trim(), existingId);
        if (duplicateName) {
            throw new DuplicateConfigurationException("ServiceLine", agencyId, "name", name);
        }

        boolean duplicateCode = existingId == null
                ? serviceLineRepository.existsByAgency_IdAndCode(agencyId, code.trim().toUpperCase(java.util.Locale.ROOT))
                : serviceLineRepository.existsByAgency_IdAndCodeAndIdNot(
                        agencyId,
                        code.trim().toUpperCase(java.util.Locale.ROOT),
                        existingId);
        if (duplicateCode) {
            throw new DuplicateConfigurationException("ServiceLine", agencyId, "code", code);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(ServiceLine serviceLine) {
        return "{\"name\":\"" + serviceLine.getName() + "\",\"code\":\"" + serviceLine.getCode() + "\"}";
    }

    public record ManageServiceLineCommand(
            @NotBlank String name,
            @NotBlank String code,
            String description,
            int displayOrder) {
    }
}
