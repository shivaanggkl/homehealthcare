package com.homehealthcare.documentation.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateFieldRepository;
import com.homehealthcare.documentation.domain.DocumentationTemplateTaskRepository;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
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
class DocumentationApiIntegrationTest {

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
    private PatientRepository patientRepository;

    @Autowired
    private PatientAttachmentRepository patientAttachmentRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private DocumentationTemplateRepository documentationTemplateRepository;

    @Autowired
    private VisitDocumentationRecordRepository visitDocumentationRecordRepository;

    @Autowired
    private DocumentationTemplateFieldRepository documentationTemplateFieldRepository;

    @Autowired
    private DocumentationTemplateTaskRepository documentationTemplateTaskRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Test
    void documentationApisSupportTemplateManagementDocumentationLifecyclePrintableSummaryAndSearch() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-880",
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
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine visit", 60, true, 1));

        mockMvc.perform(post("/api/documentation/task-library")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId":"%s",
                                  "visitTypeId":"%s",
                                  "name":"Vitals",
                                  "code":"VITALS",
                                  "description":"Collect vitals",
                                  "category":"CLINICAL",
                                  "displayOrder":1,
                                  "defaultSortOrder":1,
                                  "defaultCompletionExpectation":"Complete before departure",
                                  "requiredByDefault":true
                                }
                                """.formatted(serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VITALS"))
                .andExpect(jsonPath("$.requiredByDefault").value(true));

        UUID taskId = taskTemplateRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId()).stream()
                .filter(item -> item.getCode().equals("VITALS"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/documentation/task-library")
                        .param("category", "CLINICAL")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("VITALS"));

        mockMvc.perform(post("/api/documentation/templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Skilled Nursing Routine Note",
                                  "code":"SN-RV",
                                  "templateType":"VISIT_NOTE",
                                  "structuredDefinitionJson":"{\\"version\\":1}",
                                  "displayOrder":1,
                                  "serviceLineId":"%s",
                                  "visitTypeId":"%s",
                                  "branchId":"%s",
                                  "helpText":"Complete before submitting.",
                                  "allowedActorRoles":["CAREGIVER","BRANCH_ADMIN"],
                                  "requiresSignatureVerification":false,
                                  "sections":[
                                    {"sectionKey":"overview","title":"Overview","helpText":"Visit overview","sortOrder":1}
                                  ],
                                  "fields":[
                                    {
                                      "sectionKey":"overview",
                                      "fieldKey":"narrative",
                                      "label":"Visit Narrative",
                                      "fieldType":"LONG_TEXT",
                                      "requiredField":true,
                                      "sortOrder":1,
                                      "visibleActorRoles":["CAREGIVER","BRANCH_ADMIN"],
                                      "editableActorRoles":["CAREGIVER","BRANCH_ADMIN"]
                                    }
                                  ],
                                  "tasks":[
                                    {
                                      "sectionKey":"overview",
                                      "taskTemplateId":"%s",
                                      "sortOrder":1
                                    }
                                  ]
                                }
                                """.formatted(serviceLine.getId(), visitType.getId(), branch.getId(), taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.code").value("SN-RV"))
                .andExpect(jsonPath("$.fields[0].fieldKey").value("narrative"))
                .andExpect(jsonPath("$.tasks[0].effectiveTitle").value("Vitals"));

        var template = documentationTemplateRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId()).stream()
                .filter(item -> item.getCode().equals("SN-RV"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/documentation/templates")
                        .param("search", "SN-RV")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("SN-RV"));

        var visit = schedulingRecordService.createVisit(branchAdmin, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-07-10T10:00:00-05:00"),
                "America/Chicago",
                "routine",
                "manual",
                "Knock before entering"));

