package com.homehealthcare.agency.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
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
class AgencyRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Test
    void savesAgencyWithRequiredFieldsAndAuditing() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example"));

        assertThat(agency.getId()).isNotNull();
        assertThat(agency.getStatus()).isEqualTo(AgencyStatus.ACTIVE);
        assertThat(agency.getCreatedAt()).isNotNull();
        assertThat(agency.getUpdatedAt()).isNotNull();
        assertThat(agency.getContactEmail()).isEqualTo("ops@northstar.example");
    }

    @Test
    void enforcesUniqueSlug() {
        agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example"));

        Agency duplicate = Agency.create(
                "North Star East",
                "north-star-home-care",
                "America/New_York",
                "east@northstar.example");

        assertThatThrownBy(() -> agencyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void normalizesSlugAndEmailBeforePersist() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        " North-Star-Home-Care ",
                        "America/Chicago",
                        " OPS@NORTHSTAR.EXAMPLE "));

        assertThat(agency.getSlug()).isEqualTo("north-star-home-care");
        assertThat(agency.getContactEmail()).isEqualTo("ops@northstar.example");
    }

    @Test
    void supportsSoftDeactivation() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        "north-star-home-care",
                        "America/Chicago",
                        "ops@northstar.example"));

        agency.deactivate();
        Agency updated = agencyRepository.saveAndFlush(agency);

        assertThat(updated.getStatus()).isEqualTo(AgencyStatus.INACTIVE);
        assertThat(updated.getDeactivatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(updated.isDeactivated()).isTrue();
    }

    @Test
    void rejectsInvalidTimezone() {
        Agency agency = Agency.create(
                "North Star Home Care",
                "north-star-home-care",
                "Mars/Olympus",
                "ops@northstar.example");

        assertThatThrownBy(() -> agencyRepository.saveAndFlush(agency))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown time-zone ID");
    }

    @Test
    void supportsAllRequiredStatuses() {
        Agency active = agencyRepository.saveAndFlush(
                Agency.create(
                        "North Star Home Care",
                        UUID.randomUUID().toString(),
                        "America/Chicago",
                        "ops@northstar.example"));

        active.suspend();
        Agency suspended = agencyRepository.saveAndFlush(active);
        assertThat(suspended.getStatus()).isEqualTo(AgencyStatus.SUSPENDED);

        suspended.activate();
        Agency reactivated = agencyRepository.saveAndFlush(suspended);
        assertThat(reactivated.getStatus()).isEqualTo(AgencyStatus.ACTIVE);
    }
}
