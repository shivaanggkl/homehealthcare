package com.homehealthcare.agency.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyMfaPolicyMode;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AgencyMfaPolicyService {

    private static final String ACTOR_TYPE_AGENCY_MEMBERSHIP = "AGENCY_MEMBERSHIP";
    private static final String TARGET_TYPE_AGENCY = "AGENCY";
    private static final String ACTION_AGENCY_MFA_POLICY_UPDATED = "AGENCY_MFA_POLICY_UPDATED";

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AgencyRepository agencyRepository;
    private final AuditEventRepository auditEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional(readOnly = true)
    public AgencyMfaPolicyView currentPolicy() {
        AgencyMembership actorMembership = requireActorMembership();
        Agency agency = agencyRepository.findById(actorMembership.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Agency %s was not found".formatted(actorMembership.getAgencyId())));
        return AgencyMfaPolicyView.from(agency);
    }

    @Transactional
    public AgencyMfaPolicyView updatePolicy(@Valid UpdateAgencyMfaPolicyCommand command) {
        AgencyMembership actorMembership = requireActorMembership();
        Agency agency = agencyRepository.findById(actorMembership.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Agency %s was not found".formatted(actorMembership.getAgencyId())));

        applyPolicy(agency, command);

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_AGENCY_MEMBERSHIP,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                ACTION_AGENCY_MFA_POLICY_UPDATED,
                TARGET_TYPE_AGENCY,
                agency.getId(),
                agency.getId(),
                "{\"mode\":\"" + agency.getMfaPolicyMode().name()
                        + "\",\"requiredRoles\":" + toMetadataJsonArray(agency.requiredMfaRoles()) + "}"));

        return AgencyMfaPolicyView.from(agency);
    }

    private AgencyMembership requireActorMembership() {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new UnauthorizedAgencyMfaPolicyActorException(currentTenant.requireMembershipId()));
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_AGENCY_MFA_POLICY,
                UnauthorizedAgencyMfaPolicyActorException::new);
        return actorMembership;
    }

    private void applyPolicy(Agency agency, UpdateAgencyMfaPolicyCommand command) {
        Set<AgencyRole> normalizedRoles = normalizedRoles(command.requiredRoles());
        switch (command.mode()) {
            case OFF -> {
                if (!normalizedRoles.isEmpty()) {
                    throw new InvalidAgencyMfaPolicyConfigurationException(
                            "Required roles must be empty when MFA policy mode is OFF");
                }
                agency.disableMfaRequirement();
            }
            case ALL_USERS -> {
                if (!normalizedRoles.isEmpty()) {
                    throw new InvalidAgencyMfaPolicyConfigurationException(
                            "Required roles must be empty when MFA policy mode is ALL_USERS");
                }
                agency.requireMfaForAllUsers();
            }
            case SELECTED_ROLES -> {
                if (normalizedRoles.isEmpty()) {
                    throw new InvalidAgencyMfaPolicyConfigurationException(
                            "At least one role must be selected when MFA policy mode is SELECTED_ROLES");
                }
                agency.requireMfaForRoles(normalizedRoles);
            }
        }
    }

    private Set<AgencyRole> normalizedRoles(Set<AgencyRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    private static String toMetadataJsonArray(Set<AgencyRole> requiredRoles) {
        return requiredRoles.stream()
                .map(role -> "\"" + role.name() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    public record UpdateAgencyMfaPolicyCommand(
            @NotNull AgencyMfaPolicyMode mode,
            Set<AgencyRole> requiredRoles) {
    }

    public record AgencyMfaPolicyView(
            java.util.UUID agencyId,
            AgencyMfaPolicyMode mode,
            Set<AgencyRole> requiredRoles) {

        private static AgencyMfaPolicyView from(Agency agency) {
            return new AgencyMfaPolicyView(agency.getId(), agency.getMfaPolicyMode(), Set.copyOf(agency.requiredMfaRoles()));
        }
    }
}
