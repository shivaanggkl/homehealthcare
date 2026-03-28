package com.homehealthcare.alertrule.api;

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
import com.homehealthcare.alertrule.domain.AlertRule;
import com.homehealthcare.alertrule.domain.AlertRuleRepository;
import com.homehealthcare.alertrule.domain.AlertRuleType;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
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
class AlertRuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Test
    void branchAdminCanCrudAgencyAndBranchScopedAlertRules() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago", "CHI", "123 Main", "America/Chicago"));
        AlertRule existing = alertRuleRepository.saveAndFlush(
                AlertRule.create(agency, null, "Missed Visit Alert", AlertRuleType.MISSED_VISIT, "{\"minutes\":15}", true, false, true, 1));

        mockMvc.perform(get("/api/alert-rules")
                        .param("ruleType", "MISSED_VISIT")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].branchSpecific").value(false));

        mockMvc.perform(post("/api/alert-rules")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "name": "Late Arrival Alert",
                                  "ruleType": "LATE_ARRIVAL",
                                  "configPayloadJson": "{\\\"minutesLate\\\":10}",
                                  "notifyEmail": true,
                                  "notifySms": false,
                                  "notifyInApp": true,
                                  "displayOrder": 2
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchSpecific").value(true));

        AlertRule created = alertRuleRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId())
                .stream().filter(item -> item.getName().equals("Late Arrival Alert")).findFirst().orElseThrow();

        mockMvc.perform(put("/api/alert-rules/{alertRuleId}", created.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "name": "Late Arrival Alert Updated",
                                  "ruleType": "LATE_ARRIVAL",
                                  "configPayloadJson": "{\\\"minutesLate\\\":15}",
                                  "notifyEmail": true,
                                  "notifySms": true,
                                  "notifyInApp": true,
                                  "displayOrder": 3
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifySms").value(true));

        mockMvc.perform(delete("/api/alert-rules/{alertRuleId}", existing.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void malformedAlertPayloadReturnsBadRequest() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);

        mockMvc.perform(post("/api/alert-rules")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bad Alert",
                                  "ruleType": "MISSED_VISIT",
                                  "configPayloadJson": "{bad json}",
                                  "notifyEmail": true,
                                  "notifySms": false,
                                  "notifyInApp": true,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void caregiverIsForbiddenForAlertRuleApis() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/alert-rules")
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
