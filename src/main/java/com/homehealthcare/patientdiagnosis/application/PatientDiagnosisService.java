package com.homehealthcare.patientdiagnosis.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.PatientConflictException;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisCondition;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisConditionRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
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
public class PatientDiagnosisService {

    private final PatientRepository patientRepository;
    private final PatientDiagnosisConditionRepository patientDiagnosisConditionRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientDiagnosisCondition create(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientDiagnosisCommand command) {
        requireManageDiagnoses(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);
        assertPrimaryConditionAllowed(patientId, command.primaryCondition(), null);

        PatientDiagnosisCondition saved = patientDiagnosisConditionRepository.saveAndFlush(PatientDiagnosisCondition.create(
                patient,
                command.diagnosisCode(),
                command.description(),
                command.diagnosisType(),
                command.primaryCondition(),
                command.onsetDate(),
                command.resolvedDate(),
                command.status(),
                command.notes()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_DIAGNOSIS, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientDiagnosisCondition update(@NotNull AgencyMembership actorMembership, @NotNull UUID diagnosisId, @Valid ManagePatientDiagnosisCommand command) {
        requireManageDiagnoses(actorMembership);
        PatientDiagnosisCondition diagnosis = patientDiagnosisConditionRepository.findById(diagnosisId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientDiagnosisCondition", diagnosisId));
        assertSameAgency(actorMembership.getAgencyId(), diagnosis.getAgencyId(), "PatientDiagnosisCondition", diagnosisId);
        assertPrimaryConditionAllowed(diagnosis.getPatient().getId(), command.primaryCondition(), diagnosisId);

        diagnosis.update(
                command.diagnosisCode(),
                command.description(),
                command.diagnosisType(),
                command.primaryCondition(),
                command.onsetDate(),
                command.resolvedDate(),
                command.status(),
                command.notes());
        PatientDiagnosisCondition saved = patientDiagnosisConditionRepository.saveAndFlush(diagnosis);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_DIAGNOSIS, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientDiagnosisCondition deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID diagnosisId) {
        requireManageDiagnoses(actorMembership);
        PatientDiagnosisCondition diagnosis = patientDiagnosisConditionRepository.findById(diagnosisId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientDiagnosisCondition", diagnosisId));
        assertSameAgency(actorMembership.getAgencyId(), diagnosis.getAgencyId(), "PatientDiagnosisCondition", diagnosisId);
        diagnosis.deactivate();
        PatientDiagnosisCondition saved = patientDiagnosisConditionRepository.saveAndFlush(diagnosis);
        patientAuditService.recordDeactivated(actorMembership, Epic3PatientTargetType.PATIENT_DIAGNOSIS, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManageDiagnoses(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_DIAGNOSES,
                UnauthorizedPatientActorException::new);
    }

    private void assertPrimaryConditionAllowed(UUID patientId, boolean primaryCondition, UUID existingId) {
        if (!primaryCondition) {
            return;
        }
        long count = existingId == null
                ? patientDiagnosisConditionRepository.countByPatient_IdAndPrimaryConditionTrue(patientId)
                : patientDiagnosisConditionRepository.countByPatient_IdAndPrimaryConditionTrueAndIdNot(patientId, existingId);
        if (count > 0) {
            throw new PatientConflictException("Only one primary diagnosis is allowed per patient");
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(PatientDiagnosisCondition diagnosis) {
        return "{\"description\":\"" + diagnosis.getDescription() + "\",\"status\":\"" + diagnosis.getStatus().name() + "\"}";
    }

    public record ManagePatientDiagnosisCommand(
            String diagnosisCode,
            @NotBlank String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            @NotNull PatientDiagnosisStatus status,
            String notes) {
    }
}
