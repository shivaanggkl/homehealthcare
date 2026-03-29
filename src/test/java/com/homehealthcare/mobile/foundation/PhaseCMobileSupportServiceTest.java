package com.homehealthcare.mobile.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService;
import com.homehealthcare.mobile.application.MobileFieldSupportService.CreateMobileIncidentCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.CreateMobileMessageThreadCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.SendMobileMessageCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.UploadMobileArtifactCommand;
import com.homehealthcare.mobile.application.UnauthorizedMobileActorException;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.mobile.domain.MobileIncidentReport;
import com.homehealthcare.mobile.domain.MobileMessageEntry;
import com.homehealthcare.mobile.domain.MobileMessageThread;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class PhaseCMobileSupportServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private MobileExecutionService mobileExecutionService;

    @Autowired
    private MobileFieldSupportService mobileFieldSupportService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void mobileSupportServicePersistsArtifactsIncidentsAndMessagesWithAudit() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-400",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1950, 5, 20),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-15T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-15T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                null));
        var session = mobileExecutionService.startVisitExecution(
                caregiverMembership,
                visit.getId(),
                new StartVisitExecutionCommand(
                        OffsetDateTime.parse("2026-04-15T09:01:00-05:00"),
                        null,
                        null,
                        "mobile_app",
                        null));

        MobileFieldArtifact photo = mobileFieldSupportService.uploadArtifact(
                caregiverMembership,
                session.getId(),
                new UploadMobileArtifactCommand(
                        MobileFieldArtifactType.PHOTO,
                        "arrival-photo.jpg",
                        "image/jpeg",
                        "jpeg-image".getBytes(StandardCharsets.UTF_8),
                        "Arrival photo"));
        MobileFieldArtifact signature = mobileFieldSupportService.uploadArtifact(
                caregiverMembership,
                session.getId(),
                new UploadMobileArtifactCommand(
                        MobileFieldArtifactType.SIGNATURE,
                        "signature.png",
                        "image/png",
                        "png-image".getBytes(StandardCharsets.UTF_8),
                        "Patient signature"));

        MobileIncidentReport incident = mobileFieldSupportService.createIncident(
                caregiverMembership,
                session.getId(),
                new CreateMobileIncidentCommand(
                        "CLINICAL_CONCERN",
                        "HIGH",
                        "Patient reported dizziness during visit.",
                        OffsetDateTime.parse("2026-04-15T09:20:00-05:00"),
                        "NOTIFY_BRANCH_CLINICAL",
                        Set.of(photo.getId())));

        MobileMessageThread thread = mobileFieldSupportService.createThread(
                caregiverMembership,
                session.getId(),
                new CreateMobileMessageThreadCommand("Route delay"));
        MobileMessageEntry message = mobileFieldSupportService.sendMessage(
                caregiverMembership,
                thread.getId(),
                new SendMobileMessageCommand("Running 10 minutes behind schedule.", OffsetDateTime.parse("2026-04-15T09:25:00-05:00")));

        var summaries = mobileFieldSupportService.getThreadSummaries(caregiverMembership);
        var detail = mobileFieldSupportService.getThreadDetail(caregiverMembership, thread.getId());
        var downloaded = mobileFieldSupportService.downloadArtifact(caregiverMembership, signature.getId());
        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());

        assertThat(photo.getArtifactType()).isEqualTo(MobileFieldArtifactType.PHOTO);
        assertThat(signature.getArtifactType()).isEqualTo(MobileFieldArtifactType.SIGNATURE);
        assertThat(incident.getArtifacts()).hasSize(1);
        assertThat(incident.getIncidentType()).isEqualTo("CLINICAL_CONCERN");
        assertThat(message.getId()).isNotNull();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).lastMessagePreview()).contains("Running 10 minutes behind");
        assertThat(detail.messages()).hasSize(1);
        assertThat(downloaded.fileName()).isEqualTo("signature.png");
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic6MobileAuditAction.PHOTO_UPLOADED.actionType(),
                        Epic6MobileAuditAction.SIGNATURE_CAPTURED.actionType(),
                        Epic6MobileAuditAction.INCIDENT_FLAGGED.actionType(),
                        Epic6MobileAuditAction.MESSAGE_SENT.actionType());
    }

    @Test
    void mobileSupportServiceBlocksCrossCaregiverArtifactAndThreadAccess() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        AgencyMembership otherCaregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "other@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                otherCaregiverMembership, branch, "CG-002", "Robin Relief", "Part Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-401",
                "Mila",
                null,
                "Patient",
                null,
                LocalDate.of(1952, 1, 10),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Care Visit", "CV", "Care", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-16T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-16T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", null));
        var session = mobileExecutionService.startVisitExecution(
                caregiverMembership,
                visit.getId(),
                new StartVisitExecutionCommand(OffsetDateTime.parse("2026-04-16T09:01:00-05:00"), null, null, "mobile_app", null));
        MobileFieldArtifact photo = mobileFieldSupportService.uploadArtifact(
                caregiverMembership,
                session.getId(),
                new UploadMobileArtifactCommand(MobileFieldArtifactType.PHOTO, "photo.jpg", "image/jpeg", "jpeg-image".getBytes(StandardCharsets.UTF_8), null));
        MobileMessageThread thread = mobileFieldSupportService.createThread(
                caregiverMembership,
                session.getId(),
                new CreateMobileMessageThreadCommand("Need supplies"));

        assertThatThrownBy(() -> mobileFieldSupportService.downloadArtifact(otherCaregiverMembership, photo.getId()))
                .isInstanceOf(UnauthorizedMobileActorException.class);
        assertThatThrownBy(() -> mobileFieldSupportService.getThreadDetail(otherCaregiverMembership, thread.getId()))
                .isInstanceOf(UnauthorizedMobileActorException.class);
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Mobile", "Actor", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
