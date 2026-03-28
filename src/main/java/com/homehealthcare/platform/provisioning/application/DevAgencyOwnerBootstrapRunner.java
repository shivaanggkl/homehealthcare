package com.homehealthcare.platform.provisioning.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.admin.domain.InternalSuperAdmin;
import com.homehealthcare.platform.admin.domain.InternalSuperAdminRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrap;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapStatus;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev-bootstrap.owner", name = "enabled", havingValue = "true")
public class DevAgencyOwnerBootstrapRunner implements ApplicationRunner {

    private final DevAgencyOwnerBootstrapProperties properties;
    private final InternalSuperAdminRepository internalSuperAdminRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyProvisioningService agencyProvisioningService;
    private final AgencyOwnerBootstrapRepository agencyOwnerBootstrapRepository;
    private final UserRepository userRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateRequiredConfiguration();

        InternalSuperAdmin superAdmin = internalSuperAdminRepository.findByEmail(normalized(properties.getInternalSuperAdminEmail()))
                .orElseGet(() -> internalSuperAdminRepository.save(InternalSuperAdmin.create(
                        properties.getInternalSuperAdminFirstName(),
                        properties.getInternalSuperAdminLastName(),
                        properties.getInternalSuperAdminEmail())));

        Agency agency = agencyRepository.findBySlug(normalizedSlug(properties.getAgencySlug()))
                .orElseGet(() -> agencyProvisioningService.provisionAgency(
                        superAdmin,
                        new AgencyProvisioningService.ProvisionAgencyCommand(
                                properties.getAgencyName(),
                                properties.getAgencySlug(),
                                properties.getAgencyTimezone(),
                                properties.getAgencyContactEmail(),
                                properties.getOwnerFirstName(),
                                properties.getOwnerLastName(),
                                properties.getOwnerEmail()))
                        .agency());

        User owner = userRepository.findByEmail(normalized(properties.getOwnerEmail()))
                .orElseGet(() -> userRepository.save(User.invite(
                        properties.getOwnerFirstName(),
                        properties.getOwnerLastName(),
                        properties.getOwnerEmail(),
                        properties.getOwnerPhone())));

        syncOwnerUser(owner);
        syncOwnerMembership(owner, agency);
        completePendingBootstrap(agency, owner.getEmail());

        log.info(
                "Dev bootstrap owner ready for agency '{}' (slug='{}'). Login email='{}' password='{}'.",
                agency.getName(),
                agency.getSlug(),
                owner.getEmail(),
                properties.getOwnerPassword());
    }

    private void syncOwnerUser(User owner) {
        owner.updateProfile(
                properties.getOwnerFirstName(),
                properties.getOwnerLastName(),
                properties.getOwnerPhone());
        if (owner.getStatus() != UserStatus.ACTIVE || !owner.hasPasswordHash()) {
            owner.activateWithCredentials(passwordEncoder.encode(properties.getOwnerPassword()));
        }
        userRepository.save(owner);
    }

    private void syncOwnerMembership(User owner, Agency agency) {
        AgencyMembership membership = agencyMembershipRepository.findByUser_IdAndAgency_Id(owner.getId(), agency.getId())
                .orElseGet(() -> agencyMembershipRepository.save(AgencyMembership.grant(owner, agency, AgencyRole.AGENCY_OWNER)));

        boolean changed = false;
        if (membership.getRole() != AgencyRole.AGENCY_OWNER) {
            membership.changeRole(AgencyRole.AGENCY_OWNER);
            changed = true;
        }
        if (!membership.isActive()) {
            membership.activate();
            changed = true;
        }
        if (changed) {
            agencyMembershipRepository.save(membership);
        }
    }

    private void completePendingBootstrap(Agency agency, String ownerEmail) {
        agencyOwnerBootstrapRepository
                .findFirstByAgency_IdAndStatusOrderByCreatedAtAsc(agency.getId(), AgencyOwnerBootstrapStatus.PENDING)
                .filter(bootstrap -> bootstrap.getOwnerEmail().equals(normalized(ownerEmail)))
                .ifPresent(this::markBootstrapCompleted);
    }

    private void markBootstrapCompleted(AgencyOwnerBootstrap bootstrap) {
        bootstrap.complete();
        agencyOwnerBootstrapRepository.save(bootstrap);
    }

    private void validateRequiredConfiguration() {
        requirePresent("app.dev-bootstrap.owner.internal-super-admin-email", properties.getInternalSuperAdminEmail());
        requirePresent("app.dev-bootstrap.owner.agency-name", properties.getAgencyName());
        requirePresent("app.dev-bootstrap.owner.agency-slug", properties.getAgencySlug());
        requirePresent("app.dev-bootstrap.owner.agency-timezone", properties.getAgencyTimezone());
        requirePresent("app.dev-bootstrap.owner.agency-contact-email", properties.getAgencyContactEmail());
        requirePresent("app.dev-bootstrap.owner.owner-first-name", properties.getOwnerFirstName());
        requirePresent("app.dev-bootstrap.owner.owner-last-name", properties.getOwnerLastName());
        requirePresent("app.dev-bootstrap.owner.owner-email", properties.getOwnerEmail());
        requirePresent("app.dev-bootstrap.owner.owner-password", properties.getOwnerPassword());
    }

    private static void requirePresent(String propertyName, String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalStateException("Missing required dev bootstrap property: " + propertyName);
        }
    }

    private static String normalized(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizedSlug(String value) {
        return normalized(value);
    }
}
