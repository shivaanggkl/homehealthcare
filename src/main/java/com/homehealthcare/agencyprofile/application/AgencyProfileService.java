package com.homehealthcare.agencyprofile.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.agencyprofile.domain.AgencyProfile;
import com.homehealthcare.agencyprofile.domain.AgencyProfileRepository;
import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AgencyProfileService {

    private final AgencyRepository agencyRepository;
    private final AgencyProfileRepository agencyProfileRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public AgencyProfile upsertProfile(@NotNull AgencyMembership actorMembership, @Valid UpsertAgencyProfileCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);

        Agency agency = agencyRepository.findById(actorMembership.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Agency was not found"));
        AgencyProfile profile = agencyProfileRepository.findByAgency_Id(actorMembership.getAgencyId())
                .orElseGet(() -> AgencyProfile.create(
                        agency,
                        fallback(command.displayName(), agency.getName()),
                        command.legalName(),
                        command.primaryPhone(),
                        command.primaryAddress(),
                        fallback(command.operationsContactName(), agency.getName()),
                        fallback(command.operationsContactEmail(), agency.getContactEmail()),
                        command.supportContactName(),
                        command.supportContactEmail(),
                        command.defaultTimezone(),
                        command.defaultLocale()));

        boolean created = profile.getId() == null || !agencyProfileRepository.existsById(profile.getId());
        if (!created) {
            profile.updateProfile(
                    command.displayName(),
                    command.legalName(),
                    command.primaryPhone(),
                    command.primaryAddress(),
                    command.operationsContactName(),
                    command.operationsContactEmail(),
                    command.supportContactName(),
                    command.supportContactEmail(),
                    command.defaultTimezone(),
                    command.defaultLocale());
            profile.activate();
        }

        AgencyProfile saved = agencyProfileRepository.saveAndFlush(profile);
        String metadata = "{\"defaultTimezone\":\"" + saved.getDefaultTimezone()
                + "\",\"defaultLocale\":\"" + saved.getDefaultLocale() + "\"}";
        if (created) {
            configurationAuditService.recordCreated(
                    actorMembership,
                    Epic2ConfigurationTargetType.AGENCY_PROFILE,
                    saved.getId(),
                    null,
                    metadata);
        } else {
            configurationAuditService.recordUpdated(
                    actorMembership,
                    Epic2ConfigurationTargetType.AGENCY_PROFILE,
                    saved.getId(),
                    null,
                    metadata);
        }
        return saved;
    }

    public record UpsertAgencyProfileCommand(
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            String operationsContactEmail,
            String supportContactName,
            String supportContactEmail,
            @NotNull String defaultTimezone,
            @NotNull String defaultLocale) {
    }

    private static String fallback(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
