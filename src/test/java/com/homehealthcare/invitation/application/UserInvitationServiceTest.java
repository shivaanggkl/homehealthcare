package com.homehealthcare.invitation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.invitation.domain.UserInvitation;
import com.homehealthcare.invitation.domain.UserInvitationRepository;
import com.homehealthcare.invitation.domain.UserInvitationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(UserInvitationServiceTest.TestConfig.class)
class UserInvitationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchAssignmentRepository branchAssignmentRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private RecordingInvitationEmailSender recordingInvitationEmailSender;

    @BeforeEach
    void resetEmailSink() {
        recordingInvitationEmailSender.clear();
    }

    @Test
    void sendsInviteCreatesUserMembershipAndCapturesOptionalBranchAssignmentsWithAudit() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(
                createUser("Alicia", "Owner", "alicia.owner@northstar.example"),
                agency,
                AgencyRole.AGENCY_OWNER);
        Branch firstBranch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch secondBranch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));

        UserInvitationService.InviteUserResult result = userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        "Casey",
                        "Scheduler",
                        "casey.scheduler@northstar.example",
                        "+1 312 555 0101",
                        AgencyRole.SCHEDULER_COORDINATOR,
                        Set.of(firstBranch.getId(), secondBranch.getId())));

        User invitedUser = userRepository.findByEmail("casey.scheduler@northstar.example").orElseThrow();
        assertThat(invitedUser.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(result.membership().getUserId()).isEqualTo(invitedUser.getId());
        assertThat(result.membership().getRole()).isEqualTo(AgencyRole.SCHEDULER_COORDINATOR);
        assertThat(result.membership().isActive()).isTrue();
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                result.membership().getId(),
                com.homehealthcare.branchassignment.domain.BranchAssignmentStatus.ACTIVE)).hasSize(2);

        UserInvitation invitation = result.invitation();
        assertThat(invitation.isPending()).isTrue();
        assertThat(invitation.getExpiresAt()).isAfter(OffsetDateTime.now().plusDays(6));
        assertThat(recordingInvitationEmailSender.sentEmails()).hasSize(1);
        assertThat(recordingInvitationEmailSender.sentEmails().getFirst().recipientEmail())
                .isEqualTo("casey.scheduler@northstar.example");

        List<AuditEvent> auditEvents = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(actorMembership.getId());
        assertThat(auditEvents).extracting(AuditEvent::getActionType).contains("USER_INVITED");
    }

    @Test
    void duplicatePendingInviteIsCancelledAndReissued() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(
                createUser("Alicia", "Owner", "alicia.owner." + UUID.randomUUID() + "@northstar.example"),
                agency,
                AgencyRole.AGENCY_OWNER);

        UserInvitation firstInvitation = userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        "Casey",
                        "Scheduler",
                        "casey.scheduler@northstar.example",
                        null,
                        AgencyRole.SCHEDULER_COORDINATOR,
                        Set.of())).invitation();

        UserInvitation secondInvitation = userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        "Casey",
                        "Scheduler",
                        "casey.scheduler@northstar.example",
                        null,
                        AgencyRole.SCHEDULER_COORDINATOR,
                        Set.of())).invitation();

        UserInvitation cancelledFirst = userInvitationRepository.findById(firstInvitation.getId()).orElseThrow();
        assertThat(cancelledFirst.getStatus()).isEqualTo(UserInvitationStatus.CANCELLED);
        assertThat(secondInvitation.getStatus()).isEqualTo(UserInvitationStatus.PENDING);
        assertThat(secondInvitation.getToken()).isNotEqualTo(firstInvitation.getToken());
        assertThat(recordingInvitationEmailSender.sentEmails()).hasSize(2);
    }

    @Test
    void rejectsInviteWhenUserAlreadyActiveMemberOfAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(
                createUser("Alicia", "Owner", "alicia.owner." + UUID.randomUUID() + "@northstar.example"),
                agency,
                AgencyRole.AGENCY_OWNER);
        User existingUser = createUser("Casey", "Scheduler", "casey.scheduler." + UUID.randomUUID() + "@northstar.example");
        existingUser.activate();
        userRepository.saveAndFlush(existingUser);
        createMembership(existingUser, agency, AgencyRole.SCHEDULER_COORDINATOR);

        assertThatThrownBy(() -> userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        "Casey",
                        "Scheduler",
                        existingUser.getEmail(),
                        null,
                        AgencyRole.SCHEDULER_COORDINATOR,
                        Set.of())))
                .isInstanceOf(UserAlreadyAgencyMemberException.class);
    }

    @Test
    void rejectsNonAdminInviter() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(
                createUser("Casey", "Caregiver", "casey.caregiver." + UUID.randomUUID() + "@northstar.example"),
                agency,
                AgencyRole.CAREGIVER);

        assertThatThrownBy(() -> userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        "Jordan",
                        "Nurse",
                        "jordan.nurse@northstar.example",
                        null,
                        AgencyRole.CAREGIVER,
                        Set.of())))
                .isInstanceOf(UnauthorizedInvitationActorException.class);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName, String email) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, email, null));
    }

    static class RecordingInvitationEmailSender implements InvitationEmailSender {

        private final List<InvitationEmail> sentEmails = new ArrayList<>();

        @Override
        public void send(InvitationEmail email) {
            sentEmails.add(email);
        }

        List<InvitationEmail> sentEmails() {
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
        RecordingInvitationEmailSender recordingInvitationEmailSender() {
            return new RecordingInvitationEmailSender();
        }
    }
}
