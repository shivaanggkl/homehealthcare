package com.homehealthcare.mobile.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.mobile.domain.MobileQuickNoteStatus;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MobileExecutionApiIntegrationTest {

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
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private MobileFieldArtifactRepository mobileFieldArtifactRepository;

    @Test
    void mobileApisSupportTodayWorkExecutionDocumentationArtifactsIncidentsAndMessages() throws Exception {
        TestFixture fixture = createFixture();

        mockMvc.perform(get("/api/mobile/home")
                        .param("day", "2026-04-20")
                        .param("timezone", "America/Chicago")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits[0].visitId").value(fixture.visitId().toString()))
                .andExpect(jsonPath("$.visits[0].patientDisplaySummary").value("Ava Patient"));

        mockMvc.perform(get("/api/mobile/route")
                        .param("day", "2026-04-20")
                        .param("timezone", "America/Chicago")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stops[0].visitId").value(fixture.visitId().toString()));

        mockMvc.perform(get("/api/mobile/visits/{visitId}", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientSummary.patientDisplaySummary").value("Ava Patient"));

        String startPayload = """
                {
                  "startedAt":"2026-04-20T09:02:00-05:00",
                  "startedLatitude":41.881,
                  "startedLongitude":-87.623,
                  "startSource":"mobile_app"
                }
                """;

        String sessionResponse = mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", fixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionStatus").value("IN_PROGRESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID sessionId = extractUuid(sessionResponse, "id");

        String checklistPayload = """
                [{
                  "taskTemplateId":"%s",
                  "title":"Vitals",
                  "description":"Collect vitals",
                  "category":"CLINICAL",
                  "sortOrder":1,
                  "completed":true,
                  "completedAt":"2026-04-20T09:15:00-05:00",
                  "completionNotes":"Completed"
                }]
                """.formatted(fixture.taskTemplateId());
        mockMvc.perform(put("/api/mobile/execution-sessions/{executionSessionId}/task-checklist", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checklistPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].completed").value(true));

        String quickNotePayload = """
                {
                  "status":"%s",
                  "noteText":"Patient resting comfortably.",
                  "authoredAt":"2026-04-20T09:20:00-05:00"
                }
                """.formatted(MobileQuickNoteStatus.SUBMITTED.name());
        mockMvc.perform(post("/api/mobile/execution-sessions/{executionSessionId}/quick-notes", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quickNotePayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        MockMultipartFile photo = new MockMultipartFile(
                "file",
                "arrival-photo.jpg",
                "image/jpeg",
                "jpeg-image".getBytes(StandardCharsets.UTF_8));
        String artifactResponse = mockMvc.perform(multipart("/api/mobile/execution-sessions/{executionSessionId}/artifacts", sessionId)
                        .file(photo)
                        .param("artifactType", MobileFieldArtifactType.PHOTO.name())
                        .param("description", "Arrival")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactType").value("PHOTO"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID artifactId = extractUuid(artifactResponse, "id");

        String incidentPayload = """
                {
                  "incidentType":"CLINICAL_CONCERN",
                  "severity":"HIGH",
                  "narrative":"Patient reported dizziness.",
                  "reportedAt":"2026-04-20T09:30:00-05:00",
                  "escalationHook":"NOTIFY_BRANCH_CLINICAL",
                  "artifactIds":["%s"]
                }
                """.formatted(artifactId);
        mockMvc.perform(post("/api/mobile/execution-sessions/{executionSessionId}/incidents", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentType").value("CLINICAL_CONCERN"))
                .andExpect(jsonPath("$.artifactIds[0]").value(artifactId.toString()));

        String threadPayload = """
                {
                  "subject":"Route delay"
                }
                """;
        String threadResponse = mockMvc.perform(post("/api/mobile/execution-sessions/{executionSessionId}/messages/threads", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(threadPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Route delay"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID threadId = extractUuid(threadResponse, "id");

        String messagePayload = """
                {
                  "messageText":"Running 10 minutes behind schedule.",
                  "sentAt":"2026-04-20T09:35:00-05:00"
                }
                """;
        mockMvc.perform(post("/api/mobile/messages/threads/{threadId}/messages", threadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messagePayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadId").value(threadId.toString()));

        mockMvc.perform(get("/api/mobile/messages/threads")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threadId").value(threadId.toString()));

        mockMvc.perform(get("/api/mobile/messages/threads/{threadId}", threadId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].messageText").value("Running 10 minutes behind schedule."));

        mockMvc.perform(get("/api/mobile/artifacts/{artifactId}/download", artifactId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("arrival-photo.jpg")))
                .andExpect(content().bytes("jpeg-image".getBytes(StandardCharsets.UTF_8)));

        String endPayload = """
                {
                  "endedAt":"2026-04-20T09:58:00-05:00",
                  "endedLatitude":41.8811,
                  "endedLongitude":-87.6231,
                  "endSource":"mobile_app",
                  "syncStatus":"ACCEPTED"
                }
                """;
        mockMvc.perform(post("/api/mobile/execution-sessions/{executionSessionId}/end", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionStatus").value("COMPLETED"));
    }

    @Test
    void mobileApisBlockCrossCaregiverAccess() throws Exception {
        TestFixture fixture = createFixture();
        AgencyMembership otherCaregiverMembership = createMembership(fixture.agency(), AgencyRole.CAREGIVER, "other@example.com");
        caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                otherCaregiverMembership,
                fixture.branch(),
                "CG-099",
                "Robin Relief",
                "Part Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));

        String startPayload = """
                {
                  "startedAt":"2026-04-20T09:02:00-05:00",
                  "startSource":"mobile_app"
                }
                """;
        String sessionResponse = mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", fixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID sessionId = extractUuid(sessionResponse, "id");

        MockMultipartFile photo = new MockMultipartFile(
                "file",
                "arrival-photo.jpg",
                "image/jpeg",
                "jpeg-image".getBytes(StandardCharsets.UTF_8));
        String artifactResponse = mockMvc.perform(multipart("/api/mobile/execution-sessions/{executionSessionId}/artifacts", sessionId)
                        .file(photo)
                        .param("artifactType", MobileFieldArtifactType.PHOTO.name())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID artifactId = extractUuid(artifactResponse, "id");

        mockMvc.perform(get("/api/mobile/artifacts/{artifactId}/download", artifactId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(otherCaregiverMembership))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/mobile/visits/{visitId}", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(otherCaregiverMembership))))
                .andExpect(status().isForbidden());
    }

    @Test
    void mobileApisRejectDuplicateStartAndInvalidArtifactUploadClearly() throws Exception {
        TestFixture fixture = createFixture();

        String startPayload = """
                {
                  "startedAt":"2026-04-20T09:02:00-05:00",
                  "startSource":"mobile_app"
                }
                """;
        mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", fixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", fixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startPayload)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("An in-progress mobile execution session already exists for this visit."));

        TestFixture forbiddenFixture = createSecondFixture(fixture);
        mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", fixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAt":"2026-04-20T11:02:00-05:00",
                                  "startSource":"mobile_app"
                                }
                                """)
                        .with(authentication(TestTenantAuthentications.authenticationFor(forbiddenFixture.caregiverMembership()))))
                .andExpect(status().isForbidden());

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "capture.exe",
                "application/octet-stream",
                new byte[] {1, 2, 3});
        TestFixture nextDayFixture = createNextDayFixture(fixture);
        UUID sessionId = extractUuid(mockMvc.perform(post("/api/mobile/visits/{visitId}/execution/start", nextDayFixture.visitId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startedAt":"2026-04-21T09:02:00-05:00",
                                  "startSource":"mobile_app"
                                }
                                """)
                        .with(authentication(TestTenantAuthentications.authenticationFor(nextDayFixture.caregiverMembership()))))
                .andReturn().getResponse().getContentAsString(), "id");

        mockMvc.perform(multipart("/api/mobile/execution-sessions/{executionSessionId}/artifacts", sessionId)
                        .file(invalidFile)
                        .param("artifactType", MobileFieldArtifactType.PHOTO.name())
                        .with(authentication(TestTenantAuthentications.authenticationFor(nextDayFixture.caregiverMembership()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Artifact content type is not allowed"));
    }

    private TestFixture createFixture() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership ownerMembership = createMembership(agency, AgencyRole.AGENCY_OWNER, "owner@example.com");
        AgencyMembership caregiverMembership = createMembership(agency, AgencyRole.CAREGIVER, "caregiver@example.com");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                "CG-001",
                "Casey Care",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-500",
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
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine visit instructions", 60, true, 1));
        TaskTemplate taskTemplate = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agency,
                serviceLine,
                visitType,
                "Vitals",
                "VITALS",
                "Collect vitals",
                TaskTemplateCategory.CLINICAL,
                1));
        var visit = schedulingRecordService.createVisit(ownerMembership, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-20T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-20T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                "Knock before entering"));
        schedulingRecordService.assignCaregiver(ownerMembership, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                null));
        return new TestFixture(agency, branch, caregiverMembership, visit.getId(), taskTemplate.getId());
    }

    private AgencyMembership createMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Mobile", "Actor", email, "555-0101"));
        user.activateWithCredentials("{noop}Password123!");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record TestFixture(
            Agency agency,
            Branch branch,
            AgencyMembership caregiverMembership,
            UUID visitId,
            UUID taskTemplateId) {
    }

    private TestFixture createSecondFixture(TestFixture original) {
        AgencyMembership caregiverMembership = createMembership(original.agency(), AgencyRole.CAREGIVER, "other-start@example.com");
        caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                original.branch(),
                "CG-200",
                "Other Start",
                "Part Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        return new TestFixture(original.agency(), original.branch(), caregiverMembership, original.visitId(), original.taskTemplateId());
    }

    private TestFixture createNextDayFixture(TestFixture original) {
        AgencyMembership ownerMembership = createMembership(original.agency(), AgencyRole.AGENCY_OWNER, "owner-nextday@example.com");
        AgencyMembership caregiverMembership = createMembership(original.agency(), AgencyRole.CAREGIVER, "nextday@example.com");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                original.branch(),
                "CG-201",
                "Next Day",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                original.agency(),
                "PAT-501",
                "Nina",
                null,
                "Patient",
                null,
                LocalDate.of(1953, 2, 11),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.findAll().stream().findFirst().orElseThrow();
        VisitType visitType = visitTypeRepository.findAll().stream().findFirst().orElseThrow();
        var visit = schedulingRecordService.createVisit(ownerMembership, new ManageVisitCommand(
                patient.getId(),
                original.branch().getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-21T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-21T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(ownerMembership, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                original.branch().getId(),
                "board",
                null));
        return new TestFixture(original.agency(), original.branch(), caregiverMembership, visit.getId(), original.taskTemplateId());
    }

    private static UUID extractUuid(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) {
            throw new IllegalArgumentException("Field not found: " + field);
        }
        int valueStart = start + needle.length();
        int valueEnd = json.indexOf('"', valueStart);
        return UUID.fromString(json.substring(valueStart, valueEnd));
    }
}
