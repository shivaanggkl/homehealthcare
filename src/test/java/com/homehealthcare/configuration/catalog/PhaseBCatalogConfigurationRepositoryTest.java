package com.homehealthcare.configuration.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.agencyprofile.domain.AgencyProfile;
import com.homehealthcare.agencyprofile.domain.AgencyProfileRepository;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.security.tenant.TenantContext;
import com.homehealthcare.security.tenant.TenantContextHolder;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.util.Locale;
import java.util.UUID;
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
class PhaseBCatalogConfigurationRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private AgencyProfileRepository agencyProfileRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverCertificationRepository caregiverCertificationRepository;

    @Test
    void agencyProfileIsUniquePerAgencyAndNormalizesDefaults() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        AgencyProfile profile = agencyProfileRepository.saveAndFlush(AgencyProfile.create(
                agency,
                " North Star ",
                " North Star Holdings LLC ",
                " 312-555-0101 ",
                " 123 Main St ",
                " Ops Lead ",
                " OPS@NORTHSTAR.EXAMPLE ",
                " Support Desk ",
                " HELP@NORTHSTAR.EXAMPLE ",
                "America/Chicago",
                "en-us"));

        assertThat(profile.getDisplayName()).isEqualTo("North Star");
        assertThat(profile.getOperationsContactEmail()).isEqualTo("ops@northstar.example");
        assertThat(profile.getSupportContactEmail()).isEqualTo("help@northstar.example");
        assertThat(profile.getDefaultLocale()).isEqualTo(Locale.forLanguageTag("en-us").toLanguageTag());

        AgencyProfile duplicate = AgencyProfile.create(
                agency,
                "North Star Duplicate",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "America/Chicago",
                "en-US");

        assertThatThrownBy(() -> agencyProfileRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void serviceLineEnforcesAgencyScopedUniquenessLifecycleAndTenantFindById() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));

        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyOne, "Private Duty", "pd", "Companion and personal care", 1));

        ServiceLine sameCodeOtherAgency = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyTwo, "Private Duty", "PD", "Allowed in another agency", 1));
        assertThat(sameCodeOtherAgency.getAgencyId()).isEqualTo(agencyTwo.getId());

        serviceLine.deactivate();
        ServiceLine inactive = serviceLineRepository.saveAndFlush(serviceLine);
        assertThat(inactive.getStatus().name()).isEqualTo("INACTIVE");

        TenantContextHolder.set(new TenantContext(agencyOne.getId(), UUID.randomUUID()));
        try {
            assertThat(serviceLineRepository.findById(serviceLine.getId())).contains(inactive);
            assertThat(serviceLineRepository.findById(sameCodeOtherAgency.getId())).isEmpty();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void serviceLineRejectsDuplicateCodeWithinAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        serviceLineRepository.saveAndFlush(
                ServiceLine.create(agency, "Private Duty", "PD", "Companion and personal care", 1));

        ServiceLine duplicateCode = ServiceLine.create(agency, "Skilled Nursing", "PD", "Duplicate code", 2);
        assertThatThrownBy(() -> serviceLineRepository.saveAndFlush(duplicateCode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void visitTypeCanReferenceAgencyServiceLineAndRejectsCrossAgencyReference() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyOne, "Skilled Nursing", "SN", "Skilled services", 1));
        ServiceLine foreignServiceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyTwo, "Therapy", "TH", "Other agency", 1));

        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(
                agencyOne,
                serviceLine,
                "Skilled Nursing Follow-up",
                "sn-follow",
                "Routine follow-up visit",
                60,
                true,
                3));

        assertThat(visitType.getServiceLine()).isNotNull();
        assertThat(visitType.getServiceLine().getId()).isEqualTo(serviceLine.getId());
        assertThat(visitType.getCode()).isEqualTo("SN-FOLLOW");

        assertThatThrownBy(() -> VisitType.create(
                        agencyOne,
                        foreignServiceLine,
                        "Bad Cross Reference",
                        "bad-cross",
                        "Should fail",
                        45,
                        false,
                        0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceLine must belong to the same agency as the visit type");
    }

    @Test
    void caregiverSkillAndCertificationSupportAgencyScopedCatalogLifecycle() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        CaregiverSkill skill = caregiverSkillRepository.saveAndFlush(
                CaregiverSkill.create(agency, "Wound Care", "wc", "Clinical wound care skill"));
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agency, "CPR", "cpr", "Cardiopulmonary resuscitation", true));

        assertThat(skill.getCode()).isEqualTo("WC");
        assertThat(certification.getCode()).isEqualTo("CPR");
        assertThat(certification.isExpirationRequired()).isTrue();

        skill.deactivate();
        certification.deactivate();
        caregiverSkillRepository.saveAndFlush(skill);
        caregiverCertificationRepository.saveAndFlush(certification);

        assertThat(skill.getStatus().name()).isEqualTo("INACTIVE");
        assertThat(certification.getStatus().name()).isEqualTo("INACTIVE");

        assertThatThrownBy(() -> caregiverSkillRepository.saveAndFlush(
                        CaregiverSkill.create(agency, "Wound Care", "WC-2", "Duplicate name")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> caregiverCertificationRepository.saveAndFlush(
                        CaregiverCertification.create(agency, "CPR Renewal", "CPR", "Duplicate code", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
