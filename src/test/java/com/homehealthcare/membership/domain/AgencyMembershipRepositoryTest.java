package com.homehealthcare.membership.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
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
class AgencyMembershipRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Test
    void savesMembershipLinkedToUserAndAgencyWithRoleMetadata() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, agency, AgencyRole.AGENCY_OWNER));

        assertThat(membership.getId()).isNotNull();
        assertThat(membership.getUserId()).isEqualTo(user.getId());
        assertThat(membership.getAgencyId()).isEqualTo(agency.getId());
        assertThat(membership.getRole()).isEqualTo(AgencyRole.AGENCY_OWNER);
        assertThat(membership.getStatus()).isEqualTo(AgencyMembershipStatus.ACTIVE);
        assertThat(membership.getCreatedAt()).isNotNull();
        assertThat(membership.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesSingleMembershipPerUserPerAgency() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, agency, AgencyRole.AGENCY_OWNER));

        AgencyMembership duplicate = AgencyMembership.grant(user, agency, AgencyRole.BRANCH_ADMIN);

        assertThatThrownBy(() -> agencyMembershipRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameUserCanBelongToMultipleAgencies() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        Agency firstAgency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency secondAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));

        AgencyMembership firstMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, firstAgency, AgencyRole.AGENCY_OWNER));
        AgencyMembership secondMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, secondAgency, AgencyRole.BRANCH_ADMIN));

        assertThat(firstMembership.getAgencyId()).isNotEqualTo(secondMembership.getAgencyId());
        assertThat(agencyMembershipRepository.existsByUser_IdAndAgency_Id(user.getId(), firstAgency.getId())).isTrue();
        assertThat(agencyMembershipRepository.existsByUser_IdAndAgency_Id(user.getId(), secondAgency.getId())).isTrue();
    }

    @Test
    void supportsActiveInactiveMembershipLifecycle() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, agency, AgencyRole.SCHEDULER_COORDINATOR));

        membership.deactivate();
        AgencyMembership inactive = agencyMembershipRepository.saveAndFlush(membership);
        assertThat(inactive.getStatus()).isEqualTo(AgencyMembershipStatus.INACTIVE);
        assertThat(inactive.isActive()).isFalse();
        assertThat(agencyMembershipRepository.existsByUser_IdAndAgency_IdAndStatus(
                user.getId(), agency.getId(), AgencyMembershipStatus.ACTIVE)).isFalse();

        inactive.activate();
        AgencyMembership activeAgain = agencyMembershipRepository.saveAndFlush(inactive);
        assertThat(activeAgain.getStatus()).isEqualTo(AgencyMembershipStatus.ACTIVE);
        assertThat(activeAgain.isActive()).isTrue();
        assertThat(agencyMembershipRepository.existsByUser_IdAndAgency_IdAndStatus(
                user.getId(), agency.getId(), AgencyMembershipStatus.ACTIVE)).isTrue();
    }

    @Test
    void findsActiveMembershipsForUser() {
        User user = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.owner@northstar.example", "+1 312 555 0101"));
        Agency firstAgency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency secondAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));

        AgencyMembership activeMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, firstAgency, AgencyRole.AGENCY_OWNER));
        AgencyMembership inactiveMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, secondAgency, AgencyRole.BRANCH_ADMIN));
        inactiveMembership.deactivate();
        agencyMembershipRepository.saveAndFlush(inactiveMembership);

        assertThat(agencyMembershipRepository.findAllByUser_IdAndStatus(user.getId(), AgencyMembershipStatus.ACTIVE))
                .extracting(AgencyMembership::getId)
                .containsExactly(activeMembership.getId());
    }
}
