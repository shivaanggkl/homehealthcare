package com.homehealthcare.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.notification.domain.AdminNotificationPreference;
import com.homehealthcare.notification.domain.AdminNotificationPreferenceRepository;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.platform.email.SecurityAlertEmailSender;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.application.UserProfileAssignmentService;
import com.homehealthcare.user.application.UserStatusManagementService;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AdminSecurityNotificationIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "security.login-protection.failure-threshold=3",
        "security.login-protection.failure-window=15m",
        "security.login-protection.temporary-lockout-duration=15m",
        "security.login-protection.max-attempts-per-ip=100",
        "security.login-protection.max-attempts-per-email=100",
        "security.login-protection.rate-limit-window=1m"
})
class AdminSecurityNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AdminNotificationPreferenceRepository adminNotificationPreferenceRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserStatusManagementService userStatusManagementService;

    @Autowired
    private UserProfileAssignmentService userProfileAssignmentService;

    @Autowired
    private RecordingSecurityAlertEmailSender recordingSecurityAlertEmailSender;

    @BeforeEach
    void resetEmailSink() {
        recordingSecurityAlertEmailSender.clear();
    }

    @Test
    void repeatedFailedLoginSendsOptionalNotificationAndAuditsDelivery() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership adminMembership = createMembership(createActiveUser("Alicia", "Owner", "alicia.owner@example.com"), agency, AgencyRole.AGENCY_OWNER);
        User targetUser = createActiveUser("Casey", "Caregiver", "casey.staff@example.com");
        targetUser.updatePassword(passwordEncoder.encode("CorrectPassword1!"));
        userRepository.saveAndFlush(targetUser);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(targetUser, agency, AgencyRole.CAREGIVER));

        AdminNotificationPreference preference = adminNotificationPreferenceRepository.saveAndFlush(AdminNotificationPreference.defaultsFor(adminMembership));
        preference.update(true, true, false, false);
        adminNotificationPreferenceRepository.saveAndFlush(preference);

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .header("X-Forwarded-For", "203.0.113.10")
                            .content("""
                                    {
                                      "email": "casey.staff@example.com",
                                      "password": "WrongPassword1!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        assertThat(recordingSecurityAlertEmailSender.sentEmails()).singleElement().satisfies(email -> {
            assertThat(email.recipientEmail()).isEqualTo("alicia.owner@example.com");
            assertThat(email.subject()).contains("Repeated failed login detected");
        });
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(adminMembership.getId()))
                .filteredOn(event -> event.getActionType().equals("ADMIN_SECURITY_NOTIFICATION_SENT"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("REPEATED_FAILED_LOGIN"));
    }

    @Test
    void lockedAccountAndNewAdminCreatedRespectPreferencesAndAreAudited() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@sunrise.example"));
        AgencyMembership adminMembership = createMembership(createActiveUser("Jordan", "Owner", "jordan.owner@example.com"), agency, AgencyRole.AGENCY_OWNER);
        User targetUser = createActiveUser("Taylor", "Staff", "taylor.staff@example.com");
        AgencyMembership targetMembership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(targetUser, agency, AgencyRole.CAREGIVER));

        AdminNotificationPreference preference = adminNotificationPreferenceRepository.saveAndFlush(AdminNotificationPreference.defaultsFor(adminMembership));
        preference.update(true, false, true, true);
        adminNotificationPreferenceRepository.saveAndFlush(preference);

        userStatusManagementService.changeStatus(adminMembership, targetUser.getId(), UserStatus.LOCKED);
        userProfileAssignmentService.updateUser(
                adminMembership,
                targetUser.getId(),
                new UserProfileAssignmentService.UpdateUserCommand(
                        "Taylor",
                        "Staff",
                        null,
                        AgencyRole.BRANCH_ADMIN,
                        Set.of()));

        assertThat(recordingSecurityAlertEmailSender.sentEmails()).hasSize(2);
        assertThat(recordingSecurityAlertEmailSender.sentEmails())
                .extracting(SecurityAlertEmailSender.SecurityAlertEmail::subject)
                .anyMatch(subject -> subject.contains("Account locked"))
                .anyMatch(subject -> subject.contains("New admin created"));
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(adminMembership.getId()))
                .filteredOn(event -> event.getActionType().equals("ADMIN_SECURITY_NOTIFICATION_SENT"))
                .hasSize(2);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createActiveUser(String firstName, String lastName, String email) {
        User user = userRepository.saveAndFlush(User.invite(firstName, lastName, email, null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        return userRepository.saveAndFlush(user);
    }

    static class RecordingSecurityAlertEmailSender implements SecurityAlertEmailSender {

        private final List<SecurityAlertEmail> sentEmails = new ArrayList<>();

        @Override
        public void send(SecurityAlertEmail email) {
            sentEmails.add(email);
        }

        List<SecurityAlertEmail> sentEmails() {
            return sentEmails;
        }

        void clear() {
            sentEmails.clear();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingSecurityAlertEmailSender recordingSecurityAlertEmailSender() {
            return new RecordingSecurityAlertEmailSender();
        }
    }
}
