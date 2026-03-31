package com.homehealthcare.agencyprofile.api;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.agencyprofile.application.AgencyProfileService;
import com.homehealthcare.agencyprofile.domain.AgencyProfile;
import com.homehealthcare.agencyprofile.domain.AgencyProfileRepository;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agency/profile")
class AgencyProfileController {

    private final AgencyRepository agencyRepository;
    private final AgencyProfileRepository agencyProfileRepository;
    private final AgencyProfileService agencyProfileService;
    private final CurrentTenant currentTenant;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    AgencyProfileController(
            AgencyRepository agencyRepository,
            AgencyProfileRepository agencyProfileRepository,
            AgencyProfileService agencyProfileService,
            CurrentTenant currentTenant,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.agencyRepository = agencyRepository;
        this.agencyProfileRepository = agencyProfileRepository;
        this.agencyProfileService = agencyProfileService;
        this.currentTenant = currentTenant;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    AgencyProfileResponse profile() {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        Agency agency = agencyRepository.findById(currentTenant.requireAgencyId())
                .orElseThrow(() -> new IllegalStateException("Agency was not found"));
        AgencyProfile profile = agencyProfileRepository.findByAgency_Id(currentTenant.requireAgencyId())
                .orElseGet(() -> AgencyProfile.create(
                        agency,
                        agency.getName(),
                        null,
                        null,
                        null,
                        agency.getName(),
                        agency.getContactEmail(),
                        null,
                        null,
                        agency.getTimezone(),
                        "en-US"));
        return toResponse(profile);
    }

    @PutMapping
    AgencyProfileResponse update(@Valid @RequestBody UpdateAgencyProfileRequest request) {
        AgencyProfile saved = agencyProfileService.upsertProfile(
                configurationActorResolver.requireActorMembership(),
                new AgencyProfileService.UpsertAgencyProfileCommand(
                        request.displayName(),
                        request.legalName(),
                        request.primaryPhone(),
                        request.primaryAddress(),
                        request.operationsContactName(),
                        request.operationsContactEmail(),
                        request.supportContactName(),
                        request.supportContactEmail(),
                        request.defaultTimezone(),
                        request.defaultLocale()));
        return toResponse(saved);
    }

    private static AgencyProfileResponse toResponse(AgencyProfile profile) {
        return new AgencyProfileResponse(
                profile.getId(),
                profile.getAgencyId(),
                profile.getDisplayName(),
                profile.getLegalName(),
                profile.getPrimaryPhone(),
                profile.getPrimaryAddress(),
                profile.getOperationsContactName(),
                profile.getOperationsContactEmail(),
                profile.getSupportContactName(),
                profile.getSupportContactEmail(),
                profile.getDefaultTimezone(),
                profile.getDefaultLocale(),
                profile.getStatus());
    }

    record UpdateAgencyProfileRequest(
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            @Email String operationsContactEmail,
            String supportContactName,
            @Email String supportContactEmail,
            @NotBlank String defaultTimezone,
            @NotBlank String defaultLocale) {
    }

    record AgencyProfileResponse(
            UUID id,
            UUID agencyId,
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            String operationsContactEmail,
            String supportContactName,
            String supportContactEmail,
            String defaultTimezone,
            String defaultLocale,
            com.homehealthcare.configuration.foundation.ConfigurationStatus status) {
    }
}
