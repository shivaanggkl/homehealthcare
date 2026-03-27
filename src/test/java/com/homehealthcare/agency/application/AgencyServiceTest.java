package com.homehealthcare.agency.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AgencyServiceTest {

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private AgencyRepository agencyRepository;

    @Test
    void deactivatesAgencyLifecycleState() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example"));

        Agency updated = agencyService.deactivateAgency(agency);

        assertThat(updated.isDeactivated()).isTrue();
        assertThat(updated.getDeactivatedAt()).isNotNull();
    }
}