        mockMvc.perform(post("/api/visit-documentation")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "visitOccurrenceId":"%s",
                                  "selectedTemplateId":"%s",
                                  "startedAt":"2026-07-10T09:05:00-05:00"
                                }
                                """.formatted(visit.getId(), template.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record.status").value("DRAFT"));

        var documentationRecord = visitDocumentationRecordRepository.findByVisitOccurrence_IdAndSelectedTemplate_Id(visit.getId(), template.getId()).orElseThrow();

        mockMvc.perform(post("/api/visit-documentation/{documentationRecordId}/submit", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "submittedAt":"2026-07-10T09:10:00-05:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].fieldKey").value("narrative"));

        UUID templateFieldId = documentationTemplateFieldRepository.findAllByDocumentationTemplate_IdOrderBySortOrderAscLabelAsc(template.getId())
                .get(0)
                .getId();
        UUID templateTaskId = documentationTemplateTaskRepository.findAllByDocumentationTemplate_IdOrderBySortOrderAscIdAsc(template.getId())
                .get(0)
                .getId();

        mockMvc.perform(put("/api/visit-documentation/{documentationRecordId}/draft", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldResponses":[
                                    {
                                      "templateFieldId":"%s",
                                      "normalizedValue":"{\\"text\\":\\"Patient tolerated visit well.\\"}",
                                      "displayValue":"Patient tolerated visit well.",
                                      "completionState":"COMPLETED",
                                      "completedAt":"2026-07-10T09:11:00-05:00"
                                    }
                                  ],
                                  "taskResponses":[
                                    {
                                      "templateTaskId":"%s",
                                      "completionState":"COMPLETED",
                                      "completionNotes":"Vitals obtained",
                                      "completedAt":"2026-07-10T09:12:00-05:00"
                                    }
                                  ],
                                  "savedAt":"2026-07-10T09:12:00-05:00"
                                }
                                """.formatted(templateFieldId, templateTaskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.fieldResponses[0].displayValue").value("Patient tolerated visit well."));

        PatientAttachment attachment = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                branchAdmin,
                "care-plan.pdf",
                "application/pdf",
                1024,
                "patient/care-plan.pdf",
                "CARE_PLAN",
                "Existing care plan"));

        String attachmentResponse = mockMvc.perform(post("/api/visit-documentation/{documentationRecordId}/attachments/patient-links", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "patientAttachmentId":"%s",
                                  "caption":"Care plan",
                                  "description":"Care plan reference"
                                }
                                """.formatted(attachment.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientAttachmentId").value(attachment.getId().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID attachmentLinkId = extractUuid(attachmentResponse, "id");

        mockMvc.perform(get("/api/visit-documentation/{documentationRecordId}/attachments", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientAttachmentId").value(attachment.getId().toString()));

        mockMvc.perform(post("/api/visit-documentation/{documentationRecordId}/submit", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "submittedAt":"2026-07-10T09:15:00-05:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/visit-documentation")
                        .param("status", "SUBMITTED")
                        .param("patientId", patient.getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].templateName").value("Skilled Nursing Routine Note"));

        mockMvc.perform(get("/api/visit-documentation/by-visit")
                        .param("visitOccurrenceId", visit.getId().toString())
                        .param("selectedTemplateId", template.getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record.id").value(documentationRecord.getId().toString()));

        mockMvc.perform(get("/api/visit-documentation/{documentationRecordId}/printable-summary", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateTitle").value("Skilled Nursing Routine Note"))
                .andExpect(jsonPath("$.fields[0].displayValue").value("Patient tolerated visit well."));

        mockMvc.perform(get("/api/visit-documentation/{documentationRecordId}/printable-summary/export", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Skilled Nursing Routine Note")));

        mockMvc.perform(delete("/api/visit-documentation/{documentationRecordId}/attachments/{attachmentLinkId}", documentationRecord.getId(), attachmentLinkId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/visit-documentation/{documentationRecordId}/attachments", documentationRecord.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/documentation/templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Forbidden",
                                  "code":"FORBIDDEN",
                                  "templateType":"VISIT_NOTE",
                                  "structuredDefinitionJson":"{\\"version\\":1}",
                                  "displayOrder":1,
                                  "requiresSignatureVerification":false,
                                  "sections":[{"sectionKey":"overview","title":"Overview","sortOrder":1}],
                                  "fields":[{"sectionKey":"overview","fieldKey":"narrative","label":"Narrative","fieldType":"TEXT","requiredField":true,"sortOrder":1}],
                                  "tasks":[]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }

    private static UUID extractUuid(String json, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalArgumentException("Field not found in JSON: " + fieldName);
        }
        int valueStart = start + marker.length();
        int valueEnd = json.indexOf('"', valueStart);
        return UUID.fromString(json.substring(valueStart, valueEnd));
    }
}
