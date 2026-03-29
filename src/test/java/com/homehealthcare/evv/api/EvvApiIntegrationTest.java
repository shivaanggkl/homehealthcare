package com.homehealthcare.evv.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.evv.domain.GeofenceToleranceRule;
import com.homehealthcare.evv.domain.GeofenceToleranceRuleRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.testsupport.TestTenantAuthentications;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvvApiIntegrationTest {

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
    private PatientAddressRepository patientAddressRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private MobileFieldArtifactRepository mobileFieldArtifactRepository;
    @Autowired
    private GeofenceToleranceRuleRepository geofenceToleranceRuleRepository;

    @Test
    void evvApisSupportClockSummaryExceptionsMissedVisitsNotificationsAndEscalations() throws Exception {
        TestFixture fixture = createFixture();

        String clockInResponse = mockMvc.perform(post("/api/evv/visits/{visitId}/clock-in", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionSessionId":"%s",
                                  "capturedAt":"2026-04-22T09:01:00-05:00",
                                  "capturedLatitude":41.881001,
                                  "capturedLongitude":-87.623001,
                                  "timezone":"America/Chicago",
                                  "captureSource":"mobile_app",
                                  "platformSummary":"IOS",
                                  "appVersion":"1.0.0",
                                  "deviceClass":"PHONE",
                                  "timezoneOffsetMinutes":-300,
                                  "userAgentHash":"ua-hash",
                                  "sessionFingerprintHash":"session-hash"
                                }
                                """.formatted(fixture.executionSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("CLOCK_IN"))
                .andExpect(jsonPath("$.geofenceOutcome").value("WITHIN_TOLERANCE"))
                .andReturn().getResponse().getContentAsString();
        UUID verificationSessionId = extractUuid(clockInResponse, "verificationSessionId");

        mockMvc.perform(post("/api/evv/visits/{visitId}/clock-out", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "executionSessionId":"%s",
                                  "capturedAt":"2026-04-22T10:00:00-05:00",
                                  "capturedLatitude":41.881002,
                                  "capturedLongitude":-87.623002,
                                  "timezone":"America/Chicago",
                                  "captureSource":"mobile_app"
                                }
                                """.formatted(fixture.executionSessionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("CLOCK_OUT"));

        mockMvc.perform(post("/api/evv/verification-sessions/{verificationSessionId}/signatures", verificationSessionId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "artifactId":"%s",
                                  "signerRole":"PATIENT",
                                  "verificationStatus":"PRESENT",
                                  "recordedAt":"2026-04-22T10:01:00-05:00"
                                }
                                """.formatted(fixture.signatureArtifactId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PRESENT"));

        String exceptionResponse = mockMvc.perform(post("/api/evv/verification-sessions/{verificationSessionId}/exceptions", verificationSessionId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exceptionType":"LATE_START",
                                  "severity":"MEDIUM",
                                  "reasonCode":"TRAFFIC_DELAY",
                                  "narrative":"Traffic caused a delayed arrival."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        UUID exceptionId = extractUuid(exceptionResponse, "id");

        mockMvc.perform(get("/api/evv/my/visits/{visitId}/summary", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationSessionId").value(verificationSessionId.toString()))
                .andExpect(jsonPath("$.startEventPresent").value(true))
                .andExpect(jsonPath("$.endEventPresent").value(true))
                .andExpect(jsonPath("$.openExceptionCount").value(1));

        mockMvc.perform(get("/api/evv/visits/{visitId}/summary", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("EXCEPTION_OPEN"));

        mockMvc.perform(get("/api/evv/exceptions")
                        .param("status", "OPEN")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(exceptionId.toString()));

        mockMvc.perform(put("/api/evv/exceptions/{exceptionId}/status", exceptionId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"ACKNOWLEDGED",
                                  "actedAt":"2026-04-22T10:05:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        String missedVisitResponse = mockMvc.perform(post("/api/evv/visits/{visitId}/missed-visits", fixture.missedVisitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonCode":"PATIENT_REFUSED",
                                  "narrative":"Patient refused the visit.",
                                  "reportedAt":"2026-04-22T14:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andReturn().getResponse().getContentAsString();
        UUID missedVisitRecordId = extractUuid(missedVisitResponse, "id");

        mockMvc.perform(post("/api/evv/missed-visits/{missedVisitId}/notify-supervisor", missedVisitRecordId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientMembershipId":"%s",
                                  "channel":"IN_APP",
                                  "rationale":"Needs immediate review",
                                  "createdAt":"2026-04-22T14:05:00-05:00"
                                }
                                """.formatted(fixture.branchAdminMembership().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missedVisitRecordId").value(missedVisitRecordId.toString()));

        mockMvc.perform(post("/api/evv/exceptions/{exceptionId}/notify-supervisor", exceptionId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientMembershipId":"%s",
                                  "channel":"EMAIL",
                                  "rationale":"Exception requires QA review",
                                  "createdAt":"2026-04-22T10:06:00-05:00"
                                }
                                """.formatted(fixture.branchAdminMembership().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitExceptionRecordId").value(exceptionId.toString()));

        mockMvc.perform(post("/api/evv/exceptions/{exceptionId}/escalations", exceptionId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRoleKey":"QA_CLINICAL_REVIEWER",
                                  "severity":"HIGH",
                                  "rationale":"Create QA follow-up",
                                  "slaDueAt":"2026-04-23T09:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitExceptionRecordId").value(exceptionId.toString()))
                .andExpect(jsonPath("$.targetRoleKey").value("QA_CLINICAL_REVIEWER"));

        mockMvc.perform(post("/api/evv/missed-visits/{missedVisitId}/escalations", missedVisitRecordId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRoleKey":"QA_CLINICAL_REVIEWER",
                                  "severity":"CRITICAL",
                                  "rationale":"Missed visit needs immediate escalation",
                                  "slaDueAt":"2026-04-23T11:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missedVisitRecordId").value(missedVisitRecordId.toString()))
                .andExpect(jsonPath("$.severity").value("CRITICAL"));

        mockMvc.perform(get("/api/evv/readiness")
                        .param("day", "2026-04-22")
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.branchAdminMembership()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitId").value(fixture.visitId().toString()));
    }

    @Test
    void evvApisReturnConflictAndForbiddenForInvalidClockFlows() throws Exception {
        TestFixture fixture = createFixture();
        AgencyMembership otherCaregiverMembership = createMembership(fixture.agency(), AgencyRole.CAREGIVER, "other-evv@example.com");
        caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                otherCaregiverMembership,
                fixture.branch(),
                "CG-EVV-OTHER",
                "Other Care",
                "Part Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        geofenceToleranceRuleRepository.saveAndFlush(GeofenceToleranceRule.createBranchOverride(
                fixture.branch(), "Strict Rule", 10, 0, true));

        String payload = """
                {
                  "executionSessionId":"%s",
                  "capturedAt":"2026-04-22T09:01:00-05:00",
                  "capturedLatitude":41.990000,
                  "capturedLongitude":-87.800000,
                  "timezone":"America/Chicago",
                  "captureSource":"mobile_app"
                }
                """.formatted(fixture.executionSessionId());

        mockMvc.perform(post("/api/evv/visits/{visitId}/clock-in", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geofenceOutcome").value("OUTSIDE_TOLERANCE_BLOCKED"))
                .andExpect(jsonPath("$.overallOutcome").value("BLOCKED"));

        mockMvc.perform(post("/api/evv/visits/{visitId}/clock-in", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(fixture.caregiverMembership())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/evv/my/visits/{visitId}/summary", fixture.visitId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(otherCaregiverMembership))))
                .andExpect(status().isForbidden());
    }

    private TestFixture createFixture() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership ownerMembership = createMembership(agency, AgencyRole.AGENCY_OWNER, "owner-evv@example.com");
        AgencyMembership caregiverMembership = createMembership(agency, AgencyRole.CAREGIVER, "caregiver-evv@example.com");
        AgencyMembership branchAdminMembership = createMembership(agency, AgencyRole.BRANCH_ADMIN, "branch-admin-evv@example.com");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-EVV-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-EVV-API-1", "Ava", null, "Patient", null, LocalDate.of(1950, 1, 1), "F", "312-555-0100", null, null, "en-US", null));
        patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient, "123 Main St", null, "Chicago", "IL", "60601", "USA",
                BigDecimal.valueOf(41.881000), BigDecimal.valueOf(-87.623000), "GEOCODED", "America/Chicago", null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN-EVV", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV-EVV", "Routine visit", 60, true, 1));
        var visit = schedulingRecordService.createVisit(ownerMembership, new ManageVisitCommand(
                patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId(),
                OffsetDateTime.parse("2026-04-22T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-22T10:00:00-05:00"),
                "America/Chicago", null, null, "Knock first"));
        schedulingRecordService.assignCaregiver(ownerMembership, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(), branch.getId(), "board", null));
        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-04-22T09:00:30-05:00"),
                BigDecimal.valueOf(41.881001),
                BigDecimal.valueOf(-87.623001),
                "mobile_app",
                null));
        MobileFieldArtifact signatureArtifact = mobileFieldArtifactRepository.saveAndFlush(MobileFieldArtifact.create(
                executionSession,
                visit,
                caregiverProfile,
                patient,
                branch,
                MobileFieldArtifactType.SIGNATURE,
                "signature.png",
                "image/png",
                1024,
                "mobile/signature.png",
                "Captured signature"));

        Patient missedPatient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-EVV-API-2", "Noah", null, "Missed", null, LocalDate.of(1949, 3, 1), "M", "312-555-0101", null, null, "en-US", null));
        var missedVisit = schedulingRecordService.createVisit(ownerMembership, new ManageVisitCommand(
                missedPatient.getId(), branch.getId(), serviceLine.getId(), visitType.getId(),
                OffsetDateTime.parse("2026-04-22T13:00:00-05:00"),
                OffsetDateTime.parse("2026-04-22T14:00:00-05:00"),
                "America/Chicago", null, null, null));
        schedulingRecordService.assignCaregiver(ownerMembership, missedVisit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(), branch.getId(), "board", null));
        return new TestFixture(
                agency,
                branch,
                caregiverMembership,
                branchAdminMembership,
                visit.getId(),
                missedVisit.getId(),
                executionSession.getId(),
                signatureArtifact.getId());
    }

    private AgencyMembership createMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("EVV", role.name(), email, null));
        user.activateWithCredentials("{noop}Password123!");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
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

    private record TestFixture(
            Agency agency,
            Branch branch,
            AgencyMembership caregiverMembership,
            AgencyMembership branchAdminMembership,
            UUID visitId,
            UUID missedVisitId,
            UUID executionSessionId,
            UUID signatureArtifactId) {
    }
}
