package com.homehealthcare.agency.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AgencySettingsService {

    private static final String ACTION_AGENCY_SETTINGS_UPDATED = "AGENCY_SETTINGS_UPDATED";

    private final CurrentTenant currentTenant;
    private final AgencyRepository agencyRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional(readOnly = true)
    public AgencySettingsView getSettings() {
        AgencyMembership actorMembership = currentMembership();
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_AGENCY_SETTINGS,
                UnauthorizedAgencySettingsActorException::new);

        Agency agency = agencyRepository.findById(actorMembership.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Current agency was not found"));
        return toView(agency);
    }

    @Transactional
    public AgencySettingsView updateSettings(@Valid UpdateAgencySettingsCommand command) {
        AgencyMembership actorMembership = currentMembership();
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_SETTINGS,
                UnauthorizedAgencySettingsActorException::new);

        Agency agency = agencyRepository.findById(actorMembership.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Current agency was not found"));
        agency.rename(command.name());
        agency.updateTimezone(command.timezone());
        agency.updateContactEmail(command.contactEmail());
        Agency savedAgency = agencyRepository.saveAndFlush(agency);

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                ACTION_AGENCY_SETTINGS_UPDATED,
                "AGENCY",
                savedAgency.getId(),
                savedAgency.getId(),
                null,
                "{\"name\":\"" + savedAgency.getName()
                        + "\",\"timezone\":\"" + savedAgency.getTimezone()
                        + "\",\"contactEmail\":\"" + savedAgency.getContactEmail() + "\"}"));

        return toView(savedAgency);
    }

    private AgencyMembership currentMembership() {
        return agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
    }

    private static AgencySettingsView toView(Agency agency) {
        return new AgencySettingsView(
                agency.getId(),
                agency.getName(),
                agency.getSlug(),
                agency.getTimezone(),
                agency.getContactEmail(),
                agency.getStatus());
    }

    public record UpdateAgencySettingsCommand(
            @NotBlank String name,
            @NotBlank String timezone,
            @NotBlank @Email String contactEmail) {
    }

    public record AgencySettingsView(
            java.util.UUID agencyId,
            String name,
            String slug,
            String timezone,
            String contactEmail,
            com.homehealthcare.agency.domain.AgencyStatus status) {
    }
}
