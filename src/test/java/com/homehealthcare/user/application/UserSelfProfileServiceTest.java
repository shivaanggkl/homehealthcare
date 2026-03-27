package com.homehealthcare.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserSelfProfileServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserSelfProfileService userSelfProfileService;

    @Test
    void userCanUpdateOwnBasicProfileLanguageAndTimeZoneWithAudit() {
        User user = userRepository.saveAndFlush(
                User.invite("Casey", "Scheduler", "casey." + UUID.randomUUID() + "@northstar.example", null));
        user.activateWithCredentials("hash");
        userRepository.saveAndFlush(user);

        UserSelfProfileService.UpdatedSelfProfileResult result = userSelfProfileService.updateOwnProfile(
                user,
                new UserSelfProfileService.UpdateSelfProfileCommand(
                        "Casey Updated",
                        "Scheduler Updated",
                        "+1 312 555 0177",
                        "en-us",
                        "America/Chicago"));

        User updated = userRepository.findById(result.userId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Casey Updated");
        assertThat(updated.getLastName()).isEqualTo("Scheduler Updated");
        assertThat(updated.getPhone()).isEqualTo("+1 312 555 0177");
        assertThat(updated.getPreferredLanguage()).isEqualTo("en-US");
        assertThat(updated.getTimeZone()).isEqualTo("America/Chicago");

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(updated.getId());
        assertThat(events).extracting(AuditEvent::getActionType).contains("USER_SELF_PROFILE_UPDATED");
    }

    @Test
    void validationIsEnforcedForLanguageAndTimeZoneAndRoleCannotBeElevated() {
        User user = userRepository.saveAndFlush(
                User.invite("Casey", "Scheduler", "casey." + UUID.randomUUID() + "@northstar.example", null));
        user.activateWithCredentials("hash");
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> userSelfProfileService.updateOwnProfile(
                user,
                new UserSelfProfileService.UpdateSelfProfileCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "not-a-language",
                        "America/Chicago")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid preferred language");

        assertThatThrownBy(() -> userSelfProfileService.updateOwnProfile(
                user,
                new UserSelfProfileService.UpdateSelfProfileCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "en-US",
                        "Mars/Olympus")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown time-zone ID");

        assertThat(user.getClass().getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("role");
        assertThat(AgencyRole.values()).contains(AgencyRole.AGENCY_OWNER);
    }
}
