package com.homehealthcare.patientevent.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileFieldSupportService;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.patientevent.application.PatientEventRecordService;
import com.homehealthcare.patientevent.application.PatientEventRecordService.AddWoundHistoryCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.AssignFollowUpCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateEscalationCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateIncidentCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateInfectionCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.CreateWoundCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.LinkEvidenceCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateFollowUpCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateIncidentCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateInfectionCommand;
import com.homehealthcare.patientevent.application.PatientEventRecordService.UpdateWoundCommand;
import com.homehealthcare.patientevent.application.UnauthorizedPatientEventActorException;
import com.homehealthcare.patientevent.domain.IncidentRecord;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class PhaseBPatientEventServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private BranchAssignmentRepository branchAssignmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientAttachmentRepository patientAttachmentRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private MobileFieldSupportService mobileFieldSupportService;
    @Autowired
    private PatientEventRecordService patientEventRecordService;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void patientEventPhaseBSupportsIncidentInfectionWoundHistoryEvidenceAndAudit() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "North Star Events",
                "north-star-events",
                "America/Chicago",
                "ops@northstar-events.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Branch", "SB", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@events.example");
        AgencyMembership branchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "admin@events.example");
        AgencyMembership otherReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@events.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@events.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(branchAdmin, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(otherReviewer, otherBranch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiver, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-EVT-1",
                "Nora",
                null,
                "Patient",
                null,
                LocalDate.of(1951, 8, 3),
                "F",
                "312-555-0100",
                null,
                null,
                "en-US",
                null));

        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "SN", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine SN", "SN-R", "Routine SN visit", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-09-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-09-10T10:00:00-05:00"),
                "America/Chicago",
                "high",
                "manual",
                "Patient event context"));

        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiver,
                branch,
                "CG-EVT-1",
                "Casey Events",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                "assigned"));

        IncidentRecord incident = patientEventRecordService.createIncident(caregiver, new CreateIncidentCommand(
                patient.getId(),
                branch.getId(),
                visit.getId(),
                "FALL_EVENT",
                "HIGH",
                OffsetDateTime.parse("2026-09-10T09:10:00-05:00"),
                OffsetDateTime.parse("2026-09-10T09:15:00-05:00"),
                "Patient experienced a fall during the visit.",
                caregiver.getId()));

        IncidentRecord updatedIncident = patientEventRecordService.updateIncident(branchAdmin, incident.getId(), new UpdateIncidentCommand(
                branch.getId(),
                visit.getId(),
                "FALL_EVENT",
                "HIGH",
                OffsetDateTime.parse("2026-09-10T09:10:00-05:00"),
                OffsetDateTime.parse("2026-09-10T09:20:00-05:00"),
                "Patient experienced a fall during the visit and is under review.",
                IncidentRecordStatus.IN_REVIEW,
                caregiver.getId()));
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentRecordStatus.IN_REVIEW);

        var resolvedIncident = patientEventRecordService.resolveIncident(
                branchAdmin,
                incident.getId(),
                OffsetDateTime.parse("2026-09-10T10:30:00-05:00"));
        assertThat(resolvedIncident.getStatus()).isEqualTo(IncidentRecordStatus.RESOLVED);

        var infection = patientEventRecordService.createInfection(branchAdmin, new CreateInfectionCommand(
                patient.getId(),
                branch.getId(),
                incident.getId(),
                LocalDate.of(2026, 9, 10),
                OffsetDateTime.parse("2026-09-10T11:00:00-05:00"),
                "SKIN_INFECTION",
                "Area is being monitored for infection.",
                InfectionRecordStatus.ACTIVE));
        var updatedInfection = patientEventRecordService.updateInfection(branchAdmin, infection.getId(), new UpdateInfectionCommand(
                branch.getId(),
                incident.getId(),
                LocalDate.of(2026, 9, 10),
                OffsetDateTime.parse("2026-09-10T11:30:00-05:00"),
                "SKIN_INFECTION",
                "Area remains under monitoring.",
                InfectionRecordStatus.MONITORING));
        assertThat(updatedInfection.getStatus()).isEqualTo(InfectionRecordStatus.MONITORING);

        var resolvedInfection = patientEventRecordService.resolveInfection(
                branchAdmin,
                infection.getId(),
                OffsetDateTime.parse("2026-09-12T08:00:00-05:00"));
        assertThat(resolvedInfection.getStatus()).isEqualTo(InfectionRecordStatus.RESOLVED);

        var wound = patientEventRecordService.createWound(branchAdmin, new CreateWoundCommand(
                patient.getId(),
                branch.getId(),
                OffsetDateTime.parse("2026-09-10T09:45:00-05:00"),
                "LEFT_LEG",
                WoundRecordStatus.ACTIVE,
                "Initial wound observation."));
        var updatedWound = patientEventRecordService.updateWound(branchAdmin, wound.getId(), new UpdateWoundCommand(
                branch.getId(),
                "LEFT_LEG",
                WoundRecordStatus.STABLE,
                "Wound remains stable after initial treatment."));
        assertThat(updatedWound.getCurrentStatus()).isEqualTo(WoundRecordStatus.STABLE);

        var woundHistory = patientEventRecordService.addWoundHistory(branchAdmin, wound.getId(), new AddWoundHistoryCommand(
                branch.getId(),
                OffsetDateTime.parse("2026-09-11T08:00:00-05:00"),
                "Wound cleaned and redressed.",
                BigDecimal.valueOf(2.50),
                BigDecimal.valueOf(1.25),
                BigDecimal.valueOf(0.40),
                "IMPROVING",
                branchAdmin.getId()));
        assertThat(woundHistory.getProgressionMarker()).isEqualTo("IMPROVING");

        var resolvedWound = patientEventRecordService.resolveWound(
                branchAdmin,
                wound.getId(),
                OffsetDateTime.parse("2026-09-20T08:00:00-05:00"));
        assertThat(resolvedWound.getCurrentStatus()).isEqualTo(WoundRecordStatus.RESOLVED);

        PatientAttachment attachment = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                owner,
                "incident-note.pdf",
                "application/pdf",
                2048,
                "patient/incident-note.pdf",
                "INCIDENT_NOTE",
                "Supporting incident note"));

        var patientEvidence = patientEventRecordService.linkEvidence(branchAdmin, new LinkEvidenceCommand(
                Epic12PatientEventTargetType.INCIDENT_RECORD,
                incident.getId(),
                branch.getId(),
                attachment.getId(),
                null,
                null,
                branchAdmin.getId(),
                OffsetDateTime.parse("2026-09-10T12:00:00-05:00")));
        assertThat(patientEvidence.getSourceType()).isEqualTo(PatientEventEvidenceSourceType.PATIENT_ATTACHMENT);

        var session = mobileExecutionService.startVisitExecution(
                caregiver,
                visit.getId(),
                new MobileExecutionService.StartVisitExecutionCommand(
                        OffsetDateTime.parse("2026-09-10T09:02:00-05:00"),
                        BigDecimal.valueOf(41.881000),
                        BigDecimal.valueOf(-87.623000),
                        "mobile_app",
                        null));
        MobileFieldArtifact photoArtifact = mobileFieldSupportService.uploadArtifact(
                caregiver,
                session.getId(),
                new MobileFieldSupportService.UploadMobileArtifactCommand(
                        MobileFieldArtifactType.PHOTO,
                        "wound-photo.jpg",
                        "image/jpeg",
                        "photo-bytes".getBytes(),
                        "Wound photo"));

        var mobileEvidence = patientEventRecordService.linkEvidence(branchAdmin, new LinkEvidenceCommand(
                Epic12PatientEventTargetType.WOUND_HISTORY_ENTRY,
                woundHistory.getId(),
                branch.getId(),
                null,
                photoArtifact.getId(),
                null,
                branchAdmin.getId(),
                OffsetDateTime.parse("2026-09-11T08:15:00-05:00")));
        assertThat(mobileEvidence.getSourceType()).isEqualTo(PatientEventEvidenceSourceType.MOBILE_FIELD_ARTIFACT);

        var followUp = patientEventRecordService.assignFollowUp(branchAdmin, new AssignFollowUpCommand(
                Epic12PatientEventTargetType.INCIDENT_RECORD,
                incident.getId(),
                branch.getId(),
                branchAdmin.getId(),
                null,
                OffsetDateTime.parse("2026-09-10T12:05:00-05:00"),
                OffsetDateTime.parse("2026-09-11T12:05:00-05:00"),
                "Review patient safety and complete follow-up."));
        assertThat(followUp.getStatus()).isEqualTo(PatientEventFollowUpStatus.OPEN);

        var updatedFollowUp = patientEventRecordService.updateFollowUp(branchAdmin, followUp.getId(), new UpdateFollowUpCommand(
                branch.getId(),
                branchAdmin.getId(),
                null,
                OffsetDateTime.parse("2026-09-12T12:05:00-05:00"),
                "Review patient safety and coordinate next steps."));
        assertThat(updatedFollowUp.getDueAt()).isEqualTo(OffsetDateTime.parse("2026-09-12T12:05:00-05:00"));

        var completedFollowUp = patientEventRecordService.completeFollowUp(
                branchAdmin,
                followUp.getId(),
                OffsetDateTime.parse("2026-09-12T09:30:00-05:00"),
                "Completed corrective follow-up.");
        assertThat(completedFollowUp.getStatus()).isEqualTo(PatientEventFollowUpStatus.COMPLETED);

        var escalation = patientEventRecordService.createEscalation(branchAdmin, new CreateEscalationCommand(
                Epic12PatientEventTargetType.WOUND_RECORD,
                wound.getId(),
                branch.getId(),
                "HIGH",
                "Escalate wound review",
                branchAdmin.getId(),
                OffsetDateTime.parse("2026-09-11T09:00:00-05:00")));
        assertThat(escalation.getStatus()).isEqualTo(PatientEventEscalationStatus.ACTIVE);

        var clearedEscalation = patientEventRecordService.clearEscalation(
                branchAdmin,
                escalation.getId(),
                OffsetDateTime.parse("2026-09-12T10:00:00-05:00"),
                branchAdmin.getId());
        assertThat(clearedEscalation.getStatus()).isEqualTo(PatientEventEscalationStatus.CLEARED);

        var overdueFollowUp = patientEventRecordService.assignFollowUp(branchAdmin, new AssignFollowUpCommand(
                Epic12PatientEventTargetType.WOUND_RECORD,
                wound.getId(),
                branch.getId(),
                branchAdmin.getId(),
                null,
                OffsetDateTime.parse("2026-09-11T08:00:00-05:00"),
                OffsetDateTime.parse("2026-09-11T09:00:00-05:00"),
                "Past-due wound check."));
        assertThat(overdueFollowUp.isOverdue(OffsetDateTime.parse("2026-09-12T12:00:00-05:00"))).isTrue();

        var history = patientEventRecordService.listPatientLongitudinalHistory(branchAdmin, patient.getId());
        assertThat(history)
                .extracting(entry -> entry.historyEntryType())
                .contains(
                        PatientEventHistoryEntryType.INCIDENT_EVENT,
                        PatientEventHistoryEntryType.INFECTION_EVENT,
                        PatientEventHistoryEntryType.WOUND_CREATED,
                        PatientEventHistoryEntryType.WOUND_HISTORY_CAPTURED,
                        PatientEventHistoryEntryType.FOLLOW_UP_MILESTONE,
                        PatientEventHistoryEntryType.ESCALATION_MILESTONE,
                        PatientEventHistoryEntryType.EVIDENCE_EVENT);

        var alerts = patientEventRecordService.projectPatientAlerts(
                branchAdmin,
                patient.getId(),
                OffsetDateTime.parse("2026-09-12T12:00:00-05:00"));
        assertThat(alerts)
                .extracting(alert -> alert.alertType())
                .contains(
                        PatientEventAlertType.HIGH_SEVERITY_INCIDENT,
                        PatientEventAlertType.OVERDUE_FOLLOW_UP);

        assertThat(auditEventRepository.findAll().stream()
                .map(event -> event.getActionType()))
                .contains(
                        Epic12PatientEventAuditAction.INCIDENT_CREATED.actionType(),
                        Epic12PatientEventAuditAction.INCIDENT_UPDATED.actionType(),
                        Epic12PatientEventAuditAction.INFECTION_CREATED.actionType(),
                        Epic12PatientEventAuditAction.INFECTION_UPDATED.actionType(),
                        Epic12PatientEventAuditAction.WOUND_CREATED.actionType(),
                        Epic12PatientEventAuditAction.WOUND_UPDATED.actionType(),
                        Epic12PatientEventAuditAction.WOUND_HISTORY_ADDED.actionType(),
                        Epic12PatientEventAuditAction.EVIDENCE_LINKED.actionType(),
                        Epic12PatientEventAuditAction.FOLLOW_UP_ASSIGNED.actionType(),
                        Epic12PatientEventAuditAction.FOLLOW_UP_UPDATED.actionType(),
                        Epic12PatientEventAuditAction.ESCALATION_CREATED.actionType(),
                        Epic12PatientEventAuditAction.ESCALATION_CLEARED.actionType(),
                        Epic12PatientEventAuditAction.LONGITUDINAL_HISTORY_PROJECTED.actionType(),
                        Epic12PatientEventAuditAction.RECORD_RESOLVED.actionType());
    }

    @Test
    void patientEventPhaseBRejectsUnauthorizedOutOfBranchMutation() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "North Star Events",
                "north-star-events-2",
                "America/Chicago",
                "ops@northstar-events.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Branch", "SB", "Aurora", "America/Chicago"));
        AgencyMembership branchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "admin@events-2.example");
        AgencyMembership reviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@events-2.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(branchAdmin, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(reviewer, otherBranch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-EVT-2",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1950, 5, 20),
                "F",
                "312-555-0100",
                null,
                null,
                "en-US",
                null));
        var wound = patientEventRecordService.createWound(branchAdmin, new CreateWoundCommand(
                patient.getId(),
                branch.getId(),
                OffsetDateTime.parse("2026-09-10T09:45:00-05:00"),
                "RIGHT_ARM",
                WoundRecordStatus.ACTIVE,
                "Initial wound observation."));

        assertThatThrownBy(() -> patientEventRecordService.updateWound(reviewer, wound.getId(), new UpdateWoundCommand(
                branch.getId(),
                "RIGHT_ARM",
                WoundRecordStatus.STABLE,
                "Still stable.")))
                .isInstanceOf(UnauthorizedPatientEventActorException.class);

        assertThatThrownBy(() -> patientEventRecordService.listPatientLongitudinalHistory(reviewer, patient.getId()))
                .isInstanceOf(UnauthorizedPatientEventActorException.class);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Patient", "Event", email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
