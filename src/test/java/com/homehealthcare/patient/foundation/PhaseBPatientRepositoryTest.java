package com.homehealthcare.patient.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.patientcontact.domain.PatientContact;
import com.homehealthcare.patientcontact.domain.PatientContactRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisCondition;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisConditionRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibility;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityRepository;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityStatus;
import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PhaseBPatientRepositoryTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientContactRepository patientContactRepository;

    @Autowired
    private PatientAddressRepository patientAddressRepository;

    @Autowired
    private PatientServiceEligibilityRepository patientServiceEligibilityRepository;

    @Autowired
    private PatientDiagnosisConditionRepository patientDiagnosisConditionRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Test
    void patientNormalizesDemographicsAndSupportsLifecycle() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "  EXT-001 ",
                " Shiva ",
                " A ",
                " Kl ",
                " Shiv ",
                LocalDate.of(1990, 1, 15),
                "female",
                " 312-555-0101 ",
                " ",
                " SHIVA@EXAMPLE.COM ",
                " en-us ",
                " prefers morning visits "));

        assertThat(patient.getExternalReference()).isEqualTo("EXT-001");
        assertThat(patient.getEmail()).isEqualTo("shiva@example.com");
        assertThat(patient.getLanguage()).isEqualTo("en-US");
        assertThat(patient.getStatus()).isEqualTo(PatientLifecycleStatus.ACTIVE);

        patient.deactivate();
        patientRepository.saveAndFlush(patient);
        assertThat(patient.getStatus()).isEqualTo(PatientLifecycleStatus.INACTIVE);
    }

    @Test
    void patientContactAndAddressAreAgencyScopedAndAddressIsUniquePerPatient() {
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

        PatientContact contact = patientContactRepository.saveAndFlush(PatientContact.create(
                patient,
                "Daughter",
                "Jordan Client",
                "312-555-0144",
                "jordan@example.com",
                "123 Main St",
                true,
                true,
                true,
                "Primary emergency contact"));

        PatientAddress address = patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient,
                "123 Main St",
                "Apt 4",
                "Chicago",
                "IL",
                "60601",
                "US",
                new BigDecimal("41.881832"),
                new BigDecimal("-87.623177"),
                "GEOCODED",
                "America/Chicago",
                "Side entrance"));

        assertThat(contact.getStatus()).isEqualTo(PatientLifecycleStatus.ACTIVE);
        assertThat(address.getLatitude()).isEqualByComparingTo("41.881832");
        assertThat(address.getTimezone()).isEqualTo("America/Chicago");

        assertThatThrownBy(() -> patientAddressRepository.saveAndFlush(PatientAddress.create(
                        patient,
                        "999 Other",
                        null,
                        "Chicago",
                        "IL",
                        "60602",
                        "US",
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void serviceEligibilityRequiresSameAgencyServiceLineAndValidDateWindow() {
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
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyOne, "Private Duty", "PD", "Companion care", 1));
        ServiceLine foreignServiceLine = serviceLineRepository.saveAndFlush(
                ServiceLine.create(agencyTwo, "Skilled Nursing", "SN", "Foreign agency", 1));

        PatientServiceEligibility eligibility = patientServiceEligibilityRepository.saveAndFlush(PatientServiceEligibility.create(
                patient,
                serviceLine,
                PatientServiceEligibilityStatus.ELIGIBLE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "Intake review",
                "Approved for service"));

        assertThat(eligibility.getServiceLine()).isNotNull();
        assertThat(eligibility.getServiceLine().getId()).isEqualTo(serviceLine.getId());

        assertThatThrownBy(() -> PatientServiceEligibility.create(
                        patient,
                        foreignServiceLine,
                        PatientServiceEligibilityStatus.ELIGIBLE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviceLine must belong to the same agency as the patient");

        assertThatThrownBy(() -> patientServiceEligibilityRepository.saveAndFlush(PatientServiceEligibility.create(
                        patient,
                        serviceLine,
                        PatientServiceEligibilityStatus.ELIGIBLE,
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 1, 1),
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("effectiveTo must be on or after effectiveFrom");
    }

    @Test
    void diagnosisSupportsClinicalStateAndRejectsInvalidResolvedDates() {
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

        PatientDiagnosisCondition diagnosis = patientDiagnosisConditionRepository.saveAndFlush(PatientDiagnosisCondition.create(
                patient,
                "I10",
                "Hypertension",
                "Chronic",
                true,
                LocalDate.of(2025, 1, 1),
                null,
                PatientDiagnosisStatus.ACTIVE,
                "Monitor blood pressure"));

        assertThat(diagnosis.getStatus()).isEqualTo(PatientDiagnosisStatus.ACTIVE);
        assertThat(diagnosis.isPrimaryCondition()).isTrue();

        assertThatThrownBy(() -> patientDiagnosisConditionRepository.saveAndFlush(PatientDiagnosisCondition.create(
                        patient,
                        "J44.9",
                        "COPD",
                        "Chronic",
                        false,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 3, 1),
                        PatientDiagnosisStatus.RESOLVED,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("resolvedDate must be on or after onsetDate");
    }
}
