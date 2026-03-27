package com.homehealthcare.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserProfileAssignmentServiceTest {

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
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserProfileAssignmentService userProfileAssignmentService;

    @Test
    void adminCanEditBasicDetailsRoleAndBranchAssignments() {
        Fixture fixture = createFixture(AgencyRole.AGENCY_OWNER, AgencyRole.CAREGIVER);
        Branch firstBranch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "North Branch", "NB-01", "123 North St", "America/Chicago"));
        Branch secondBranch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "South Branch", "SB-01", "456 South St", "America/Chicago"));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(fixture.targetMembership(), firstBranch));

        UserProfileAssignmentService.UpdatedUserResult result = userProfileAssignmentService.updateUser(
                fixture.actorMembership(),
                fixture.targetUser().getId(),
                new UserProfileAssignmentService.UpdateUserCommand(
                        "Jordan",
                        "Coordinator",
                        "+1 312 555 0188",
                        AgencyRole.SCHEDULER_COORDINATOR,
                        Set.of(secondBranch.getId())));

        User updatedUser = userRepository.findById(result.userId()).orElseThrow();
        AgencyMembership updatedMembership = agencyMembershipRepository.findById(result.membershipId()).orElseThrow();

        assertThat(updatedUser.getFirstName()).isEqualTo("Jordan");
        assertThat(updatedUser.getLastName()).isEqualTo("Coordinator");
        assertThat(updatedUser.getPhone()).isEqualTo("+1 312 555 0188");
        assertThat(updatedMembership.getRole()).isEqualTo(AgencyRole.SCHEDULER_COORDINATOR);
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                updatedMembership.getId(),
                BranchAssignmentStatus.ACTIVE))
                .extracting(assignment -> assignment.getBranch().getName())
                .containsExactly("South Branch");

        List<AuditEvent> auditEvents = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(fixture.actorMembership().getId());
        assertThat(auditEvents).extracting(AuditEvent::getActionType).contains("USER_PROFILE_ASSIGNMENTS_UPDATED");
    }

    @Test
    void restrictedFieldsAndPermissionsAreProtected() {
        Fixture branchAdminVsOwner = createFixture(AgencyRole.BRANCH_ADMIN, AgencyRole.AGENCY_OWNER);

        assertThatThrownBy(() -> userProfileAssignmentService.updateUser(
                branchAdminVsOwner.actorMembership(),
                branchAdminVsOwner.targetUser().getId(),
                new UserProfileAssignmentService.UpdateUserCommand(
                        "Alicia",
                        "Owner",
                        null,
                        AgencyRole.AGENCY_OWNER,
                        Set.of())))
                .isInstanceOf(ProtectedUserEditException.class);

        Fixture caregiverActor = createFixture(AgencyRole.CAREGIVER, AgencyRole.CAREGIVER);
        assertThatThrownBy(() -> userProfileAssignmentService.updateUser(
                caregiverActor.actorMembership(),
                caregiverActor.targetUser().getId(),
                new UserProfileAssignmentService.UpdateUserCommand(
                        "Casey",
                        "Caregiver",
                        null,
                        AgencyRole.CAREGIVER,
                        Set.of())))
                .isInstanceOf(UnauthorizedUserEditActorException.class);
    }

    @Test
    void rejectsCrossAgencyEdits() {
        Agency firstAgency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Agency secondAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", UUID.randomUUID().toString(), "America/New_York", "ops@sunrise.example"));

        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), firstAgency, AgencyRole.AGENCY_OWNER);
        User outsider = createUser("Jordan", "Outsider");
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(outsider, secondAgency, AgencyRole.CAREGIVER));

        assertThatThrownBy(() -> userProfileAssignmentService.updateUser(
                actorMembership,
                outsider.getId(),
                new UserProfileAssignmentService.UpdateUserCommand(
                        "Jordan",
                        "Updated",
                        null,
                        AgencyRole.CAREGIVER,
                        Set.of())))
                .isInstanceOf(UserNotManageableInAgencyException.class);
    }

    private Fixture createFixture(AgencyRole actorRole, AgencyRole targetRole) {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Admin"), agency, actorRole);
        User targetUser = createUser("Casey", "Staff");
        AgencyMembership targetMembership = createMembership(targetUser, agency, targetRole);
        return new Fixture(agency, actorMembership, targetUser, targetMembership);
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(
                firstName,
                lastName,
                UUID.randomUUID() + "@northstar.example",
                null));
    }

    private record Fixture(Agency agency, AgencyMembership actorMembership, User targetUser, AgencyMembership targetMembership) {
    }
}
