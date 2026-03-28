package com.homehealthcare.mileagepay.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mileagepay.domain.MileagePaySettingRepository;
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
class MileagePaySettingIntegrationTest {

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
    private MileagePaySettingRepository mileagePaySettingRepository;

    @Test
    void ownerCanReadAndUpsertAgencyDefaultAndBranchOverrideSettings() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership owner = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago", "CHI", "123 Main", "America/Chicago"));

        mockMvc.perform(put("/api/mileage-pay-settings/default")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reimbursementStrategy": "STANDARD_RATE",
                                  "mileageRate": 0.6700,
                                  "travelPayEnabled": true,
                                  "visitTypePayAdjustmentsJson": "{\\\"STD\\\":{\\\"perVisit\\\":5.00}}",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchOverride").value(false))
                .andExpect(jsonPath("$.reimbursementStrategy").value("STANDARD_RATE"));

        mockMvc.perform(put("/api/mileage-pay-settings/branches/{branchId}", branch.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reimbursementStrategy": "CUSTOM_RATE",
                                  "mileageRate": 0.7200,
                                  "travelPayEnabled": false,
                                  "visitTypePayAdjustmentsJson": null,
                                  "displayOrder": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchOverride").value(true))
                .andExpect(jsonPath("$.branchId").value(branch.getId().toString()));

        mockMvc.perform(get("/api/mileage-pay-settings")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyDefault.reimbursementStrategy").value("STANDARD_RATE"))
                .andExpect(jsonPath("$.branchOverrides[0].branchId").value(branch.getId().toString()));
    }

    @Test
    void malformedMileageAdjustmentsReturnBadRequest() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership owner = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(put("/api/mileage-pay-settings/default")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reimbursementStrategy": "STANDARD_RATE",
                                  "mileageRate": 0.6700,
                                  "travelPayEnabled": true,
                                  "visitTypePayAdjustmentsJson": "{bad json}",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void caregiverIsForbiddenForMileagePayApis() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/mileage-pay-settings")
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
