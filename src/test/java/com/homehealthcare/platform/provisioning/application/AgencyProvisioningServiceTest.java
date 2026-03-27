package com.homehealthcare.platform.provisioning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.application.DuplicateAgencySlugException;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.platform.admin.domain.InternalSuperAdmin;
import com.homehealthcare.platform.admin.domain.InternalSuperAdminRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrap;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AgencyProvisioningServiceTest {

    @Autowired
    private InternalSuperAdminRepository internalSuperAdminRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private AgencyOwnerBootstrapRepository agencyOwnerBootstrapRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AgencyProvisioningService agencyProvisioningService;

    @Test
    void provisionsAgencyAndBootstrapsFirstOwnerWithAuditTrail() {
        InternalSuperAdmin admin = internalSuperAdminRepository.saveAndFlush(
                InternalSuperAdmin.create("Platform", "Operator", "ops@platform.example"));

        AgencyProvisioningService.ProvisionedAgencyResult result = agencyProvisioningService.provisionAgency(
                admin,
                new AgencyProvisioningService.ProvisionAgencyCommand(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example",
                        "Alicia",
                        "Owner",
                        "alicia.owner@northstar.example"));

        assertThat(result.agency().getId()).isNotNull();
        assertThat(result.bootstrap().getId()).isNotNull();
        assertThat(agencyRepository.findBySlug("north-star-home-care")).contains(result.agency());

        AgencyOwnerBootstrap bootstrap = agencyOwnerBootstrapRepository
                .findFirstByAgency_IdAndStatusOrderByCreatedAtAsc(
                        result.agency().getId(),
                        AgencyOwnerBootstrapStatus.PENDING)
                .orElseThrow();
        assertThat(bootstrap.getOwnerEmail()).isEqualTo("alicia.owner@northstar.example");

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(admin.getId());
        assertThat(events).hasSize(2);
        assertThat(events).extracting(AuditEvent::getActionType)
                .containsExactly("AGENCY_CREATED", "OWNER_BOOTSTRAPPED");
        assertThat(events).allMatch(event -> event.getAgencyId().equals(result.agency().getId()));
    }

    @Test
    void rejectsInactiveInternalSuperAdmin() {
        InternalSuperAdmin admin = internalSuperAdminRepository.saveAndFlush(
                InternalSuperAdmin.create("Platform", "Operator", "ops@platform.example"));
        admin.deactivate();
        internalSuperAdminRepository.saveAndFlush(admin);

        assertThatThrownBy(() -> agencyProvisioningService.provisionAgency(
                admin,
                new AgencyProvisioningService.ProvisionAgencyCommand(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example",
                        "Alicia",
                        "Owner",
                        "alicia.owner@northstar.example")))
                .isInstanceOf(InactiveInternalSuperAdminException.class);
    }

    @Test
    void rejectsDuplicateAgencySlugWithinProvisioningFlow() {
        InternalSuperAdmin admin = internalSuperAdminRepository.saveAndFlush(
                InternalSuperAdmin.create("Platform", "Operator", "ops@platform.example"));

        agencyProvisioningService.provisionAgency(
                admin,
                new AgencyProvisioningService.ProvisionAgencyCommand(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example",
                        "Alicia",
                        "Owner",
                        "alicia.owner@northstar.example"));

        assertThatThrownBy(() -> agencyProvisioningService.provisionAgency(
                admin,
                new AgencyProvisioningService.ProvisionAgencyCommand(
                        "North Star East",
                        "north-star-home-care",
                        "America/New_York",
                        "ops@east.example",
                        "Evan",
                        "Owner",
                        "evan.owner@east.example")))
                .isInstanceOf(DuplicateAgencySlugException.class);
    }
}
