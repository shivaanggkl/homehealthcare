package com.homehealthcare.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(UserStatusManagementServiceTest.TestConfig.class)
class UserStatusManagementServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserStatusManagementService userStatusManagementService;

    @Autowired
    private UserAuthenticationPolicy userAuthenticationPolicy;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RecordingUserSessionService recordingUserSessionService;

    @BeforeEach
    void resetSessions() {
        recordingUserSessionService.clear();
    }

    @Test
    void adminCanActivateLockSuspendAndDeactivateUserWithAudit() {
        Fixture fixture = createFixture(AgencyRole.AGENCY_OWNER);

        User active = userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.ACTIVE);
        assertThat(active.getStatus()).isEqualTo(UserStatus.ACTIVE);

        User locked = userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.LOCKED);
        assertThat(locked.getStatus()).isEqualTo(UserStatus.LOCKED);

        User suspended = userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.SUSPENDED);
        assertThat(suspended.getStatus()).isEqualTo(UserStatus.SUSPENDED);

        User deactivated = userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.DEACTIVATED);
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(deactivated.getDeactivatedAt()).isNotNull();

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(fixture.actorMembership().getId());
        assertThat(events).filteredOn(event -> event.getActionType().equals("USER_STATUS_CHANGED")).hasSize(4);
    }

    @Test
    void deactivatedUsersCannotSignInAndSuspendedUsersLoseActiveSessions() {
        Fixture fixture = createFixture(AgencyRole.AGENCY_OWNER);
        fixture.targetUser().activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(fixture.targetUser());

        userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.SUSPENDED);

        assertThat(recordingUserSessionService.revocations())
                .containsExactly(new RecordingUserSessionService.Revocation(fixture.targetUser().getId(), "USER_SUSPENDED"));
        assertThatThrownBy(() -> userAuthenticationPolicy.requireCanSignIn(fixture.targetUser()))
                .isInstanceOf(UserSignInBlockedException.class);

        userStatusManagementService.changeStatus(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                UserStatus.DEACTIVATED);

        User deactivated = userRepository.findById(fixture.targetUser().getId()).orElseThrow();
        assertThatThrownBy(() -> userAuthenticationPolicy.requireCanSignIn(deactivated))
                .isInstanceOf(UserSignInBlockedException.class);
    }

    @Test
    void rejectsUnauthorizedOrCrossAgencyStatusChanges() {
        Fixture caregiverActor = createFixture(AgencyRole.CAREGIVER);

        assertThatThrownBy(() -> userStatusManagementService.changeStatus(
                caregiverActor.actorMembership(),
                caregiverActor.targetUser().getId(),
                UserStatus.SUSPENDED))
                .isInstanceOf(UnauthorizedUserStatusActorException.class);

        Agency secondAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", UUID.randomUUID().toString(), "America/New_York", "ops@sunrise.example"));
        User outsider = userRepository.saveAndFlush(User.invite("Other", "User", UUID.randomUUID() + "@example.com", null));
        outsider.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(outsider);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(outsider, secondAgency, AgencyRole.CAREGIVER));

        Fixture ownerActor = createFixture(AgencyRole.AGENCY_OWNER);
        assertThatThrownBy(() -> userStatusManagementService.changeStatus(
                ownerActor.actorMembership(),
                outsider.getId(),
                UserStatus.SUSPENDED))
                .isInstanceOf(UserNotManageableInAgencyException.class);
    }

    private Fixture createFixture(AgencyRole actorRole) {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User actor = userRepository.saveAndFlush(User.invite("Alicia", "Admin", UUID.randomUUID() + "@northstar.example", null));
        actor.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        actor = userRepository.saveAndFlush(actor);
        AgencyMembership actorMembership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(actor, agency, actorRole));

        User target = userRepository.saveAndFlush(User.invite("Casey", "Staff", UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership targetMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(target, agency, AgencyRole.CAREGIVER));
        targetMembership.getId();

        return new Fixture(actorMembership, target);
    }

    record Fixture(AgencyMembership actorMembership, User targetUser) {
    }

    static class RecordingUserSessionService implements UserSessionService {

        record Revocation(UUID userId, String reason) {
        }

        private final List<Revocation> revocations = new ArrayList<>();

        @Override
        public void revokeAllSessions(UUID userId, String reason) {
            revocations.add(new Revocation(userId, reason));
        }

        List<Revocation> revocations() {
            return revocations;
        }

        void clear() {
            revocations.clear();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingUserSessionService recordingUserSessionService() {
            return new RecordingUserSessionService();
        }
    }
}
