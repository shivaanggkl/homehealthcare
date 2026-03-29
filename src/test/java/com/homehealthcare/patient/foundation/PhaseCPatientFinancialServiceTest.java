package com.homehealthcare.patient.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.application.LikelyDuplicatePatientException;
import com.homehealthcare.patient.application.PatientConflictException;
import com.homehealthcare.patient.application.PatientRecordService;
import com.homehealthcare.patient.application.InvalidPatientStateTransitionException;
import com.homehealthcare.patientauthorization.application.PatientEpisodeAuthorizationService;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientpayer.application.PatientPayerLinkService;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.application.ServiceLineCatalogService;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
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
class PhaseCPatientFinancialServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private PatientRecordService patientRecordService;

    @Autowired
    private PatientPayerLinkService patientPayerLinkService;

    @Autowired
    private PatientEpisodeAuthorizationService patientEpisodeAuthorizationService;

    @Autowired
    private ServiceLineCatalogService serviceLineCatalogService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void phaseCPatientServicesEnforceDuplicateAndDateWindowConflictsAndAudit() {
        AgencyMembership branchAdmin = persistMembership(AgencyRole.BRANCH_ADMIN);

        Patient patient = patientRecordService.create(branchAdmin, new PatientRecordService.ManagePatientCommand(
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

        assertThatThrownBy(() -> patientRecordService.create(branchAdmin, new PatientRecordService.ManagePatientCommand(
                "PAT-002",
                "Shiva",
                null,
                "Kl",
                null,
                LocalDate.of(1990, 1, 15),
                null,
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(LikelyDuplicatePatientException.class);

        PatientPayerLink payerLink = patientPayerLinkService.create(branchAdmin, patient.getId(), new PatientPayerLinkService.ManagePatientPayerLinkCommand(
                "Blue Cross",
                null,
                "POL-001",
                "GRP-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true,
                PatientPayerLinkStatus.ACTIVE,
                "Primary commercial payer"));

        assertThatThrownBy(() -> patientPayerLinkService.create(branchAdmin, patient.getId(), new PatientPayerLinkService.ManagePatientPayerLinkCommand(
                "Blue Cross",
                null,
                "POL-001",
                "GRP-1",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31),
                false,
                PatientPayerLinkStatus.ACTIVE,
                "Overlapping coverage")))
                .isInstanceOf(PatientConflictException.class)
                .hasMessage("Overlapping payer coverage exists for the same patient and payer identity");

        ServiceLine serviceLine = serviceLineCatalogService.create(
                branchAdmin,
                new ServiceLineCatalogService.ManageServiceLineCommand(
                        "Private Duty",
                        "PD",
                        "Companion and personal care",
                        1));

        PatientEpisodeAuthorization authorization = patientEpisodeAuthorizationService.create(
                branchAdmin,
                patient.getId(),
                new PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand(
                        payerLink.getId(),
                        serviceLine.getId(),
                        "AUTH-001",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        40,
                        10,
                        PatientEpisodeAuthorizationStatus.ACTIVE,
                        "Initial authorization"));

        assertThatThrownBy(() -> patientEpisodeAuthorizationService.create(
                branchAdmin,
                patient.getId(),
                new PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand(
                        payerLink.getId(),
                        serviceLine.getId(),
                        "AUTH-001",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 8, 31),
                        20,
                        0,
                        PatientEpisodeAuthorizationStatus.ACTIVE,
                        "Conflicting authorization window")))
                .isInstanceOf(PatientConflictException.class)
                .hasMessage("Overlapping patient authorization window exists for the same authorization identity");

        patientEpisodeAuthorizationService.update(
                branchAdmin,
                authorization.getId(),
                new PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand(
                        payerLink.getId(),
                        serviceLine.getId(),
                        "AUTH-001",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        40,
                        40,
                        PatientEpisodeAuthorizationStatus.EXHAUSTED,
                        "All units consumed"));

        assertThatThrownBy(() -> patientEpisodeAuthorizationService.update(
                branchAdmin,
                authorization.getId(),
                new PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand(
                        payerLink.getId(),
                        serviceLine.getId(),
                        "AUTH-001",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30),
                        40,
                        40,
                        PatientEpisodeAuthorizationStatus.PENDING,
                        "Invalid transition")))
                .isInstanceOf(InvalidPatientStateTransitionException.class);

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(branchAdmin.getAgencyId());
        assertThat(events)
                .extracting(AuditEvent::getTargetType)
                .contains(
                        Epic3PatientTargetType.PATIENT.name(),
                        Epic3PatientTargetType.PATIENT_PAYER_LINK.name(),
                        Epic3PatientTargetType.PATIENT_EPISODE_AUTHORIZATION.name());
    }

    private AgencyMembership persistMembership(AgencyRole role) {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        User user = userRepository.saveAndFlush(
                User.invite("Agency", role.name(), role.name().toLowerCase() + "@northstar.example", "312-555-0199"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
