package com.homehealthcare.configuration.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.alertrule.application.AlertRuleCatalogService;
import com.homehealthcare.alertrule.domain.AlertRuleType;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchpolicy.application.BranchPolicyCatalogService;
import com.homehealthcare.documentationtemplate.application.DocumentationTemplateCatalogService;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mileagepay.application.MileagePaySettingService;
import com.homehealthcare.mileagepay.domain.MileageReimbursementStrategy;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.tasktemplate.application.TaskTemplateCatalogService;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class PhaseCConfigurationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private TaskTemplateCatalogService taskTemplateCatalogService;

    @Autowired
    private DocumentationTemplateCatalogService documentationTemplateCatalogService;

    @Autowired
    private BranchPolicyCatalogService branchPolicyCatalogService;

    @Autowired
    private AlertRuleCatalogService alertRuleCatalogService;

    @Autowired
    private MileagePaySettingService mileagePaySettingService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void phaseCServicesPersistSettingsAndAuditThem() {
        AgencyMembership actorMembership = persistOwnerMembership();
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(actorMembership.getAgency(), "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        taskTemplateCatalogService.create(
                actorMembership,
                new TaskTemplateCatalogService.ManageTaskTemplateCommand(
                        null,
                        null,
                        "Medication Reminder",
                        "MED-REM",
                        "Clinical follow-up task",
                        TaskTemplateCategory.CLINICAL,
                        1));

        DocumentationTemplate template = documentationTemplateCatalogService.createDraft(
                actorMembership,
                new DocumentationTemplateCatalogService.ManageDocumentationTemplateCommand(
                        "Visit Note",
                        "VN-1",
                        DocumentationTemplateType.VISIT_NOTE,
                        "{\"sections\":[\"subjective\",\"objective\"]}",
                        1));
        DocumentationTemplate updated = documentationTemplateCatalogService.updateDraft(
                actorMembership,
                template.getId(),
                new DocumentationTemplateCatalogService.ManageDocumentationTemplateCommand(
                        "Visit Note",
                        "VN-1",
                        DocumentationTemplateType.VISIT_NOTE,
                        "{\"sections\":[\"subjective\",\"objective\",\"plan\"]}",
                        1));
        documentationTemplateCatalogService.publish(actorMembership, updated.getId());

        branchPolicyCatalogService.create(
                actorMembership,
                new BranchPolicyCatalogService.ManageBranchPolicyCommand(
                        branch.getId(),
                        "scheduling.window",
                        "{\"minutes\":30}",
                        false,
                        1,
                        OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                        null));

        branchPolicyCatalogService.create(
                actorMembership,
                new BranchPolicyCatalogService.ManageBranchPolicyCommand(
                        branch.getId(),
                        "care.plan.default",
                        null,
                        true,
                        2,
                        null,
                        null));

        alertRuleCatalogService.create(
                actorMembership,
                new AlertRuleCatalogService.ManageAlertRuleCommand(
                        branch.getId(),
                        "Late Arrival Alert",
                        AlertRuleType.LATE_ARRIVAL,
                        "{\"minutesLate\":15}",
                        true,
                        false,
                        true,
                        1));

        mileagePaySettingService.create(
                actorMembership,
                new MileagePaySettingService.ManageMileagePaySettingCommand(
                        null,
                        MileageReimbursementStrategy.STANDARD_RATE,
                        new BigDecimal("0.6700"),
                        true,
                        "{\"VN-1\":{\"perVisit\":5.00}}",
                        1,
                        OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                        null));
        mileagePaySettingService.create(
                actorMembership,
                new MileagePaySettingService.ManageMileagePaySettingCommand(
                        branch.getId(),
                        MileageReimbursementStrategy.CUSTOM_RATE,
                        new BigDecimal("0.7200"),
                        false,
                        null,
                        2,
                        null,
                        null));

        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(branchPolicyCatalogService.resolveEffectiveSettings(
                        branch.getId(),
                        "scheduling.window",
                        OffsetDateTime.parse("2026-06-01T00:00:00Z")))
                .contains("{\"minutes\":30}");
        assertThat(branchPolicyCatalogService.resolveEffectiveSettings(
                        branch.getId(),
                        "care.plan.default",
                        OffsetDateTime.now()))
                .isEmpty();

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId());
        assertThat(events)
                .extracting(AuditEvent::getTargetType)
                .contains(
                        "TASK_TEMPLATE",
                        "DOCUMENTATION_TEMPLATE",
                        "BRANCH_POLICY",
                        "ALERT_RULE",
                        "MILEAGE_PAY_SETTING");
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains("CONFIGURATION_CREATED", "CONFIGURATION_UPDATED", "CONFIGURATION_PUBLISHED");
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
