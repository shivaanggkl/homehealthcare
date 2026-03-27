package com.homehealthcare.branch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.security.tenant.TenantContext;
import com.homehealthcare.security.tenant.TenantContextHolder;
import java.util.UUID;
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
class BranchRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void savesBranchWithAgencyOwnershipAndRequiredFields() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        Branch branch = branchRepository.saveAndFlush(
                Branch.create(
                        agency,
                        "Chicago Central",
                        "chi-01",
                        "123 Main St, Chicago, IL 60601",
                        "America/Chicago"));

        assertThat(branch.getId()).isNotNull();
        assertThat(branch.getAgencyId()).isEqualTo(agency.getId());
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.ACTIVE);
        assertThat(branch.getCreatedAt()).isNotNull();
        assertThat(branch.getUpdatedAt()).isNotNull();
        assertThat(branch.getCode()).isEqualTo("CHI-01");
    }

    @Test
    void enforcesUniqueNameWithinAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        Branch duplicate = Branch.create(agency, "Chicago Central", "CHI-02", "456 Lake St", "America/Chicago");

        assertThatThrownBy(() -> branchRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesUniqueCodeWithinAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        Branch duplicate = Branch.create(agency, "Chicago North", "CHI-01", "456 Lake St", "America/Chicago");

        assertThatThrownBy(() -> branchRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameNameAndCodeAcrossDifferentAgencies() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));

        Branch first = branchRepository.saveAndFlush(
                Branch.create(agencyOne, "Main Branch", "MAIN", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agencyTwo, "Main Branch", "MAIN", "999 Broad St", "America/New_York"));

        assertThat(first.getAgencyId()).isNotEqualTo(second.getAgencyId());
    }

    @Test
    void supportsActiveInactiveLifecycle() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));

        branch.deactivate();
        Branch inactive = branchRepository.saveAndFlush(branch);
        assertThat(inactive.getStatus()).isEqualTo(BranchStatus.INACTIVE);
        assertThat(inactive.isDeactivated()).isTrue();

        inactive.activate();
        Branch activeAgain = branchRepository.saveAndFlush(inactive);
        assertThat(activeAgain.getStatus()).isEqualTo(BranchStatus.ACTIVE);
        assertThat(activeAgain.isDeactivated()).isFalse();
    }

    @Test
    void inheritedFindByIdAndExistsByIdDoNotCrossTenantBoundaryWhenContextIsBound() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));
        Branch branchOne = branchRepository.saveAndFlush(
                Branch.create(agencyOne, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch branchTwo = branchRepository.saveAndFlush(
                Branch.create(agencyTwo, "Brooklyn Central", "BK-01", "456 Flatbush Ave", "America/New_York"));

        TenantContextHolder.set(new TenantContext(agencyOne.getId(), UUID.randomUUID()));
        try {
            assertThat(branchRepository.findById(branchOne.getId())).contains(branchOne);
            assertThat(branchRepository.findById(branchTwo.getId())).isEmpty();
            assertThat(branchRepository.existsById(branchTwo.getId())).isFalse();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
