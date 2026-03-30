package com.homehealthcare.messaging.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.messaging.application.MessagingCoordinationService;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageBranchBroadcastCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageEscalationCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageStaffGroupCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageThreadCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.SendMessageCommand;
import com.homehealthcare.messaging.application.UnauthorizedMessagingActorException;
import com.homehealthcare.messaging.domain.BranchBroadcast;
import com.homehealthcare.messaging.domain.CommunicationContextLinkRepository;
import com.homehealthcare.messaging.domain.CommunicationMessage;
import com.homehealthcare.messaging.domain.CommunicationMessageRepository;
import com.homehealthcare.messaging.domain.CommunicationThread;
import com.homehealthcare.messaging.domain.MessageReadReceipt;
import com.homehealthcare.messaging.domain.StaffGroup;
import com.homehealthcare.messaging.domain.ThreadParticipantRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
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
class PhaseBMessagingCoordinationServiceTest {

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
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private MessagingCoordinationService messagingCoordinationService;

    @Autowired
    private ThreadParticipantRepository threadParticipantRepository;

    @Autowired
    private CommunicationMessageRepository communicationMessageRepository;

    @Autowired
    private CommunicationContextLinkRepository communicationContextLinkRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void messagingCoordinationPersistsPhaseBModelsAndAuditSignals() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership scheduler = persistMembership(agency, AgencyRole.SCHEDULER_COORDINATOR, "scheduler@northstar.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        AgencyMembership reviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@northstar.example");
        assignBranch(scheduler, branch);
        assignBranch(caregiver, branch);
        assignBranch(reviewer, branch);

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-900",
                "Mila",
                null,
                "Patient",
                null,
                LocalDate.of(1951, 7, 3),
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
                OffsetDateTime.parse("2026-04-20T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-20T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        TaskTemplate taskTemplate = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agency,
                serviceLine,
                visitType,
                "Medication reminder",
                "MED-REM",
                "Confirm medication adherence",
                TaskTemplateCategory.CLINICAL,
                1));

        StaffGroup staffGroup = messagingCoordinationService.createStaffGroup(owner, new ManageStaffGroupCommand(
                "North clinical team",
                "Branch clinical coordination",
                branch.getId()));
        messagingCoordinationService.addStaffGroupMember(owner, staffGroup.getId(), caregiver.getId());

        CommunicationThread thread = messagingCoordinationService.createThread(owner, new ManageThreadCommand(
                MessagingThreadType.VISIT_COORDINATION,
                "Visit coordination",
                branch.getId(),
                patient.getId(),
                visit.getId(),
                taskTemplate.getId(),
                Set.of(scheduler.getId()),
                Set.of(staffGroup.getId())));

        CommunicationMessage message = messagingCoordinationService.sendMessage(owner, thread.getId(), new SendMessageCommand(
                "Please confirm arrival timing.",
                OffsetDateTime.parse("2026-04-20T08:30:00-05:00"),
                CommunicationMessageType.USER_MESSAGE,
                null));
        MessageReadReceipt readReceipt = messagingCoordinationService.markMessageRead(caregiver, message.getId(), OffsetDateTime.parse("2026-04-20T08:35:00-05:00"));
        messagingCoordinationService.tagEscalation(owner, thread.getId(), new ManageEscalationCommand(
                MessagingEscalationStatus.URGENT,
                "ArrivalRisk",
                "Caregiver has not yet confirmed ETA."));
        messagingCoordinationService.resolveEscalation(reviewer, thread.getId());

        BranchBroadcast broadcast = messagingCoordinationService.createBranchBroadcast(owner, new ManageBranchBroadcastCommand(
                branch.getId(),
                Set.of(AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.CAREGIVER),
                "Weather advisory",
                "Expect 15 minute delays across the north branch today.",
                OffsetDateTime.parse("2026-04-20T18:00:00-05:00")));

        var patientThreads = messagingCoordinationService.listThreadsForPatient(scheduler, patient.getId());
        var visitThreads = messagingCoordinationService.listThreadsForVisit(caregiver, visit.getId());
        var unreadProjection = messagingCoordinationService.unreadProjection(scheduler);
        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());

        assertThat(threadParticipantRepository.findAllByThread_IdAndRemovedAtIsNullOrderByAddedAtAsc(thread.getId()))
                .extracting(participant -> participant.getMembership().getId())
                .contains(owner.getId(), scheduler.getId(), caregiver.getId());
        assertThat(communicationMessageRepository.findAllByThread_IdOrderByCreatedAtAtSourceAsc(thread.getId())).hasSize(1);
        assertThat(communicationContextLinkRepository.findAllByThread_IdOrderByContextTypeAsc(thread.getId()))
                .extracting(link -> link.getContextType().name())
                .contains(
                        CoordinationContextType.BRANCH.name(),
                        CoordinationContextType.PATIENT.name(),
                        CoordinationContextType.VISIT.name(),
                        CoordinationContextType.TASK.name(),
                        CoordinationContextType.STAFF_GROUP.name());
        assertThat(readReceipt.getDeliveryState()).isEqualTo(MessagingDeliveryState.READ);
        assertThat(thread.getEscalationStatus()).isEqualTo(MessagingEscalationStatus.RESOLVED);
        assertThat(broadcast.getStatus()).isEqualTo(BranchBroadcastStatus.SENT);
        assertThat(patientThreads).extracting(CommunicationThread::getId).contains(thread.getId());
        assertThat(visitThreads).extracting(CommunicationThread::getId).contains(thread.getId());
        assertThat(unreadProjection.activeBroadcastCount()).isEqualTo(1);
        assertThat(unreadProjection.escalatedThreadCount()).isGreaterThanOrEqualTo(1);
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic9MessagingAuditAction.THREAD_CREATED.actionType(),
                        Epic9MessagingAuditAction.PARTICIPANT_ADDED.actionType(),
                        Epic9MessagingAuditAction.MESSAGE_SENT.actionType(),
                        Epic9MessagingAuditAction.MESSAGE_READ.actionType(),
                        Epic9MessagingAuditAction.STAFF_GROUP_UPDATED.actionType(),
                        Epic9MessagingAuditAction.BRANCH_BROADCAST_SENT.actionType(),
                        Epic9MessagingAuditAction.ESCALATION_TAGGED.actionType(),
                        Epic9MessagingAuditAction.ESCALATION_RESOLVED.actionType());
    }

    @Test
    void messagingCoordinationRejectsUnauthorizedBranchAccess() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");

        assertThatThrownBy(() -> messagingCoordinationService.createThread(caregiver, new ManageThreadCommand(
                        MessagingThreadType.PATIENT_COORDINATION,
                        "Unauthorized",
                        branch.getId(),
                        null,
                        null,
                        null,
                        Set.of(),
                        Set.of())))
                .isInstanceOf(UnauthorizedMessagingActorException.class);

        CommunicationThread thread = messagingCoordinationService.createThread(owner, new ManageThreadCommand(
                MessagingThreadType.DIRECT_SECURE,
                "Owner only",
                branch.getId(),
                null,
                null,
                null,
                Set.of(),
                Set.of()));

        assertThatThrownBy(() -> messagingCoordinationService.sendMessage(caregiver, thread.getId(), new SendMessageCommand(
                        "Forbidden",
                        OffsetDateTime.now(),
                        CommunicationMessageType.USER_MESSAGE,
                        null)))
                .isInstanceOf(UnauthorizedMessagingActorException.class);
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(
                Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Team", "Member", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private void assignBranch(AgencyMembership membership, Branch branch) {
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(membership, branch));
    }
}
