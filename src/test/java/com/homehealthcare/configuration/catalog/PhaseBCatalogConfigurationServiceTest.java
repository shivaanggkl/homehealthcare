package com.homehealthcare.configuration.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.agencyprofile.application.AgencyProfileService;
import com.homehealthcare.caregiverskill.application.CaregiverSkillCatalogService;
import com.homehealthcare.certification.application.CaregiverCertificationCatalogService;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationAuditAction;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.application.ServiceLineCatalogService;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.application.VisitTypeCatalogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhaseBCatalogConfigurationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AgencyProfileService agencyProfileService;

    @Autowired
    private ServiceLineCatalogService serviceLineCatalogService;

    @Autowired
    private VisitTypeCatalogService visitTypeCatalogService;

    @Autowired
    private CaregiverSkillCatalogService caregiverSkillCatalogService;

    @Autowired
    private CaregiverCertificationCatalogService caregiverCertificationCatalogService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void phaseBCatalogServicesPersistChangesAndAuditThem() {
        AgencyMembership actorMembership = persistOwnerMembership();

        agencyProfileService.upsertProfile(actorMembership, new AgencyProfileService.UpsertAgencyProfileCommand(
                "North Star Home Care",
                "North Star Holdings LLC",
                "312-555-0101",
                "123 Main St, Chicago, IL 60601",
                "Operations Lead",
                "operations@northstar.example",
                "Support Desk",
                "support@northstar.example",
                "America/Chicago",
                "en-US"));

        ServiceLine serviceLine = serviceLineCatalogService.create(
                actorMembership,
                new ServiceLineCatalogService.ManageServiceLineCommand(
                        "Private Duty",
                        "PD",
                        "Companion and personal care",
                        1));

        visitTypeCatalogService.create(
                actorMembership,
                new VisitTypeCatalogService.ManageVisitTypeCommand(
                        serviceLine.getId(),
                        "Private Duty Standard Visit",
                        "PD-STD",
                        "Standard private duty visit",
                        60,
                        true,
                        2));

        caregiverSkillCatalogService.create(
                actorMembership,
                new CaregiverSkillCatalogService.ManageCaregiverSkillCommand(
                        "Dementia Care",
                        "DEM",
                        "Memory care skill"));

        caregiverCertificationCatalogService.create(
                actorMembership,
                new CaregiverCertificationCatalogService.ManageCaregiverCertificationCommand(
                        "CPR",
                        "CPR",
                        "Cardiopulmonary resuscitation",
                        true));

        serviceLineCatalogService.deactivate(actorMembership, serviceLine.getId());

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId());
        assertThat(events)
                .extracting(AuditEvent::getTargetType)
                .contains(
                        Epic2ConfigurationTargetType.AGENCY_PROFILE.name(),
                        Epic2ConfigurationTargetType.SERVICE_LINE.name(),
                        Epic2ConfigurationTargetType.VISIT_TYPE.name(),
                        Epic2ConfigurationTargetType.CAREGIVER_SKILL.name(),
                        Epic2ConfigurationTargetType.CAREGIVER_CERTIFICATION.name());
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic2ConfigurationAuditAction.CREATED.actionType(),
                        Epic2ConfigurationAuditAction.DEACTIVATED.actionType());
    }

    private AgencyMembership persistOwnerMembership() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        User owner = userRepository.saveAndFlush(
                User.invite("Agency", "Owner", "owner@northstar.example", "312-555-0199"));
        owner.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(owner);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(owner, agency, AgencyRole.AGENCY_OWNER));
    }
}
