package com.homehealthcare.documentation.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
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
class DocumentationApiFailureIntegrationTest {

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
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private DocumentationTemplateRepository documentationTemplateRepository;

    @Test
    void documentationApiEnforcesSecurityAndValidation() throws Exception {
        Agency agency1 = agencyRepository.saveAndFlush(
                Agency.create("Agency 1", UUID.randomUUID().toString(), "America/Chicago", "ops@agency1.example"));
        Branch branch1 = branchRepository.saveAndFlush(Branch.create(agency1, "Branch 1", "B1", "Chicago", "America/Chicago"));
        AgencyMembership admin1 = createMembership(createUser("Admin", "1"), agency1, AgencyRole.BRANCH_ADMIN);
        Patient patient1 = patientRepository.saveAndFlush(Patient.create(
                agency1, "PAT-001", "Patient", "One", "One", null, LocalDate.of(1980, 1, 1), "M", null, null, null, null, null));

        Agency agency2 = agencyRepository.saveAndFlush(
                Agency.create("Agency 2", UUID.randomUUID().toString(), "America/New_York", "ops@agency2.example"));
        Branch branch2 = branchRepository.saveAndFlush(Branch.create(agency2, "Branch 2", "B2", "New York", "America/New_York"));
        AgencyMembership admin2 = createMembership(createUser("Admin", "2"), agency2, AgencyRole.BRANCH_ADMIN);

        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency1, "SN", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency1, serviceLine, "Routine", "RT", "Routine visit", 60, true, 1));
        DocumentationTemplate template1 = documentationTemplateRepository.saveAndFlush(DocumentationTemplate.createDraft(
                agency1, "Template 1", "T1", DocumentationTemplateType.VISIT_NOTE, "{}", 1));

        var visit = schedulingRecordService.createVisit(admin1, new SchedulingRecordService.ManageVisitCommand(
                patient1.getId(),
                branch1.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-08-01T09:00:00-05:00"),
                OffsetDateTime.parse("2026-08-01T10:00:00-05:00"),
                "America/Chicago", "routine", "manual", null));

        // Admin from agency 2 trying to create documentation for a visit in agency 1
        mockMvc.perform(post("/api/visit-documentation")
                        .with(authentication(TestTenantAuthentications.authenticationFor(admin2)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "visitOccurrenceId":"%s",
                                  "selectedTemplateId":"%s",
                                  "startedAt":"2026-08-01T09:05:00-05:00"
                                }
                                """.formatted(visit.getId(), template1.getId())))
                .andExpect(status().isNotFound()); // not found because the visit is in another agency

        // Admin from agency 1 trying to use a non-existent template
        mockMvc.perform(post("/api/visit-documentation")
                        .with(authentication(TestTenantAuthentications.authenticationFor(admin1)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "visitOccurrenceId":"%s",
                                  "selectedTemplateId":"%s",
                                  "startedAt":"2026-08-01T09:05:00-05:00"
                                }
                                """.formatted(visit.getId(), UUID.randomUUID())))
                .andExpect(status().isNotFound());

        // Create documentation successfully
        String createdDocumentationJson = mockMvc.perform(post("/api/visit-documentation")
                        .with(authentication(TestTenantAuthentications.authenticationFor(admin1)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "visitOccurrenceId":"%s",
                                  "selectedTemplateId":"%s",
                                  "startedAt":"2026-08-01T09:05:00-05:00"
                                }
                                """.formatted(visit.getId(), template1.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID documentationId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(createdDocumentationJson, "$.record.id"));

        // Admin from agency 2 trying to get the documentation
        mockMvc.perform(get("/api/visit-documentation/{documentationRecordId}", documentationId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(admin2))))
                .andExpect(status().isNotFound());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }
}
