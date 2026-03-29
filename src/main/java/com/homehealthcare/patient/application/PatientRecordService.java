package com.homehealthcare.patient.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PatientRecordService {

    private final PatientRepository patientRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public Patient create(@NotNull AgencyMembership actorMembership, @Valid ManagePatientCommand command) {
        requireManageDemographics(actorMembership);
        Agency agency = actorMembership.getAgency();

        Patient saved = patientRepository.saveAndFlush(Patient.create(
                agency,
                command.externalReference(),
                command.firstName(),
                command.middleName(),
                command.lastName(),
                command.preferredName(),
                command.dateOfBirth(),
                command.sexMarker(),
                command.primaryPhone(),
                command.secondaryPhone(),
                command.email(),
                command.language(),
                command.notesSummary()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public Patient update(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientCommand command) {
        requireManageDemographics(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        patient.updateDetails(
                command.externalReference(),
                command.firstName(),
                command.middleName(),
                command.lastName(),
                command.preferredName(),
                command.dateOfBirth(),
                command.sexMarker(),
                command.primaryPhone(),
                command.secondaryPhone(),
                command.email(),
                command.language(),
                command.notesSummary());
        Patient saved = patientRepository.saveAndFlush(patient);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public Patient deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId) {
        requireManageDemographics(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);
        patient.deactivate();
        Patient saved = patientRepository.saveAndFlush(patient);
        patientAuditService.recordDeactivated(actorMembership, Epic3PatientTargetType.PATIENT, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManageDemographics(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_DEMOGRAPHICS,
                UnauthorizedPatientActorException::new);
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(Patient patient) {
        return "{\"firstName\":\"" + patient.getFirstName() + "\",\"lastName\":\"" + patient.getLastName() + "\",\"status\":\"" + patient.getStatus().name() + "\"}";
    }

    public record ManagePatientCommand(
            String externalReference,
            @NotBlank String firstName,
            String middleName,
            @NotBlank String lastName,
            String preferredName,
            @NotNull LocalDate dateOfBirth,
            String sexMarker,
            String primaryPhone,
            String secondaryPhone,
            String email,
            String language,
            String notesSummary) {
    }
}
