package com.homehealthcare.platform.provisioning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.admin.domain.InternalSuperAdminRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapRepository;
import com.homehealthcare.platform.provisioning.domain.AgencyOwnerBootstrapStatus;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.dev-bootstrap.owner.enabled=true",
        "app.dev-bootstrap.owner.internal-super-admin-email=ops@platform.local",
        "app.dev-bootstrap.owner.agency-name=North Star Home Care",
        "app.dev-bootstrap.owner.agency-slug=north-star-home-care",
        "app.dev-bootstrap.owner.agency-timezone=America/Chicago",
        "app.dev-bootstrap.owner.agency-contact-email=hello@northstar.example",
        "app.dev-bootstrap.owner.owner-first-name=Alicia",
        "app.dev-bootstrap.owner.owner-last-name=Owner",
        "app.dev-bootstrap.owner.owner-email=alicia.owner@northstar.example",
        "app.dev-bootstrap.owner.owner-phone=+1 312 555 0101",
        "app.dev-bootstrap.owner.owner-password=StartPassword1!"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DevAgencyOwnerBootstrapRunnerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private InternalSuperAdminRepository internalSuperAdminRepository;

    @Autowired
    private AgencyOwnerBootstrapRepository agencyOwnerBootstrapRepository;

    @Autowired
    private DevAgencyOwnerBootstrapRunner runner;

    @Test
    void seedsLoginableAgencyOwnerOnStartup() throws Exception {
        Agency agency = agencyRepository.findBySlug("north-star-home-care").orElseThrow();
        User owner = userRepository.findByEmail("alicia.owner@northstar.example").orElseThrow();
        AgencyMembership membership = agencyMembershipRepository.findByUser_IdAndAgency_Id(owner.getId(), agency.getId()).orElseThrow();

        assertThat(internalSuperAdminRepository.findByEmail("ops@platform.local")).isPresent();
        assertThat(owner.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(owner.hasPasswordHash()).isTrue();
        assertThat(membership.getRole()).isEqualTo(AgencyRole.AGENCY_OWNER);
        assertThat(membership.isActive()).isTrue();
        assertThat(agencyOwnerBootstrapRepository.findAllByAgency_IdOrderByCreatedAtAsc(agency.getId()))
                .singleElement()
                .satisfies(bootstrap -> {
                    assertThat(bootstrap.getOwnerEmail()).isEqualTo("alicia.owner@northstar.example");
                    assertThat(bootstrap.getStatus()).isEqualTo(AgencyOwnerBootstrapStatus.COMPLETED);
                });

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alicia.owner@northstar.example",
                                  "password": "StartPassword1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void rerunningSeederIsIdempotent() throws Exception {
        long initialAgencyCount = agencyRepository.count();
        long initialUserCount = userRepository.count();
        long initialMembershipCount = agencyMembershipRepository.count();
        long initialBootstrapCount = agencyOwnerBootstrapRepository.count();

        runner.run(new DefaultApplicationArguments(new String[0]));

        Agency agency = agencyRepository.findBySlug("north-star-home-care").orElseThrow();
        User owner = userRepository.findByEmail("alicia.owner@northstar.example").orElseThrow();

        assertThat(agencyRepository.count()).isEqualTo(initialAgencyCount);
        assertThat(userRepository.count()).isEqualTo(initialUserCount);
        assertThat(agencyMembershipRepository.count()).isEqualTo(initialMembershipCount);
        assertThat(agencyOwnerBootstrapRepository.count()).isEqualTo(initialBootstrapCount);
        assertThat(agencyMembershipRepository.findByUser_IdAndAgency_Id(owner.getId(), agency.getId()))
                .hasValueSatisfying(membership -> {
                    assertThat(membership.getRole()).isEqualTo(AgencyRole.AGENCY_OWNER);
                    assertThat(membership.isActive()).isTrue();
                });
    }
}
