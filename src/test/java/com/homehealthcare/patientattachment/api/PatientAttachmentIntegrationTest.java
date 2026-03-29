package com.homehealthcare.patientattachment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientAttachmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientAttachmentRepository patientAttachmentRepository;

    @Test
    void uploadListUpdateAndDownloadAttachment() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership schedulerMembership = createMembership(createUser("Alicia", "Scheduler"), agency, AgencyRole.SCHEDULER_COORDINATOR);
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-001",
                "Shiva",
                null,
                "Kl",
                null,
                LocalDate.of(1990, 1, 15),
                "female",
                null,
                null,
                "shiva@example.com",
                "en-US",
                null));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../plan-of-care.pdf",
                "application/pdf",
                "hello attachment".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/patients/{patientId}/attachments", patient.getId())
                        .file(file)
                        .param("category", "PLAN_OF_CARE")
                        .param("description", "Initial plan")
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patient.getId().toString()))
                .andExpect(jsonPath("$.fileName").value("plan-of-care.pdf"))
                .andExpect(jsonPath("$.category").value("PLAN_OF_CARE"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"));

        PatientAttachment attachment = patientAttachmentRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/patients/{patientId}/attachments", patient.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attachment.getId().toString()))
                .andExpect(jsonPath("$[0].uploaderMembershipId").value(schedulerMembership.getId().toString()));

        mockMvc.perform(put("/api/patient-attachments/{attachmentId}", attachment.getId())
                        .param("category", "INTAKE_PACKET")
                        .param("description", "Updated metadata")
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("INTAKE_PACKET"))
                .andExpect(jsonPath("$.description").value("Updated metadata"));

        mockMvc.perform(get("/api/patient-attachments/{attachmentId}/download", attachment.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("plan-of-care.pdf")))
                .andExpect(content().bytes("hello attachment".getBytes(StandardCharsets.UTF_8)));

        assertThat(patientAttachmentRepository.countByPatient_Id(patient.getId())).isEqualTo(1);
    }

    @Test
    void unauthorizedDownloadAndInvalidUploadAreBlocked() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership schedulerMembership = createMembership(createUser("Alicia", "Scheduler"), agency, AgencyRole.SCHEDULER_COORDINATOR);
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-001",
                "Shiva",
                null,
                "Kl",
                null,
                LocalDate.of(1990, 1, 15),
                "female",
                null,
                null,
                "shiva@example.com",
                "en-US",
                null));

        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "clinical-note.txt",
                "text/plain",
                "clinical note".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/patients/{patientId}/attachments", patient.getId())
                        .file(validFile)
                        .param("category", "CLINICAL_NOTE")
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isOk());

        PatientAttachment attachment = patientAttachmentRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/patient-attachments/{attachmentId}/download", attachment.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/patients/{patientId}/attachments", patient.getId())
                        .file(invalidFile)
                        .param("category", "OTHER")
                        .with(authentication(TestTenantAuthentications.authenticationFor(schedulerMembership))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Attachment content type is not allowed"));
    }

    private User createUser(String firstName, String lastName) {
        User user = userRepository.saveAndFlush(User.invite(
                firstName,
                lastName,
                (firstName + "." + lastName + "+" + UUID.randomUUID() + "@example.com").toLowerCase(),
                "555-0101"));
        user.activateWithCredentials("{noop}Password123!");
        return userRepository.saveAndFlush(user);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
