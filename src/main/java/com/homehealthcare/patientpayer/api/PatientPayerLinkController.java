package com.homehealthcare.patientpayer.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patientpayer.application.PatientPayerLinkService;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkStatus;
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
class PatientPayerLinkController {

    private final PatientPayerLinkRepository patientPayerLinkRepository;
    private final PatientPayerLinkService patientPayerLinkService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientPayerLinkController(
            PatientPayerLinkRepository patientPayerLinkRepository,
            PatientPayerLinkService patientPayerLinkService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientPayerLinkRepository = patientPayerLinkRepository;
        this.patientPayerLinkService = patientPayerLinkService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/patients/{patientId}/payer-links")
    List<PatientPayerLinkResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(name = "status", required = false) PatientPayerLinkStatus status,
            @RequestParam(name = "primaryPayer", required = false) Boolean primaryPayer) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.MANAGE_PATIENT_PAYER_LINKAGE, UnauthorizedPatientActorException::new);
        return patientPayerLinkRepository.findAllByPatient_IdOrderByEffectiveFromDesc(patientId).stream()
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> primaryPayer == null || item.isPrimaryPayer() == primaryPayer)
                .map(PatientPayerLinkController::toResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/payer-links")
    PatientPayerLinkResponse create(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientPayerLinkRequest request) {
        PatientPayerLink saved = patientPayerLinkService.create(
                configurationActorResolver.requireActorMembership(),
                patientId,
                toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/patient-payer-links/{payerLinkId}")
    PatientPayerLinkResponse update(@PathVariable UUID payerLinkId, @Valid @RequestBody ManagePatientPayerLinkRequest request) {
        PatientPayerLink saved = patientPayerLinkService.update(configurationActorResolver.requireActorMembership(), payerLinkId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/patient-payer-links/{payerLinkId}")
    PatientPayerLinkResponse deactivate(@PathVariable UUID payerLinkId) {
        PatientPayerLink saved = patientPayerLinkService.deactivate(configurationActorResolver.requireActorMembership(), payerLinkId);
        return toResponse(saved);
    }

    private static PatientPayerLinkService.ManagePatientPayerLinkCommand toCommand(ManagePatientPayerLinkRequest request) {
        return new PatientPayerLinkService.ManagePatientPayerLinkCommand(
                request.payerName(),
                request.payerExternalId(),
                request.memberPolicyNumber(),
                request.groupNumber(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.primaryPayer(),
                request.status(),
                request.notes());
    }

    private static PatientPayerLinkResponse toResponse(PatientPayerLink item) {
        return new PatientPayerLinkResponse(
                item.getId(),
                item.getPatient().getId(),
                item.getPayerName(),
                item.getPayerExternalId(),
                item.getMemberPolicyNumber(),
                item.getGroupNumber(),
                item.getEffectiveFrom(),
                item.getEffectiveTo(),
                item.isPrimaryPayer(),
                item.getStatus(),
                item.getNotes());
    }

    record ManagePatientPayerLinkRequest(
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String groupNumber,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean primaryPayer,
            @NotNull PatientPayerLinkStatus status,
            String notes) {
    }

    record PatientPayerLinkResponse(
            UUID id,
            UUID patientId,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String groupNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean primaryPayer,
            PatientPayerLinkStatus status,
            String notes) {
    }
}
