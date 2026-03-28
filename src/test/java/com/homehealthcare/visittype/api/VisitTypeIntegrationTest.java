package com.homehealthcare.visittype.api;

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
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
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
class VisitTypeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Test
    void branchAdminCanCrudVisitTypesWithServiceLineFiltering() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agency, "Private Duty", "PD", "Private duty", 1));
        VisitType existing = visitTypeRepository.saveAndFlush(
                VisitType.create(agency, serviceLine, "Private Duty Standard", "PD-STD", "Default", 60, true, 1));

        mockMvc.perform(get("/api/visit-types")
                        .param("serviceLineId", serviceLine.getId().toString())
                        .param("status", "ACTIVE")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceLineId").value(serviceLine.getId().toString()));

        mockMvc.perform(post("/api/visit-types")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId": "%s",
                                  "name": "Private Duty Extended",
                                  "code": "PD-EXT",
                                  "description": "Longer visit",
                                  "defaultDurationMinutes": 90,
                                  "billable": true,
                                  "displayOrder": 2
                                }
                                """.formatted(serviceLine.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultDurationMinutes").value(90));

        VisitType created = visitTypeRepository.findByAgency_IdAndCode(agency.getId(), "PD-EXT").orElseThrow();

        mockMvc.perform(put("/api/visit-types/{visitTypeId}", created.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId": "%s",
                                  "name": "Private Duty Extended Updated",
                                  "code": "PD-EXT-2",
                                  "description": "Updated",
                                  "defaultDurationMinutes": 75,
                                  "billable": false,
                                  "displayOrder": 3
                                }
                                """.formatted(serviceLine.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billable").value(false));

        mockMvc.perform(delete("/api/visit-types/{visitTypeId}", existing.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void invalidVisitTypeReferenceReturnsNotFound() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);

        mockMvc.perform(post("/api/visit-types")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId": "%s",
                                  "name": "Private Duty Extended",
                                  "code": "PD-EXT",
                                  "description": "Longer visit",
                                  "defaultDurationMinutes": 90,
                                  "billable": true,
                                  "displayOrder": 2
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void caregiverIsForbiddenAndVisitTypeInUseCannotBeDeactivated() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agency, "Private Duty", "PD", "Private duty", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(
                VisitType.create(agency, serviceLine, "Private Duty Standard", "PD-STD", "Default", 60, true, 1));
        taskTemplateRepository.saveAndFlush(
                TaskTemplate.create(agency, serviceLine, visitType, "Medication Reminder", "MED", "Task", TaskTemplateCategory.CLINICAL, 1));

        mockMvc.perform(get("/api/visit-types")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/visit-types/{visitTypeId}", visitType.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isConflict());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }
}
