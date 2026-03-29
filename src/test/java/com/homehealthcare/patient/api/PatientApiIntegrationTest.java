package com.homehealthcare.patient.api;

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
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.security.branch.AgencyRole;
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
class PatientApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired private PatientRepository patientRepository;

    @Test
    void patientCrudSearchAndUnauthorizedFlowWork() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership scheduler = createMembership(createUser("Alicia", "Scheduler"), agency, AgencyRole.SCHEDULER_COORDINATOR);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(post("/api/patients")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"externalReference":"PAT-001","firstName":"Shiva","middleName":null,"lastName":"Kl","preferredName":"Shiv","dateOfBirth":"1990-01-15","sexMarker":"female","primaryPhone":"312-555-0101","secondaryPhone":null,"email":"shiva@example.com","language":"en-US","notesSummary":"Prefers morning visits"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("PAT-001"));

        Patient patient = patientRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/patients")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("search", "shiv")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(patient.getId().toString()));

        mockMvc.perform(get("/api/patients/{patientId}", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patient.getId().toString()));

        mockMvc.perform(put("/api/patients/{patientId}", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"externalReference":"PAT-001","firstName":"Shivaangg","middleName":null,"lastName":"Kl","preferredName":"Shiv","dateOfBirth":"1990-01-15","sexMarker":"female","primaryPhone":"312-555-0101","secondaryPhone":null,"email":"shiva@example.com","language":"en-US","notesSummary":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Shivaangg"));

        mockMvc.perform(delete("/api/patients/{patientId}", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/patients")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());
    }

    private User createUser(String firstName, String lastName) {
        User user = userRepository.saveAndFlush(User.invite(firstName, lastName, (firstName + "." + lastName + "+" + UUID.randomUUID() + "@example.com").toLowerCase(), "555-0101"));
        user.activateWithCredentials("{noop}Password123!");
        return userRepository.saveAndFlush(user);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
