package com.homehealthcare.patientevent.api;

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
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
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
class PatientEventApiIntegrationTest {

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
    private PatientAttachmentRepository patientAttachmentRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Test
    void patientEventApisSupportRecordsEvidenceFollowUpsEscalationsTimelineAndSummary() throws Exception {
        PatientEventScenario scenario = createScenario();

        String incidentId = mockMvc.perform(post("/api/patient-events/incidents")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "branchId":"%s",
                                  "visitOccurrenceId":"%s",
                                  "incidentType":"FALL_EVENT",
                                  "severityLabel":"HIGH",
                                  "occurredAt":"2026-09-10T09:10:00-05:00",
                                  "reportedAt":"2026-09-10T09:15:00-05:00",
                                  "summary":"Patient experienced a fall during the visit.",
                                  "reportedByMembershipId":"%s"
                                }
                                """.formatted(
                                        scenario.patient().getId(),
                                        scenario.branch().getId(),
                                        scenario.visitId(),
                                        scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/patient-events/incidents/%s".formatted(incidentId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "visitOccurrenceId":"%s",
                                  "incidentType":"FALL_EVENT",
                                  "severityLabel":"HIGH",
                                  "occurredAt":"2026-09-10T09:10:00-05:00",
                                  "reportedAt":"2026-09-10T09:20:00-05:00",
                                  "summary":"Patient experienced a fall during the visit and is under review.",
                                  "status":"IN_REVIEW",
                                  "reportedByMembershipId":"%s"
                                }
                                """.formatted(scenario.branch().getId(), scenario.visitId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mockMvc.perform(post("/api/patient-events/incidents/%s/resolve".formatted(incidentId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "resolvedAt":"2026-09-10T10:30:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        String infectionId = mockMvc.perform(post("/api/patient-events/infections")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "branchId":"%s",
                                  "relatedIncidentId":"%s",
                                  "onsetDate":"2026-09-10",
                                  "identifiedAt":"2026-09-10T11:00:00-05:00",
                                  "infectionType":"SKIN_INFECTION",
                                  "summary":"Area is being monitored for infection.",
                                  "status":"ACTIVE"
                                }
                                """.formatted(scenario.patient().getId(), scenario.branch().getId(), incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/patient-events/infections/%s".formatted(infectionId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "relatedIncidentId":"%s",
                                  "onsetDate":"2026-09-10",
                                  "identifiedAt":"2026-09-10T11:30:00-05:00",
                                  "infectionType":"SKIN_INFECTION",
                                  "summary":"Area remains under monitoring.",
                                  "status":"MONITORING"
                                }
                                """.formatted(scenario.branch().getId(), incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MONITORING"));

        String woundId = mockMvc.perform(post("/api/patient-events/wounds")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "branchId":"%s",
                                  "identifiedAt":"2026-09-10T09:45:00-05:00",
                                  "woundTypeOrSite":"LEFT_LEG",
                                  "currentStatus":"ACTIVE",
                                  "baselineSummary":"Initial wound observation."
                                }
                                """.formatted(scenario.patient().getId(), scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/patient-events/wounds/%s".formatted(woundId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "woundTypeOrSite":"LEFT_LEG",
                                  "currentStatus":"STABLE",
                                  "baselineSummary":"Wound remains stable after initial treatment."
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("STABLE"));

        String woundHistoryId = mockMvc.perform(post("/api/patient-events/wounds/%s/history".formatted(woundId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "capturedAt":"2026-09-11T08:00:00-05:00",
                                  "observationSummary":"Wound cleaned and redressed.",
                                  "lengthCm":2.50,
                                  "widthCm":1.25,
                                  "depthCm":0.40,
                                  "progressionMarker":"DETERIORATING",
                                  "capturedByMembershipId":"%s"
                                }
                                """.formatted(scenario.branch().getId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressionMarker").value("DETERIORATING"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/patient-events/wounds/%s/history".formatted(woundId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(woundHistoryId));

        String evidenceLinkId = mockMvc.perform(post("/api/patient-events/evidence-links")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"INCIDENT_RECORD",
                                  "targetId":"%s",
                                  "branchId":"%s",
                                  "patientAttachmentId":"%s",
                                  "linkedByMembershipId":"%s",
                                  "linkedAt":"2026-09-10T12:00:00-05:00"
                                }
                                """.formatted(
                                        incidentId,
                                        scenario.branch().getId(),
                                        scenario.patientAttachment().getId(),
                                        scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("PATIENT_ATTACHMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/patient-events/evidence-links")
                        .param("targetType", "INCIDENT_RECORD")
                        .param("targetId", incidentId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(evidenceLinkId));

        String followUpId = mockMvc.perform(post("/api/patient-events/follow-ups")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"INCIDENT_RECORD",
                                  "targetId":"%s",
                                  "branchId":"%s",
                                  "ownerMembershipId":"%s",
                                  "assignedAt":"2026-09-10T12:05:00-05:00",
                                  "dueAt":"2026-09-11T12:05:00-05:00",
                                  "followUpNote":"Review patient safety and complete follow-up."
                                }
                                """.formatted(incidentId, scenario.branch().getId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/patient-events/follow-ups/%s".formatted(followUpId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "ownerMembershipId":"%s",
                                  "dueAt":"2026-09-12T12:05:00-05:00",
                                  "followUpNote":"Review patient safety and coordinate next steps."
                                }
                                """.formatted(scenario.branch().getId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueAt").exists());

        mockMvc.perform(post("/api/patient-events/follow-ups/%s/complete".formatted(followUpId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "completionAt":"2026-09-12T09:30:00-05:00",
                                  "followUpNote":"Completed corrective follow-up."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/patient-events/follow-ups")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"WOUND_RECORD",
                                  "targetId":"%s",
                                  "branchId":"%s",
                                  "ownerMembershipId":"%s",
                                  "assignedAt":"2026-09-11T08:00:00-05:00",
                                  "dueAt":"2026-09-11T09:00:00-05:00",
                                  "followUpNote":"Past-due wound check."
                                }
                                """.formatted(woundId, scenario.branch().getId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk());

        String escalationId = mockMvc.perform(post("/api/patient-events/escalations")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType":"WOUND_RECORD",
                                  "targetId":"%s",
                                  "branchId":"%s",
                                  "severityLabel":"HIGH",
                                  "reasonTag":"Escalate wound review",
                                  "escalatedByMembershipId":"%s",
                                  "escalatedAt":"2026-09-11T09:00:00-05:00"
                                }
                                """.formatted(woundId, scenario.branch().getId(), scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/patient-events/escalations")
                        .param("patientId", scenario.patient().getId().toString())
                        .param("status", "ACTIVE")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(escalationId));

        mockMvc.perform(post("/api/patient-events/infections/%s/resolve".formatted(infectionId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "resolvedAt":"2026-09-12T08:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/api/patient-events/wounds/%s/resolve".formatted(woundId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "resolvedAt":"2026-09-20T08:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("RESOLVED"));

        mockMvc.perform(post("/api/patient-events/escalations/%s/clear".formatted(escalationId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clearedAt":"2026-09-12T10:00:00-05:00",
                                  "clearedByMembershipId":"%s"
                                }
                                """.formatted(scenario.branchAdmin().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"));

        mockMvc.perform(get("/api/patient-events/incidents")
                        .param("patientId", scenario.patient().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentId));

        mockMvc.perform(get("/api/patient-events/infections")
                        .param("patientId", scenario.patient().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(infectionId));

        mockMvc.perform(get("/api/patient-events/wounds")
                        .param("patientId", scenario.patient().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(woundId));

        mockMvc.perform(get("/api/patient-events/follow-ups")
                        .param("patientId", scenario.patient().getId().toString())
                        .param("overdueAsOf", "2026-09-12T12:00:00-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        mockMvc.perform(get("/api/patient-events/patients/%s/timeline".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historyEntryType").exists());

        mockMvc.perform(get("/api/patient-events/patients/%s/alerts".formatted(scenario.patient().getId()))
                        .param("asOf", "2026-09-12T12:00:00-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].alertType").isArray());

        mockMvc.perform(get("/api/patient-events/patients/%s/summary".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(scenario.patient().getId().toString()))
                .andExpect(jsonPath("$.openIncidentCount").value(0))
                .andExpect(jsonPath("$.activeInfectionCount").value(0))
                .andExpect(jsonPath("$.activeWoundCount").value(0))
                .andExpect(jsonPath("$.openFollowUpCount").value(1));

        mockMvc.perform(delete("/api/patient-events/evidence-links/%s".formatted(evidenceLinkId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/patient-events/evidence-links")
                        .param("targetType", "INCIDENT_RECORD")
                        .param("targetId", incidentId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void patientEventApisEnforceForbiddenForOutOfScopeReviewer() throws Exception {
        PatientEventScenario scenario = createScenario();

        mockMvc.perform(get("/api/patient-events/patients/%s/timeline".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.outOfScopeReviewer()))))
                .andExpect(status().isForbidden());
    }

    private PatientEventScenario createScenario() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Patient Event API Home Health",
                "patient-event-api",
                "America/Chicago",
                "ops@patient-event-api.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Event", "NE", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Event", "SE", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@patient-event-api.example");
        AgencyMembership branchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "admin@patient-event-api.example");
        AgencyMembership outOfScopeReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@patient-event-api.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(branchAdmin, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(outOfScopeReviewer, otherBranch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-EVT-API-1",
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
        UUID visitId = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                        patient.getId(),
                        branch.getId(),
                        serviceLine.getId(),
                        visitType.getId(),
                        java.time.OffsetDateTime.parse("2026-09-10T09:00:00-05:00"),
                        java.time.OffsetDateTime.parse("2026-09-10T10:00:00-05:00"),
                        "America/Chicago",
                        "high",
                        "manual",
                        "Patient event context"))
                .getId();

        PatientAttachment patientAttachment = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                owner,
                "incident-note.pdf",
                "application/pdf",
                2048,
                "patient/incident-note.pdf",
                "INCIDENT_NOTE",
                "Supporting incident note"));

        return new PatientEventScenario(branch, branchAdmin, outOfScopeReviewer, patient, patientAttachment, visitId);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Patient", "Event", email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record PatientEventScenario(
            Branch branch,
            AgencyMembership branchAdmin,
            AgencyMembership outOfScopeReviewer,
            Patient patient,
            PatientAttachment patientAttachment,
            UUID visitId) {
    }
}
