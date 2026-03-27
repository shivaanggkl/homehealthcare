package com.homehealthcare.platform.provisioning.application;

import com.homehealthcare.agency.application.DuplicateAgencySlugException;
import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.platform.admin.domain.InternalSuperAdmin;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrap;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AgencyProvisioningService {

    private static final String ACTOR_TYPE_INTERNAL_SUPER_ADMIN = "INTERNAL_SUPER_ADMIN";
    private static final String ACTION_AGENCY_CREATED = "AGENCY_CREATED";
    private static final String ACTION_OWNER_BOOTSTRAPPED = "OWNER_BOOTSTRAPPED";
    private static final String TARGET_TYPE_AGENCY = "AGENCY";
    private static final String TARGET_TYPE_AGENCY_OWNER_BOOTSTRAP = "AGENCY_OWNER_BOOTSTRAP";

    private final AgencyRepository agencyRepository;
    private final AgencyOwnerBootstrapRepository agencyOwnerBootstrapRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public ProvisionedAgencyResult provisionAgency(
            @NotNull InternalSuperAdmin actor,
            @Valid ProvisionAgencyCommand command) {
        requireActiveActor(actor);

        String normalizedSlug = command.slug().trim().toLowerCase(Locale.ROOT);
        if (agencyRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateAgencySlugException(normalizedSlug);
        }

        Agency agency = agencyRepository.save(Agency.create(
                command.agencyName(),
                normalizedSlug,
                command.timezone(),
                command.contactEmail()));

        AgencyOwnerBootstrap bootstrap = agencyOwnerBootstrapRepository.save(
                AgencyOwnerBootstrap.create(
                        agency,
                        command.ownerFirstName(),
                        command.ownerLastName(),
                        command.ownerEmail()));

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_INTERNAL_SUPER_ADMIN,
                actor.getId(),
                actor.getEmail(),
                ACTION_AGENCY_CREATED,
                TARGET_TYPE_AGENCY,
                agency.getId(),
                agency.getId(),
                "{\"slug\":\"" + agency.getSlug() + "\",\"status\":\"" + agency.getStatus().name() + "\"}"));

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_INTERNAL_SUPER_ADMIN,
                actor.getId(),
                actor.getEmail(),
                ACTION_OWNER_BOOTSTRAPPED,
                TARGET_TYPE_AGENCY_OWNER_BOOTSTRAP,
                bootstrap.getId(),
                agency.getId(),
                "{\"ownerEmail\":\"" + bootstrap.getOwnerEmail() + "\",\"bootstrapStatus\":\"" + bootstrap.getStatus().name() + "\"}"));

        return new ProvisionedAgencyResult(agency, bootstrap);
    }

    private static void requireActiveActor(InternalSuperAdmin actor) {
        if (!actor.isActive()) {
            throw new InactiveInternalSuperAdminException(actor.getId());
        }
    }

    public record ProvisionAgencyCommand(
            @NotBlank String agencyName,
            @NotBlank String slug,
            @NotBlank String timezone,
            @NotBlank @Email String contactEmail,
            @NotBlank String ownerFirstName,
            @NotBlank String ownerLastName,
            @NotBlank @Email String ownerEmail) {
    }

    public record ProvisionedAgencyResult(
            Agency agency,
            AgencyOwnerBootstrap bootstrap) {
    }
}
