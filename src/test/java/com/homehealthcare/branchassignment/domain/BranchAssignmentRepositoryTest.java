package com.homehealthcare.branchassignment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class BranchAssignmentRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private BranchAssignmentRepository branchAssignmentRepository;

    @Test
    void supportsOneOrManyAssignmentsForSingleMembership() {
        TestFixture fixture = createFixture();
        Branch firstBranch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch secondBranch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));

        BranchAssignment firstAssignment = branchAssignmentRepository.saveAndFlush(
                BranchAssignment.assign(fixture.membership(), firstBranch));
        BranchAssignment secondAssignment = branchAssignmentRepository.saveAndFlush(
                BranchAssignment.assign(fixture.membership(), secondBranch));

        assertThat(firstAssignment.getAgencyMembershipId()).isEqualTo(fixture.membership().getId());
        assertThat(secondAssignment.getAgencyMembershipId()).isEqualTo(fixture.membership().getId());
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                fixture.membership().getId(), BranchAssignmentStatus.ACTIVE)).hasSize(2);
    }

    @Test
    void assignmentsCanBeActivatedAndDeactivated() {
        TestFixture fixture = createFixture();
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        BranchAssignment assignment = branchAssignmentRepository.saveAndFlush(
                BranchAssignment.assign(fixture.membership(), branch));
        assignment.deactivate();
        BranchAssignment inactive = branchAssignmentRepository.saveAndFlush(assignment);

        assertThat(inactive.isActive()).isFalse();
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                fixture.membership().getId(), BranchAssignmentStatus.ACTIVE)).isEmpty();

        inactive.activate();
        BranchAssignment activeAgain = branchAssignmentRepository.saveAndFlush(inactive);

        assertThat(activeAgain.isActive()).isTrue();
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                fixture.membership().getId(), BranchAssignmentStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void rejectsDuplicateAssignmentForSameMembershipAndBranch() {
        TestFixture fixture = createFixture();
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(fixture.membership(), branch));

        assertThatThrownBy(() -> branchAssignmentRepository.saveAndFlush(
                BranchAssignment.assign(fixture.membership(), branch)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAssignmentAcrossDifferentAgencies() {
        TestFixture first = createFixture();
        Agency secondAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));
        Branch secondAgencyBranch = branchRepository.saveAndFlush(
                Branch.create(secondAgency, "Brooklyn Central", "BK-01", "456 Flatbush Ave", "America/New_York"));

        assertThatThrownBy(() -> BranchAssignment.assign(first.membership(), secondAgencyBranch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same agency");
    }

    @Test
    void rejectsAssignmentForInactiveMembership() {
        TestFixture fixture = createFixture();
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(fixture.agency(), "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        fixture.membership().deactivate();
        agencyMembershipRepository.saveAndFlush(fixture.membership());

        assertThatThrownBy(() -> BranchAssignment.assign(fixture.membership(), branch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active agency membership");
    }

    private TestFixture createFixture() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", java.util.UUID.randomUUID() + "@example.com", "+1 312 555 0101"));
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", java.util.UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, agency, AgencyRole.SCHEDULER_COORDINATOR));
        return new TestFixture(agency, membership);
    }

    private record TestFixture(Agency agency, AgencyMembership membership) {
    }
}
