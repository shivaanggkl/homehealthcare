package com.homehealthcare.patienteligibility.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patienteligibility.application.PatientServiceEligibilityService;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibility;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityRepository;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityStatus;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
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
class PatientServiceEligibilityController {

    private final PatientServiceEligibilityRepository patientServiceEligibilityRepository;
    private final PatientServiceEligibilityService patientServiceEligibilityService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientServiceEligibilityController(
            PatientServiceEligibilityRepository patientServiceEligibilityRepository,
            PatientServiceEligibilityService patientServiceEligibilityService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientServiceEligibilityRepository = patientServiceEligibilityRepository;
        this.patientServiceEligibilityService = patientServiceEligibilityService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/patients/{patientId}/eligibilities")
    List<PatientServiceEligibilityResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(name = "status", required = false) PatientServiceEligibilityStatus status,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);
        return patientServiceEligibilityRepository.findAllByPatient_IdOrderByEffectiveFromDesc(patientId).stream()
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> serviceLineId == null || (item.getServiceLine() != null && serviceLineId.equals(item.getServiceLine().getId())))
                .map(PatientServiceEligibilityController::toResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/eligibilities")
    PatientServiceEligibilityResponse create(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientServiceEligibilityRequest request) {
        PatientServiceEligibility saved = patientServiceEligibilityService.create(
                configurationActorResolver.requireActorMembership(),
                patientId,
                toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/patient-eligibilities/{eligibilityId}")
    PatientServiceEligibilityResponse update(@PathVariable UUID eligibilityId, @Valid @RequestBody ManagePatientServiceEligibilityRequest request) {
        PatientServiceEligibility saved = patientServiceEligibilityService.update(configurationActorResolver.requireActorMembership(), eligibilityId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/patient-eligibilities/{eligibilityId}")
    PatientServiceEligibilityResponse deactivate(@PathVariable UUID eligibilityId) {
        PatientServiceEligibility saved = patientServiceEligibilityService.deactivate(configurationActorResolver.requireActorMembership(), eligibilityId);
        return toResponse(saved);
    }

    private static PatientServiceEligibilityService.ManagePatientServiceEligibilityCommand toCommand(ManagePatientServiceEligibilityRequest request) {
        return new PatientServiceEligibilityService.ManagePatientServiceEligibilityCommand(
                request.serviceLineId(),
                request.status(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.verificationSource(),
                request.notes());
    }

    private static PatientServiceEligibilityResponse toResponse(PatientServiceEligibility item) {
        return new PatientServiceEligibilityResponse(
                item.getId(),
                item.getPatient().getId(),
                item.getServiceLine() == null ? null : item.getServiceLine().getId(),
                item.getStatus(),
                item.getEffectiveFrom(),
                item.getEffectiveTo(),
                item.getVerificationSource(),
                item.getNotes());
    }

    record ManagePatientServiceEligibilityRequest(
            UUID serviceLineId,
            @NotNull PatientServiceEligibilityStatus status,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
    }

    record PatientServiceEligibilityResponse(
            UUID id,
            UUID patientId,
            UUID serviceLineId,
            PatientServiceEligibilityStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
    }
}
