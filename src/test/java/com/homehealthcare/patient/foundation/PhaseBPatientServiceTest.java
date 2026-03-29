package com.homehealthcare.patient.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.application.PatientRecordService;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.application.PatientAddressService;
import com.homehealthcare.patientcontact.application.PatientContactService;
import com.homehealthcare.patientdiagnosis.application.PatientDiagnosisService;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
import com.homehealthcare.patienteligibility.application.PatientServiceEligibilityService;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.application.ServiceLineCatalogService;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PhaseBPatientServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientRecordService patientRecordService;

    @Autowired
    private PatientContactService patientContactService;

    @Autowired
    private PatientAddressService patientAddressService;

    @Autowired
    private PatientServiceEligibilityService patientServiceEligibilityService;

    @Autowired
    private PatientDiagnosisService patientDiagnosisService;

    @Autowired
    private ServiceLineCatalogService serviceLineCatalogService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void phaseBPatientServicesPersistChangesAndAuditThem() {
        AgencyMembership actorMembership = persistOwnerMembership();

        Patient patient = patientRecordService.create(actorMembership, new PatientRecordService.ManagePatientCommand(
                "PAT-001",
                "Shiva",
                null,
                "Kl",
                "Shiv",
                LocalDate.of(1990, 1, 15),
                "female",
                "312-555-0101",
                null,
                "shiva@example.com",
                "en-US",
                "Prefers weekday mornings"));

        patientContactService.create(actorMembership, patient.getId(), new PatientContactService.ManagePatientContactCommand(
                "Sister",
                "Neha Kl",
                "312-555-0102",
                "neha@example.com",
                "123 Main St",
                true,
                true,
                false,
                "Emergency contact"));

        patientAddressService.upsert(actorMembership, patient.getId(), new PatientAddressService.ManagePatientAddressCommand(
                "123 Main St",
                "Unit 4",
                "Chicago",
                "IL",
                "60601",
                "US",
                new BigDecimal("41.881832"),
                new BigDecimal("-87.623177"),
                "GEOCODED",
                "America/Chicago",
                "Buzz apartment before entering"));

        patientAddressService.upsert(actorMembership, patient.getId(), new PatientAddressService.ManagePatientAddressCommand(
                "123 Main St",
                "Unit 5",
                "Chicago",
                "IL",
                "60601",
                "US",
                new BigDecimal("41.881832"),
                new BigDecimal("-87.623177"),
                "GEOCODED",
                "America/Chicago",
                "Use rear elevator"));

        ServiceLine serviceLine = serviceLineCatalogService.create(
                actorMembership,
                new ServiceLineCatalogService.ManageServiceLineCommand(
                        "Private Duty",
                        "PD",
                        "Companion and personal care",
                        1));

        patientServiceEligibilityService.create(actorMembership, patient.getId(), new PatientServiceEligibilityService.ManagePatientServiceEligibilityCommand(
                serviceLine.getId(),
                PatientServiceEligibilityStatus.ELIGIBLE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "Intake review",
                "Approved for private duty"));

        patientDiagnosisService.create(actorMembership, patient.getId(), new PatientDiagnosisService.ManagePatientDiagnosisCommand(
                "I10",
                "Hypertension",
                "Chronic",
                true,
                LocalDate.of(2025, 1, 1),
                null,
                PatientDiagnosisStatus.ACTIVE,
                "Monitor blood pressure"));

        patientRecordService.deactivate(actorMembership, patient.getId());

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId());
        assertThat(events)
                .extracting(AuditEvent::getTargetType)
                .contains(
                        Epic3PatientTargetType.PATIENT.name(),
                        Epic3PatientTargetType.PATIENT_CONTACT.name(),
                        Epic3PatientTargetType.PATIENT_ADDRESS.name(),
                        Epic3PatientTargetType.PATIENT_SERVICE_ELIGIBILITY.name(),
                        Epic3PatientTargetType.PATIENT_DIAGNOSIS.name());
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic3PatientAuditAction.CREATED.actionType(),
                        Epic3PatientAuditAction.UPDATED.actionType(),
                        Epic3PatientAuditAction.DEACTIVATED.actionType());

        Patient savedPatient = patientRepository.findById(patient.getId()).orElseThrow();
        assertThat(savedPatient.getStatus()).isEqualTo(com.homehealthcare.patient.foundation.PatientLifecycleStatus.INACTIVE);
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
