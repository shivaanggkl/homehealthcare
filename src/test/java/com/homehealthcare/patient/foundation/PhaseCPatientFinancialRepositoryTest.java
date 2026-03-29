package com.homehealthcare.patient.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkStatus;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhaseCPatientFinancialRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientPayerLinkRepository patientPayerLinkRepository;

    @Autowired
    private PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Test
    void patientPayerLinkRequiresIdentityAndValidDates() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                null,
                "Casey",
                null,
                "Client",
                null,
                LocalDate.of(1984, 3, 2),
                null,
                null,
                null,
                null,
                null,
                null));

        PatientPayerLink link = patientPayerLinkRepository.saveAndFlush(PatientPayerLink.create(
                patient,
                "Blue Cross",
                null,
                "POL-001",
                "GRP-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true,
                PatientPayerLinkStatus.ACTIVE,
                "Primary commercial payer"));

        assertThat(link.getPayerName()).isEqualTo("Blue Cross");
        assertThat(link.isPrimaryPayer()).isTrue();

        assertThatThrownBy(() -> PatientPayerLink.create(
                patient,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                null,
                false,
                PatientPayerLinkStatus.PENDING,
                null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payerName or payerExternalId must be provided");

        assertThatThrownBy(() -> PatientPayerLink.create(
                patient,
                "Blue Cross",
                null,
                "POL-001",
                null,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1),
                false,
                PatientPayerLinkStatus.ACTIVE,
                null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("effectiveTo must be on or after effectiveFrom");
    }

    @Test
    void patientAuthorizationRequiresValidUnitStateAndAgencyAlignedServiceLine() {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agencyOne,
                null,
                "Casey",
                null,
                "Client",
                null,
                LocalDate.of(1984, 3, 2),
                null,
                null,
                null,
                null,
                null,
                null));
        PatientPayerLink payerLink = patientPayerLinkRepository.saveAndFlush(PatientPayerLink.create(
                patient,
                "Blue Cross",
                null,
                "POL-001",
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true,
                PatientPayerLinkStatus.ACTIVE,
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyOne, "Private Duty", "PD", "Companion care", 1));
        ServiceLine foreignServiceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyTwo, "Skilled Nursing", "SN", "Foreign agency", 1));

        PatientEpisodeAuthorization authorization = patientEpisodeAuthorizationRepository.saveAndFlush(PatientEpisodeAuthorization.create(
                patient,
                payerLink,
                serviceLine,
                "AUTH-001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                40,
                10,
                PatientEpisodeAuthorizationStatus.ACTIVE,
                "Initial authorization"));

        assertThat(authorization.getAuthorizationNumber()).isEqualTo("AUTH-001");
        assertThat(authorization.getAuthorizedUnits()).isEqualTo(40);

        assertThatThrownBy(() -> PatientEpisodeAuthorization.create(
                patient,
                payerLink,
                foreignServiceLine,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null,
                PatientEpisodeAuthorizationStatus.PENDING,
                null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceLine must belong to the same agency as the patient");

        assertThatThrownBy(() -> PatientEpisodeAuthorization.create(
                patient,
                payerLink,
                serviceLine,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                10,
                11,
                PatientEpisodeAuthorizationStatus.ACTIVE,
                null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("usedUnits must be less than or equal to authorizedUnits");
    }
}
