package com.homehealthcare.tasktemplate.api;

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
class TaskTemplateIntegrationTest {

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
    void branchAdminCanListFilterAndCrudTaskTemplates() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agency, "Private Duty", "PD", "Private duty", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(
                VisitType.create(agency, serviceLine, "Standard Visit", "STD", "Default", 60, true, 1));
        TaskTemplate existing = taskTemplateRepository.saveAndFlush(
                TaskTemplate.create(agency, serviceLine, visitType, "Medication Reminder", "MED", "Task", TaskTemplateCategory.CLINICAL, 1));

        mockMvc.perform(get("/api/task-templates")
                        .param("category", "CLINICAL")
                        .param("serviceLineId", serviceLine.getId().toString())
                        .param("visitTypeId", visitType.getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Medication Reminder"));

        mockMvc.perform(post("/api/task-templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "name": "Vitals Check",
                                  "code": "VITALS",
                                  "description": "Collect vitals",
                                  "category": "CLINICAL",
                                  "displayOrder": 2
                                }
                                """.formatted(serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VITALS"));

        TaskTemplate created = taskTemplateRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId())
                .stream().filter(item -> item.getCode().equals("VITALS")).findFirst().orElseThrow();

        mockMvc.perform(put("/api/task-templates/{taskTemplateId}", created.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceLineId": "%s",
                                  "visitTypeId": "%s",
                                  "name": "Vitals Check Updated",
                                  "code": "VITALS-2",
                                  "description": "Updated",
                                  "category": "CLINICAL",
                                  "displayOrder": 3
                                }
                                """.formatted(serviceLine.getId(), visitType.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vitals Check Updated"));

        mockMvc.perform(delete("/api/task-templates/{taskTemplateId}", existing.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/task-templates")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }
}
