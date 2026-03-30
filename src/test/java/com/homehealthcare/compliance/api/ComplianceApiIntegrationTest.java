package com.homehealthcare.compliance.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
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
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateChecklistDefinitionCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateDocumentationRequirementCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.EvaluateDocumentationRequirementCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecalculateChecklistResultCommand;
import com.homehealthcare.compliance.domain.ComplianceChecklistDefinition;
import com.homehealthcare.compliance.domain.RequiredDocumentationRequirement;
import com.homehealthcare.compliance.foundation.ComplianceChecklistResultStatus;
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
import java.time.OffsetDateTime;
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
class ComplianceApiIntegrationTest {

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
    private ComplianceWorkspaceService complianceWorkspaceService;

    @Test
    void complianceApisSupportDashboardWorkspaceMutationsAndHistory() throws Exception {
        ComplianceScenario scenario = createScenario();

        mockMvc.perform(post("/api/compliance/patients/%s/acknowledgments".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "acknowledgmentType":"RIGHT_TO_CARE",
                                  "effectiveAt":"2026-08-01T11:00:00-05:00",
                                  "expiresAt":"2026-09-01T00:00:00-05:00",
                                  "capturedByMembershipId":"%s",
                                  "captureMethod":"PATIENT_SIGNATURE"
                                }
                                """.formatted(scenario.branch().getId(), scenario.reviewer().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgmentType").value("RIGHT_TO_CARE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/compliance/patients/%s/certification-periods".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "programContext":"MEDICARE",
                                  "startDate":"2026-08-01",
                                  "endDate":"2026-09-30",
                                  "closed":false,
                                  "source":"MANUAL"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordState").value("ACTIVE"));

        String reminderId = mockMvc.perform(post("/api/compliance/patients/%s/risk-reminders".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "riskType":"FALL_RISK",
                                  "severityLabel":"HIGH",
                                  "summary":"Monitor for fall-risk changes.",
                                  "effectiveAt":"2026-08-01T12:00:00-05:00"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskType").value("FALL_RISK"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/compliance/patients/%s/status-projection/recalculate".formatted(scenario.patient().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "serviceLineId":"%s",
                                  "requiredAcknowledgmentTypes":["RIGHT_TO_CARE"],
                                  "certificationExpiryWarningDays":10,
                                  "evaluatedAt":"2026-08-01T13:00:00-05:00"
                                }
                                """.formatted(scenario.branch().getId(), scenario.serviceLine().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(scenario.patient().getId().toString()))
                .andExpect(jsonPath("$.readinessStatus").value("WARNING"));

        mockMvc.perform(get("/api/compliance/dashboard/patients")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("readinessStatus", "WARNING")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].patientId").value(scenario.patient().getId().toString()));

        mockMvc.perform(get("/api/compliance/patients/%s".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .param("serviceLineId", scenario.serviceLine().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection.readinessStatus").value("WARNING"))
                .andExpect(jsonPath("$.results.length()").value(2));

        mockMvc.perform(get("/api/compliance/patients/%s/checklist-results".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .param("serviceLineId", scenario.serviceLine().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationCategory").value("CHECKLIST_ITEM"));

        mockMvc.perform(get("/api/compliance/patients/%s/documentation-results".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .param("serviceLineId", scenario.serviceLine().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationCategory").value("REQUIRED_DOCUMENTATION"))
                .andExpect(jsonPath("$[0].missingReason").value("DOCUMENTATION_DUE_SOON"));

        mockMvc.perform(put("/api/compliance/patients/%s/acknowledgments/%s".formatted(
                                scenario.patient().getId(), scenario.acknowledgmentId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "acknowledgmentType":"RIGHT_TO_CARE",
                                  "effectiveAt":"2026-08-02T11:00:00-05:00",
                                  "expiresAt":"2026-10-01T00:00:00-05:00",
                                  "capturedByMembershipId":"%s",
                                  "captureMethod":"VERBAL"
                                }
                                """.formatted(scenario.branch().getId(), scenario.reviewer().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captureMethod").value("VERBAL"));

        mockMvc.perform(post("/api/compliance/patients/%s/certification-periods/%s/close".formatted(
                                scenario.patient().getId(), scenario.certificationPeriodId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId":"%s",
                                  "programContext":"MEDICARE",
                                  "startDate":"2026-08-01",
                                  "endDate":"2026-09-30",
                                  "source":"MANUAL"
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordState").value("CLOSED"));

        mockMvc.perform(post("/api/compliance/patients/%s/risk-reminders/%s/resolve".formatted(
                                scenario.patient().getId(), reminderId))
                        .param("resolvedAt", "2026-08-03T09:00:00-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(get("/api/compliance/patients/%s/history".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.auditContext.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/compliance/checklist-definitions/%s/history".formatted(scenario.checklistDefinition().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetId").value(scenario.checklistDefinition().getId().toString()));

        mockMvc.perform(get("/api/compliance/documentation-requirements/%s/history".formatted(scenario.documentationRequirement().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetId").value(scenario.documentationRequirement().getId().toString()));
    }

    @Test
    void complianceApisEnforceForbiddenForUnassignedReviewer() throws Exception {
        ComplianceScenario scenario = createScenario();

        mockMvc.perform(get("/api/compliance/patients/%s".formatted(scenario.patient().getId()))
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.outOfScopeReviewer()))))
                .andExpect(status().isForbidden());
    }

    private ComplianceScenario createScenario() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Compliance API Home Health",
                "compliance-api",
                "America/Chicago",
                "ops@compliance-api.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Compliance", "NC", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Compliance", "SC", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@compliance-api.example");
        AgencyMembership reviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@compliance-api.example");
        AgencyMembership outOfScopeReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "other-reviewer@compliance-api.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(reviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(outOfScopeReviewer, otherBranch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-COMP-API-1",
                "Nora",
                null,
                "Careplan",
                null,
                LocalDate.of(1951, 8, 3),
                "F",
                "312-555-0199",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));

        ComplianceChecklistDefinition checklistDefinition = complianceWorkspaceService.createChecklistDefinition(owner, new CreateChecklistDefinitionCommand(
                branch.getId(),
                serviceLine.getId(),
                "PLAN_COMPLETE",
                "Plan of care has required elements",
                "HIGH",
                5,
                true));
        complianceWorkspaceService.recalculateChecklistResult(reviewer, new RecalculateChecklistResultCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                checklistDefinition.getId(),
                ComplianceChecklistResultStatus.PASS,
                "MANUAL_REVIEW",
                null,
                "Plan is complete.",
                "SYSTEM",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                OffsetDateTime.parse("2026-08-01T10:00:00-05:00")));

        RequiredDocumentationRequirement documentationRequirement = complianceWorkspaceService.createDocumentationRequirement(owner, new CreateDocumentationRequirementCommand(
                branch.getId(),
                serviceLine.getId(),
                "VISIT_NOTE",
                "SIGNED_ATTACHMENT_NOTE",
                "Visit note must be recent and include attachment plus signature",
                true,
                true,
                2,
                5,
                true,
                true,
                true));
        complianceWorkspaceService.evaluateDocumentationRequirement(reviewer, new EvaluateDocumentationRequirementCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                documentationRequirement.getId(),
                OffsetDateTime.parse("2026-08-01T09:00:00-05:00"),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "SYSTEM",
                OffsetDateTime.parse("2026-08-01T10:05:00-05:00")));

        UUID acknowledgmentId = complianceWorkspaceService.recordAcknowledgment(reviewer, new ComplianceWorkspaceService.RecordConsentAcknowledgmentCommand(
                        patient.getId(),
                        branch.getId(),
                        "RIGHT_TO_CARE",
                        OffsetDateTime.parse("2026-08-01T11:00:00-05:00"),
                        OffsetDateTime.parse("2026-09-01T00:00:00-05:00"),
                        reviewer.getId(),
                        "PATIENT_SIGNATURE",
                        null,
                        null))
                .getId();

        UUID certificationPeriodId = complianceWorkspaceService.saveCertificationPeriod(reviewer, new ComplianceWorkspaceService.SaveCertificationPeriodCommand(
                        null,
                        patient.getId(),
                        branch.getId(),
                        null,
                        "MEDICARE",
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 30),
                        false,
                        "MANUAL"))
                .getId();

        return new ComplianceScenario(
                branch,
                patient,
                serviceLine,
                owner,
                reviewer,
                outOfScopeReviewer,
                checklistDefinition,
                documentationRequirement,
                acknowledgmentId,
                certificationPeriodId);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite(role.name(), "User", email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record ComplianceScenario(
            Branch branch,
            Patient patient,
            ServiceLine serviceLine,
            AgencyMembership owner,
            AgencyMembership reviewer,
            AgencyMembership outOfScopeReviewer,
            ComplianceChecklistDefinition checklistDefinition,
            RequiredDocumentationRequirement documentationRequirement,
            UUID acknowledgmentId,
            UUID certificationPeriodId) {
    }
}
