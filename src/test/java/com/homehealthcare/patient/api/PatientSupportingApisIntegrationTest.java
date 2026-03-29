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
class PatientSupportingApisIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AgencyRepository agencyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ServiceLineRepository serviceLineRepository;

    @Test
    void contactAddressEligibilityDiagnosisPayerAndAuthorizationApisWork() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership scheduler = createMembership(createUser("Alicia", "Scheduler"), agency, AgencyRole.SCHEDULER_COORDINATOR);
        AgencyMembership billing = createMembership(createUser("Bela", "Billing"), agency, AgencyRole.BILLING_BACK_OFFICE);
        AgencyMembership qa = createMembership(createUser("Quinn", "Qa"), agency, AgencyRole.QA_CLINICAL_REVIEWER);
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-001", "Shiva", null, "Kl", "Shiv", LocalDate.of(1990, 1, 15), "female", null, null, "shiva@example.com", "en-US", null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Private Duty", "PD", "Companion care", 1));

        mockMvc.perform(post("/api/patients/{patientId}/contacts", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"relationshipType":"Sister","fullName":"Neha Kl","phone":"312-555-0102","email":"neha@example.com","address":"123 Main St","emergencyContact":true,"primaryContact":true,"responsibleParty":false,"notes":"Emergency contact"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryContact").value(true));

        mockMvc.perform(put("/api/patients/{patientId}/address", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"addressLine1":"123 Main St","addressLine2":"Unit 4","city":"Chicago","state":"IL","postalCode":"60601","country":"US","latitude":41.881832,"longitude":-87.623177,"geocodeStatus":"GEOCODED","timezone":"America/Chicago","locationNotes":"Buzz apartment before entering"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Chicago"));

        mockMvc.perform(post("/api/patients/{patientId}/eligibilities", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"serviceLineId":"%s","status":"ELIGIBLE","effectiveFrom":"2026-01-01","effectiveTo":"2026-12-31","verificationSource":"Intake review","notes":"Approved"}
                                """.formatted(serviceLine.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ELIGIBLE"));

        mockMvc.perform(post("/api/patients/{patientId}/diagnoses", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(qa)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"diagnosisCode":"I10","description":"Hypertension","diagnosisType":"Chronic","primaryCondition":true,"onsetDate":"2025-01-01","resolvedDate":null,"status":"ACTIVE","notes":"Monitor blood pressure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryCondition").value(true));

        String payerResponse = mockMvc.perform(post("/api/patients/{patientId}/payer-links", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"payerName":"Blue Cross","payerExternalId":null,"memberPolicyNumber":"POL-001","groupNumber":"GRP-1","effectiveFrom":"2026-01-01","effectiveTo":"2026-12-31","primaryPayer":true,"status":"ACTIVE","notes":"Primary commercial payer"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryPayer").value(true))
                .andReturn().getResponse().getContentAsString();
        String payerId = payerResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        String authorizationResponse = mockMvc.perform(post("/api/patients/{patientId}/authorizations", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"patientPayerLinkId":"%s","serviceLineId":"%s","authorizationNumber":"AUTH-001","startDate":"2026-01-01","endDate":"2026-06-30","authorizedUnits":40,"usedUnits":10,"status":"ACTIVE","notes":"Initial authorization"}
                                """.formatted(payerId, serviceLine.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizedUnits").value(40))
                .andReturn().getResponse().getContentAsString();
        String authorizationId = authorizationResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/patients/{patientId}/contacts", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Neha Kl"));

        mockMvc.perform(get("/api/patients/{patientId}/address", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postalCode").value("60601"));

        mockMvc.perform(get("/api/patients/{patientId}/eligibilities", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler)))
                        .param("serviceLineId", serviceLine.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceLineId").value(serviceLine.getId().toString()));

        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(qa)))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisCode").value("I10"));

        mockMvc.perform(get("/api/patients/{patientId}/payer-links", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing)))
                        .param("primaryPayer", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payerName").value("Blue Cross"));

        mockMvc.perform(get("/api/patients/{patientId}/authorizations", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing)))
                        .param("currentOnly", "true")
                        .param("serviceLineId", serviceLine.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorizationNumber").value("AUTH-001"));

        mockMvc.perform(delete("/api/patient-eligibilities/{eligibilityId}", extractIdFromListGet(
                        mockMvc.perform(get("/api/patients/{patientId}/eligibilities", patient.getId())
                                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                                .andReturn().getResponse().getContentAsString()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scheduler))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        mockMvc.perform(delete("/api/patient-diagnoses/{diagnosisId}", extractIdFromListGet(
                        mockMvc.perform(get("/api/patients/{patientId}/diagnoses", patient.getId())
                                        .with(authentication(TestTenantAuthentications.authenticationFor(qa))))
                                .andReturn().getResponse().getContentAsString()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(qa))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(delete("/api/patient-payer-links/{payerLinkId}", payerId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(delete("/api/patient-authorizations/{authorizationId}", authorizationId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(billing))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private static String extractIdFromListGet(String body) {
        return body.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
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
