package com.homehealthcare.messaging.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.messaging.domain.BranchBroadcastRepository;
import com.homehealthcare.messaging.domain.CommunicationMessageRepository;
import com.homehealthcare.messaging.domain.CommunicationThreadRepository;
import com.homehealthcare.messaging.domain.StaffGroupRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessagingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private CommunicationThreadRepository communicationThreadRepository;

    @Autowired
    private CommunicationMessageRepository communicationMessageRepository;

    @Autowired
    private StaffGroupRepository staffGroupRepository;

    @Autowired
    private BranchBroadcastRepository branchBroadcastRepository;

    @Test
    void messagingApisSupportInboxContextGroupsBroadcastsEscalationAndSummary() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = createMembership(createUser("Alicia", "Owner", "owner@northstar.example"), agency, AgencyRole.AGENCY_OWNER);
        AgencyMembership scheduler = createMembership(createUser("Sam", "Scheduler", "scheduler@northstar.example"), agency, AgencyRole.SCHEDULER_COORDINATOR);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver", "caregiver@northstar.example"), agency, AgencyRole.CAREGIVER);
        AgencyMembership reviewer = createMembership(createUser("Quinn", "Reviewer", "reviewer@northstar.example"), agency, AgencyRole.QA_CLINICAL_REVIEWER);
        assignBranch(scheduler, branch);
        assignBranch(caregiver, branch);
        assignBranch(reviewer, branch);

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-990",
                "Mila",
                null,
                "Patient",
                null,
                LocalDate.of(1955, 8, 12),
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
                java.time.OffsetDateTime.parse("2026-08-20T09:00:00-05:00"),
                java.time.OffsetDateTime.parse("2026-08-20T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        TaskTemplate taskTemplate = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agency,
                serviceLine,
                visitType,
                "Medication reminder",
                "MED-R",
                "Medication reminder",
                TaskTemplateCategory.CLINICAL,
                1));

        mockMvc.perform(post("/api/messaging/groups")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"North Team",
                                  "description":"North branch coordination",
                                  "branchId":"%s"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("North Team"));

        UUID groupId = staffGroupRepository.findAllByAgency_IdOrderByNameAsc(agency.getId()).get(0).getId();

        mockMvc.perform(post("/api/messaging/groups/{groupId}/members", groupId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "membershipId":"%s" }
                                """.formatted(caregiver.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(caregiver.getId().toString()));

        mockMvc.perform(post("/api/messaging/threads")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "threadType":"VISIT_COORDINATION",
                                  "subject":"Visit coordination",
                                  "branchId":"%s",
                                  "patientId":"%s",
                                  "visitOccurrenceId":"%s",
                                  "taskTemplateId":"%s",
                                  "participantMembershipIds":["%s"],
                                  "staffGroupIds":["%s"]
                                }
                                """.formatted(branch.getId(), patient.getId(), visit.getId(), taskTemplate.getId(), scheduler.getId(), groupId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadType").value("VISIT_COORDINATION"))
                .andExpect(jsonPath("$.patientId").value(patient.getId().toString()));

        UUID threadId = communicationThreadRepository.findAllByAgency_IdOrderByLastMessageAtDescCreatedAtDesc(agency.getId()).get(0).getId();

        mockMvc.perform(get("/api/messaging/threads")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(threadId.toString()));

        mockMvc.perform(get("/api/messaging/threads/{threadId}", threadId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thread.id").value(threadId.toString()))
                .andExpect(jsonPath("$.participants[0].membershipId").exists());

        mockMvc.perform(post("/api/messaging/threads/{threadId}/messages", threadId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "messageBody":"Please confirm arrival timing.",
                                  "sentAt":"2026-08-20T08:30:00-05:00",
                                  "messageType":"USER_MESSAGE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadId").value(threadId.toString()))
                .andExpect(jsonPath("$.messageBody").value("Please confirm arrival timing."));

        UUID messageId = communicationMessageRepository.findAllByThread_IdOrderByCreatedAtAtSourceAsc(threadId).get(0).getId();

        mockMvc.perform(post("/api/messaging/messages/{messageId}/read", messageId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "readAt":"2026-08-20T08:35:00-05:00" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryState").value("READ"));

        mockMvc.perform(get("/api/messaging/messages/{messageId}/read-receipts", messageId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(reviewer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId.toString()));

        mockMvc.perform(get("/api/messaging/threads/by-patient/{patientId}", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patient.getId().toString()));

        mockMvc.perform(get("/api/messaging/threads/by-visit/{visitId}", visit.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitOccurrenceId").value(visit.getId().toString()));

        mockMvc.perform(get("/api/messaging/threads/by-task/{taskTemplateId}", taskTemplate.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(reviewer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(threadId.toString()));

        mockMvc.perform(post("/api/messaging/threads/{threadId}/escalation", threadId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"URGENT",
                                  "tag":"ArrivalRisk",
                                  "reason":"ETA not confirmed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("URGENT"));

        mockMvc.perform(delete("/api/messaging/threads/{threadId}/escalation", threadId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(reviewer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/api/messaging/broadcasts")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "eligibleRoles":["SCHEDULER_COORDINATOR","CAREGIVER"],
                                  "subject":"Weather advisory",
                                  "body":"Expect delays today.",
                                  "expiresAt":"2026-08-20T18:00:00-05:00"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Weather advisory"))
                .andExpect(jsonPath("$.status").value("SENT"));

        UUID broadcastId = branchBroadcastRepository.findAllByAgency_IdOrderByCreatedAtDesc(agency.getId()).get(0).getId();

        mockMvc.perform(get("/api/messaging/broadcasts")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(broadcastId.toString()));

        mockMvc.perform(get("/api/messaging/summary")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread.unreadMessageCount").isNumber())
                .andExpect(jsonPath("$.recentBroadcasts[0].id").value(broadcastId.toString()));

        mockMvc.perform(put("/api/messaging/groups/{groupId}", groupId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"North Ops Team",
                                  "description":"Updated description",
                                  "branchId":"%s"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("North Ops Team"));

        mockMvc.perform(delete("/api/messaging/groups/{groupId}/members/{membershipId}", groupId, caregiver.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removedAt").exists());

        mockMvc.perform(delete("/api/messaging/groups/{groupId}", groupId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/messaging/broadcasts/{broadcastId}", broadcastId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void messagingApisReturnControlledForbiddenAndNotFoundErrors() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = createMembership(createUser("Alicia", "Owner", "owner2@northstar.example"), agency, AgencyRole.AGENCY_OWNER);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver", "caregiver2@northstar.example"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(post("/api/messaging/groups")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Forbidden",
                                  "description":"Forbidden",
                                  "branchId":"%s"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/messaging/threads/{threadId}", UUID.randomUUID())
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isNotFound());
    }

    private User createUser(String firstName, String lastName, String email) {
        User user = userRepository.saveAndFlush(User.invite(firstName, lastName, email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        return userRepository.saveAndFlush(user);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private void assignBranch(AgencyMembership membership, Branch branch) {
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(membership, branch));
    }
}
