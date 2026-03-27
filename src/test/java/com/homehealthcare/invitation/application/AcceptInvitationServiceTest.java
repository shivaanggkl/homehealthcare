package com.homehealthcare.invitation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.invitation.domain.UserInvitation;
import com.homehealthcare.invitation.domain.UserInvitationRepository;
import com.homehealthcare.invitation.domain.UserInvitationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.auth.application.WeakPasswordException;
import com.homehealthcare.auth.application.PasswordReuseNotAllowedException;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AcceptInvitationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AcceptInvitationService acceptInvitationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void exposesInvitationDetailsForPendingToken() {
        UserInvitation invitation = createPendingInvitation();

        AcceptInvitationService.InvitationDetails details = acceptInvitationService.getInvitationDetails(invitation.getToken());

        assertThat(details.invitationId()).isEqualTo(invitation.getId());
        assertThat(details.email()).isEqualTo(invitation.getEmail());
        assertThat(details.role()).isEqualTo(AgencyRole.SCHEDULER_COORDINATOR);
        assertThat(details.expiresAt()).isEqualTo(invitation.getExpiresAt());
    }

    @Test
    void acceptsInvitationActivatesUserSetsPasswordAndAudits() {
        UserInvitation invitation = createPendingInvitation();

        AcceptInvitationService.AcceptedInvitationResult result = acceptInvitationService.acceptInvitation(
                invitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey Updated",
                        "Scheduler Updated",
                        "+1 312 555 0199",
                        "S3cureInvitePassword!"));

        User updatedUser = userRepository.findById(result.userId()).orElseThrow();
        UserInvitation acceptedInvitation = userInvitationRepository.findById(result.invitationId()).orElseThrow();

        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updatedUser.getFirstName()).isEqualTo("Casey Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Scheduler Updated");
        assertThat(updatedUser.getPhone()).isEqualTo("+1 312 555 0199");
        assertThat(updatedUser.hasPasswordHash()).isTrue();
        assertThat(passwordEncoder.matches("S3cureInvitePassword!", updatedUser.getPasswordHash())).isTrue();
        assertThat(acceptedInvitation.getStatus()).isEqualTo(UserInvitationStatus.ACCEPTED);
        assertThat(acceptedInvitation.getAcceptedAt()).isNotNull();

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(updatedUser.getId());
        assertThat(events).extracting(AuditEvent::getActionType).contains("INVITATION_ACCEPTED");
    }

    @Test
    void rejectsExpiredInvitationTokenCleanly() {
        UserInvitation invitation = createPendingInvitation();
        forceExpire(invitation);

        assertThatThrownBy(() -> acceptInvitationService.acceptInvitation(
                invitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "S3cureInvitePassword!")))
                .isInstanceOf(InvitationExpiredException.class);

        assertThat(userInvitationRepository.findById(invitation.getId()).orElseThrow().getStatus())
                .isEqualTo(UserInvitationStatus.EXPIRED);
    }

    @Test
    void rejectsInvalidOrAlreadyUsedTokenCleanly() {
        assertThatThrownBy(() -> acceptInvitationService.getInvitationDetails("missing-token"))
                .isInstanceOf(InvitationNotFoundException.class);

        UserInvitation invitation = createPendingInvitation();
        acceptInvitationService.acceptInvitation(
                invitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "S3cureInvitePassword!"));

        assertThatThrownBy(() -> acceptInvitationService.acceptInvitation(
                invitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "AnotherPassword!")))
                .isInstanceOf(InvitationAlreadyUsedException.class);
    }

    @Test
    void rejectsCommonOrReusedInvitationPasswords() {
        UserInvitation invitation = createPendingInvitation();

        assertThatThrownBy(() -> acceptInvitationService.acceptInvitation(
                invitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "Password123!")))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessage("Password is too common or compromised. Choose a less predictable password");

        User invitedUser = invitation.getUser();
        invitedUser.activateWithCredentials(passwordEncoder.encode("S3cureInvitePassword!"));
        userRepository.saveAndFlush(invitedUser);

        UserInvitation secondInvitation = createPendingInvitationForUser(invitedUser, invitation.getAgencyMembership());

        assertThatThrownBy(() -> acceptInvitationService.acceptInvitation(
                secondInvitation.getToken(),
                new AcceptInvitationService.AcceptInvitationCommand(
                        "Casey",
                        "Scheduler",
                        null,
                        "S3cureInvitePassword!")))
                .isInstanceOf(PasswordReuseNotAllowedException.class)
                .hasMessage("Password cannot match any of your last 5 passwords");
    }

    private UserInvitation createPendingInvitation() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User inviter = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner." + UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership inviterMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(inviter, agency, AgencyRole.AGENCY_OWNER));
        User invitedUser = userRepository.saveAndFlush(
                User.invite("Casey", "Scheduler", "casey.scheduler." + UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership invitedMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(invitedUser, agency, AgencyRole.SCHEDULER_COORDINATOR));

        return userInvitationRepository.saveAndFlush(UserInvitation.issue(
                inviterMembership,
                invitedMembership,
                invitedUser,
                invitedUser.getEmail(),
                UUID.randomUUID().toString(),
                OffsetDateTime.now().plusDays(7)));
    }

    private UserInvitation createPendingInvitationForUser(User invitedUser, AgencyMembership invitedMembership) {
        Agency agency = invitedMembership.getAgency();
        User inviter = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner." + UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership inviterMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(inviter, agency, AgencyRole.AGENCY_OWNER));

        return userInvitationRepository.saveAndFlush(UserInvitation.issue(
                inviterMembership,
                invitedMembership,
                invitedUser,
                invitedUser.getEmail(),
                UUID.randomUUID().toString(),
                OffsetDateTime.now().plusDays(7)));
    }

    private void forceExpire(UserInvitation invitation) {
        invitation.rescheduleExpiration(OffsetDateTime.now().minusMinutes(1));
        userInvitationRepository.saveAndFlush(invitation);
    }
}
