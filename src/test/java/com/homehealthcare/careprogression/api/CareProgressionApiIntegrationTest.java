package com.homehealthcare.careprogression.api;

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
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
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
class CareProgressionApiIntegrationTest {

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

    @Test
    void careProgressionApisSupportTemplateGoalInterventionProgressHistorySyncAndSummaryFlows() throws Exception {
        CareProgressionScenario scenario = createScenario();
        LocalDate atRiskDate = LocalDate.now().plusDays(3);

        String goalTemplateId = mockMvc.perform(post("/api/care-progression/goal-templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "serviceLineId":"%s",
                                  "name":"Mobility Improvement",
                                  "description":"Improve transfer and walking tolerance.",
                                  "targetOutcomeGuidance":"Patient should tolerate daily transfers with minimal assistance.",
                                  "defaultInterventionScaffold":"Daily transfer practice and gait observation.",
                                  "status":"ACTIVE"
                                }
                                """.formatted(scenario.branch().getId(), scenario.serviceLine().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/care-progression/goal-templates/%s".formatted(goalTemplateId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "serviceLineId":"%s",
                                  "name":"Mobility Improvement",
                                  "description":"Improve transfers, gait, and safe ambulation.",
                                  "targetOutcomeGuidance":"Patient should tolerate daily transfers with minimal assistance.",
                                  "defaultInterventionScaffold":"Daily transfer practice and gait observation.",
                                  "status":"ACTIVE"
                                }
                                """.formatted(scenario.branch().getId(), scenario.serviceLine().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Improve transfers, gait, and safe ambulation."));

        String patientGoalId = mockMvc.perform(post("/api/care-progression/patient-goals")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "branchId":"%s",
                                  "goalTemplateId":"%s",
                                  "ownerMembershipId":"%s",
                                  "title":"Increase walking endurance",
                                  "description":"Patient should walk safely from bedroom to kitchen.",
                                  "targetDate":"%s"
                                }
                                """.formatted(
                                        scenario.patient().getId(),
                                        scenario.branch().getId(),
                                        goalTemplateId,
                                        scenario.branchAdmin().getId(),
                                        atRiskDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/care-progression/patient-goals/%s".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId":"%s",
                                  "branchId":"%s",
                                  "goalTemplateId":"%s",
                                  "ownerMembershipId":"%s",
                                  "title":"Increase walking endurance",
                                  "description":"Patient should walk safely from bedroom to kitchen twice daily.",
                                  "targetDate":"%s"
                                }
                                """.formatted(
                                        scenario.patient().getId(),
                                        scenario.branch().getId(),
                                        goalTemplateId,
                                        scenario.branchAdmin().getId(),
                                        atRiskDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Patient should walk safely from bedroom to kitchen twice daily."));

        String interventionId = mockMvc.perform(post("/api/care-progression/patient-goals/%s/interventions".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "ownerMembershipId":"%s",
                                  "title":"Daily transfer practice",
                                  "description":"Practice sit-to-stand during each visit.",
                                  "targetDate":"%s",
                                  "status":"ACTIVE",
                                  "derivedFromTemplate":true
                                }
                                """.formatted(scenario.branch().getId(), scenario.branchAdmin().getId(), atRiskDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/care-progression/interventions/%s".formatted(interventionId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "ownerMembershipId":"%s",
                                  "title":"Daily transfer practice",
                                  "description":"Practice sit-to-stand and supervised gait each visit.",
                                  "targetDate":"%s",
                                  "status":"COMPLETED",
                                  "derivedFromTemplate":true
                                }
                                """.formatted(scenario.branch().getId(), scenario.branchAdmin().getId(), atRiskDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/care-progression/patient-goals/%s/progress-notes".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.caregiver())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "goalInterventionId":"%s",
                                  "branchId":"%s",
                                  "capturedByMembershipId":"%s",
                                  "noteText":"Patient completed supervised transfers with fewer rest breaks.",
                                  "progressionSummary":"Improving tolerance",
                                  "statusImpact":"NO_STATE_CHANGE"
                                }
                                """.formatted(interventionId, scenario.branch().getId(), scenario.caregiver().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capturedByMembershipId").value(scenario.caregiver().getId().toString()));

        mockMvc.perform(get("/api/care-progression/patient-goals/%s/progress-notes".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].progressionSummary").value("Improving tolerance"));

        mockMvc.perform(post("/api/care-progression/patient-goals/%s/state-transitions".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"COMPLETED",
                                  "changedAt":"2026-10-15T10:00:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/care-progression/patient-goals/%s/state-transitions".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"UNMET",
                                  "changedAt":"2026-10-16T10:00:00-05:00"
                                }
                                """))
                .andExpect(status().isBadRequest());

        String syncLinkId = mockMvc.perform(post("/api/care-progression/patient-goals/%s/careplan-sync".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "careplanIdentifier":"PLAN-1001",
                                  "syncStatus":"STALE",
                                  "lastSyncedAt":"2026-10-14T08:00:00-05:00",
                                  "syncSource":"plan-service"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("STALE"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/care-progression/careplan-sync/%s".formatted(syncLinkId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "careplanIdentifier":"PLAN-1001",
                                  "syncStatus":"ALIGNED",
                                  "lastSyncedAt":"2026-10-15T08:30:00-05:00",
                                  "syncSource":"plan-service"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("ALIGNED"));

        mockMvc.perform(get("/api/care-progression/patient-goals/%s/versions".formatted(patientGoalId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(5));

        mockMvc.perform(get("/api/care-progression/patients/%s/summary".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGoalCount").value(1))
                .andExpect(jsonPath("$.goals[0].targetDatePosture").value("AT_RISK"))
                .andExpect(jsonPath("$.goals[0].carePlanSyncStatus").value("ALIGNED"));

        mockMvc.perform(delete("/api/care-progression/interventions/%s".formatted(interventionId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.caregiver()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/care-progression/goal-templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.caregiver())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "name":"Unauthorized Template",
                                  "status":"ACTIVE"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/care-progression/goal-templates/%s".formatted(goalTemplateId))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.branchAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private CareProgressionScenario createScenario() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership branchAdmin = createMembership(agency, AgencyRole.BRANCH_ADMIN, "branchadmin@northstar.example");
        AgencyMembership caregiver = createMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(branchAdmin, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiver, branch));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-GOAL-1",
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
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(
                agency,
                "Skilled Nursing",
                "SN",
                "Skilled nursing care",
                1));
        return new CareProgressionScenario(agency, branch, branchAdmin, caregiver, patient, serviceLine);
    }

    private AgencyMembership createMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Epic13", role.name(), email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record CareProgressionScenario(
            Agency agency,
            Branch branch,
            AgencyMembership branchAdmin,
            AgencyMembership caregiver,
            Patient patient,
            ServiceLine serviceLine) {}
}
