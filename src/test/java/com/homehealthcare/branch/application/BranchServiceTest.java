package com.homehealthcare.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branch.domain.BranchStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BranchServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchService branchService;

    @Test
    void createsBranchForExistingAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        Branch branch = branchService.createBranch(new BranchService.CreateBranchCommand(
                agency.getId(),
                "Chicago Central",
                "chi-01",
                "123 Main St, Chicago, IL 60601",
                "America/Chicago"));

        assertThat(branch.getId()).isNotNull();
        assertThat(branch.getAgencyId()).isEqualTo(agency.getId());
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.ACTIVE);
        assertThat(branchRepository.findByAgency_IdAndCode(agency.getId(), "CHI-01")).contains(branch);
    }

    @Test
    void rejectsBranchCreationForMissingAgency() {
        assertThatThrownBy(() -> branchService.createBranch(new BranchService.CreateBranchCommand(
                UUID.randomUUID(),
                "Chicago Central",
                "CHI-01",
                "123 Main St, Chicago, IL 60601",
                "America/Chicago")))
                .isInstanceOf(AgencyNotFoundException.class);
    }

    @Test
    void rejectsDuplicateNameWithinAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        branchService.createBranch(new BranchService.CreateBranchCommand(
                agency.getId(),
                "Chicago Central",
                "CHI-01",
                "123 Main St",
                "America/Chicago"));

        assertThatThrownBy(() -> branchService.createBranch(new BranchService.CreateBranchCommand(
                agency.getId(),
                "Chicago Central",
                "CHI-02",
                "456 Lake St",
                "America/Chicago")))
                .isInstanceOf(DuplicateBranchNameException.class);
    }

    @Test
    void rejectsDuplicateCodeWithinAgency() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        branchService.createBranch(new BranchService.CreateBranchCommand(
                agency.getId(),
                "Chicago Central",
                "CHI-01",
                "123 Main St",
                "America/Chicago"));

        assertThatThrownBy(() -> branchService.createBranch(new BranchService.CreateBranchCommand(
                agency.getId(),
                "Chicago North",
                "chi-01",
                "456 Lake St",
                "America/Chicago")))
                .isInstanceOf(DuplicateBranchCodeException.class);
    }
}
