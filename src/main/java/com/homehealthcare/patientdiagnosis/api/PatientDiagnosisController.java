package com.homehealthcare.patientdiagnosis.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patientdiagnosis.application.PatientDiagnosisService;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisCondition;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisConditionRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class PatientDiagnosisController {

    private final PatientDiagnosisConditionRepository patientDiagnosisConditionRepository;
    private final PatientDiagnosisService patientDiagnosisService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientDiagnosisController(
            PatientDiagnosisConditionRepository patientDiagnosisConditionRepository,
            PatientDiagnosisService patientDiagnosisService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientDiagnosisConditionRepository = patientDiagnosisConditionRepository;
        this.patientDiagnosisService = patientDiagnosisService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/patients/{patientId}/diagnoses")
    List<PatientDiagnosisResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(name = "status", required = false) PatientDiagnosisStatus status) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);
        return patientDiagnosisConditionRepository.findAllByPatient_IdOrderByPrimaryConditionDescDescriptionAsc(patientId).stream()
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .filter(item -> status == null || item.getStatus() == status)
                .map(PatientDiagnosisController::toResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/diagnoses")
    PatientDiagnosisResponse create(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientDiagnosisRequest request) {
        PatientDiagnosisCondition saved = patientDiagnosisService.create(
                configurationActorResolver.requireActorMembership(),
                patientId,
                toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/patient-diagnoses/{diagnosisId}")
    PatientDiagnosisResponse update(@PathVariable UUID diagnosisId, @Valid @RequestBody ManagePatientDiagnosisRequest request) {
        PatientDiagnosisCondition saved = patientDiagnosisService.update(configurationActorResolver.requireActorMembership(), diagnosisId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/patient-diagnoses/{diagnosisId}")
    PatientDiagnosisResponse deactivate(@PathVariable UUID diagnosisId) {
        PatientDiagnosisCondition saved = patientDiagnosisService.deactivate(configurationActorResolver.requireActorMembership(), diagnosisId);
        return toResponse(saved);
    }

    private static PatientDiagnosisService.ManagePatientDiagnosisCommand toCommand(ManagePatientDiagnosisRequest request) {
        return new PatientDiagnosisService.ManagePatientDiagnosisCommand(
                request.diagnosisCode(),
                request.description(),
                request.diagnosisType(),
                request.primaryCondition(),
                request.onsetDate(),
                request.resolvedDate(),
                request.status(),
                request.notes());
    }

    private static PatientDiagnosisResponse toResponse(PatientDiagnosisCondition item) {
        return new PatientDiagnosisResponse(
                item.getId(),
                item.getPatient().getId(),
                item.getDiagnosisCode(),
                item.getDescription(),
                item.getDiagnosisType(),
                item.isPrimaryCondition(),
                item.getOnsetDate(),
                item.getResolvedDate(),
                item.getStatus(),
                item.getNotes());
    }

    record ManagePatientDiagnosisRequest(
            String diagnosisCode,
            @NotBlank String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            @NotNull PatientDiagnosisStatus status,
            String notes) {
    }

    record PatientDiagnosisResponse(
            UUID id,
            UUID patientId,
            String diagnosisCode,
            String description,
            String diagnosisType,
            boolean primaryCondition,
            LocalDate onsetDate,
            LocalDate resolvedDate,
            PatientDiagnosisStatus status,
            String notes) {
    }
}
