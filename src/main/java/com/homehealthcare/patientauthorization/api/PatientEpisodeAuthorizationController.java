package com.homehealthcare.patientauthorization.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patientauthorization.application.PatientEpisodeAuthorizationService;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
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
class PatientEpisodeAuthorizationController {

    private final PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository;
    private final PatientEpisodeAuthorizationService patientEpisodeAuthorizationService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientEpisodeAuthorizationController(
            PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository,
            PatientEpisodeAuthorizationService patientEpisodeAuthorizationService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientEpisodeAuthorizationRepository = patientEpisodeAuthorizationRepository;
        this.patientEpisodeAuthorizationService = patientEpisodeAuthorizationService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/patients/{patientId}/authorizations")
    List<PatientEpisodeAuthorizationResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(name = "status", required = false) PatientEpisodeAuthorizationStatus status,
            @RequestParam(name = "currentOnly", required = false, defaultValue = "false") boolean currentOnly,
            @RequestParam(name = "patientPayerLinkId", required = false) UUID patientPayerLinkId,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_AUTHORIZATIONS, UnauthorizedPatientActorException::new);
        LocalDate today = LocalDate.now();
        return patientEpisodeAuthorizationRepository.findAllByPatient_IdOrderByStartDateDesc(patientId).stream()
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> !currentOnly || (!item.getStartDate().isAfter(today) && !item.getEndDate().isBefore(today)))
                .filter(item -> patientPayerLinkId == null || (item.getPatientPayerLink() != null && patientPayerLinkId.equals(item.getPatientPayerLink().getId())))
                .filter(item -> serviceLineId == null || (item.getServiceLine() != null && serviceLineId.equals(item.getServiceLine().getId())))
                .map(PatientEpisodeAuthorizationController::toResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/authorizations")
    PatientEpisodeAuthorizationResponse create(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientEpisodeAuthorizationRequest request) {
        PatientEpisodeAuthorization saved = patientEpisodeAuthorizationService.create(
                configurationActorResolver.requireActorMembership(),
                patientId,
                toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/patient-authorizations/{authorizationId}")
    PatientEpisodeAuthorizationResponse update(@PathVariable UUID authorizationId, @Valid @RequestBody ManagePatientEpisodeAuthorizationRequest request) {
        PatientEpisodeAuthorization saved = patientEpisodeAuthorizationService.update(configurationActorResolver.requireActorMembership(), authorizationId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/patient-authorizations/{authorizationId}")
    PatientEpisodeAuthorizationResponse deactivate(@PathVariable UUID authorizationId) {
        PatientEpisodeAuthorization saved = patientEpisodeAuthorizationService.deactivate(configurationActorResolver.requireActorMembership(), authorizationId);
        return toResponse(saved);
    }

    private static PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand toCommand(ManagePatientEpisodeAuthorizationRequest request) {
        return new PatientEpisodeAuthorizationService.ManagePatientEpisodeAuthorizationCommand(
                request.patientPayerLinkId(),
                request.serviceLineId(),
                request.authorizationNumber(),
                request.startDate(),
                request.endDate(),
                request.authorizedUnits(),
                request.usedUnits(),
                request.status(),
                request.notes());
    }

    private static PatientEpisodeAuthorizationResponse toResponse(PatientEpisodeAuthorization item) {
        return new PatientEpisodeAuthorizationResponse(
                item.getId(),
                item.getPatient().getId(),
                item.getPatientPayerLink() == null ? null : item.getPatientPayerLink().getId(),
                item.getServiceLine() == null ? null : item.getServiceLine().getId(),
                item.getAuthorizationNumber(),
                item.getStartDate(),
                item.getEndDate(),
                item.getAuthorizedUnits(),
                item.getUsedUnits(),
                item.getStatus(),
                item.getNotes());
    }

    record ManagePatientEpisodeAuthorizationRequest(
            UUID patientPayerLinkId,
            UUID serviceLineId,
            String authorizationNumber,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Integer authorizedUnits,
            Integer usedUnits,
            @NotNull PatientEpisodeAuthorizationStatus status,
            String notes) {
    }

    record PatientEpisodeAuthorizationResponse(
            UUID id,
            UUID patientId,
            UUID patientPayerLinkId,
            UUID serviceLineId,
            String authorizationNumber,
            LocalDate startDate,
            LocalDate endDate,
            Integer authorizedUnits,
            Integer usedUnits,
            PatientEpisodeAuthorizationStatus status,
            String notes) {
    }
}
