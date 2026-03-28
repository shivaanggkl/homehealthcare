package com.homehealthcare.configuration.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchpolicy.domain.BranchPolicy;
import com.homehealthcare.branchpolicy.domain.BranchPolicyRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.mileagepay.domain.MileagePaySetting;
import com.homehealthcare.mileagepay.domain.MileagePaySettingRepository;
import com.homehealthcare.mileagepay.domain.MileageReimbursementStrategy;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhaseCConfigurationRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private DocumentationTemplateRepository documentationTemplateRepository;

    @Autowired
    private BranchPolicyRepository branchPolicyRepository;

    @Autowired
    private MileagePaySettingRepository mileagePaySettingRepository;

    @Test
    void taskTemplateSupportsOptionalLinksAndRejectsCrossAgencyReferences() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));

        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyOne, "Private Duty", "PD", "Private duty", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(
                VisitType.create(agencyOne, serviceLine, "Private Duty Visit", "PDV", "Visit", 60, true, 1));
        ServiceLine foreignServiceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyTwo, "Skilled Nursing", "SN", "Other agency", 1));

        TaskTemplate template = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agencyOne,
                serviceLine,
                visitType,
                "Medication Reminder",
                "med-rem",
                "Medication reminder task",
                TaskTemplateCategory.CLINICAL,
                1));

        assertThat(template.getCode()).isEqualTo("MED-REM");
        assertThat(template.getVisitType().getId()).isEqualTo(visitType.getId());

        assertThatThrownBy(() -> TaskTemplate.create(
                        agencyOne,
                        foreignServiceLine,
                        null,
                        "Bad Link",
                        "bad-link",
                        "Should fail",
                        TaskTemplateCategory.OPERATIONAL,
                        0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceLine must belong to the same agency as the task template");
    }

    @Test
    void taskTemplatesEnforceAgencyScopedUniqueness() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agency,
                null,
                null,
                "Medication Reminder",
                "MED",
                "Task",
                TaskTemplateCategory.CLINICAL,
                1));

        assertThatThrownBy(() -> taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                        agency,
                        null,
                        null,
                        "Medication Reminder",
                        "MED-2",
                        "Duplicate name",
                        TaskTemplateCategory.CLINICAL,
                        2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void documentationTemplatesEnforceAgencyScopedUniqueness() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-2", "America/Chicago", "ops2@northstar.example"));

        documentationTemplateRepository.saveAndFlush(DocumentationTemplate.createDraft(
                agency,
                "Visit Note",
                "VN-1",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"sections\":[]}",
                1));

        assertThatThrownBy(() -> documentationTemplateRepository.saveAndFlush(DocumentationTemplate.createDraft(
                        agency,
                        "Another Name",
                        "VN-1",
                        DocumentationTemplateType.VISIT_NOTE,
                        "{\"sections\":[]}",
                        1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void branchPolicyAndMileagePaySupportScopedPersistenceAndFallbackFields() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        BranchPolicy policy = branchPolicyRepository.saveAndFlush(BranchPolicy.create(
                branch,
                "scheduling.window",
                "{\"minutes\":30}",
                false,
                1));
        policy.assignEffectiveWindow(
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-12-31T23:59:59Z"));
        branchPolicyRepository.saveAndFlush(policy);

        assertThat(policy.effectiveSettingsPayloadJson()).isEqualTo("{\"minutes\":30}");
        assertThat(policy.isEffectiveAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"))).isTrue();

        MileagePaySetting defaultSetting = mileagePaySettingRepository.saveAndFlush(MileagePaySetting.create(
                agency,
                null,
                MileageReimbursementStrategy.STANDARD_RATE,
                new BigDecimal("0.6700"),
                true,
                "{\"PDV\":{\"hourly\":2.50}}",
                1));
        MileagePaySetting branchOverride = mileagePaySettingRepository.saveAndFlush(MileagePaySetting.create(
                agency,
                branch,
                MileageReimbursementStrategy.CUSTOM_RATE,
                new BigDecimal("0.7200"),
                false,
                null,
                2));

        assertThat(defaultSetting.isBranchOverride()).isFalse();
        assertThat(branchOverride.isBranchOverride()).isTrue();
    }
}
