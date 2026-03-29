package com.homehealthcare.patient.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.patient.application.PatientRecordService;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.PatientLifecycleStatus;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
@RequestMapping("/api/patients")
class PatientController {

    private final PatientRepository patientRepository;
    private final PatientRecordService patientRecordService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientController(
            PatientRepository patientRepository,
            PatientRecordService patientRecordService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientRepository = patientRepository;
        this.patientRecordService = patientRecordService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    PagedResponse<PatientResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) PatientLifecycleStatus status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);

        List<PatientResponse> filtered = patientRepository.findAllByAgency_IdOrderByLastNameAscFirstNameAsc(actorMembership.getAgencyId()).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> matchesSearch(item, search))
                .map(PatientController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @GetMapping("/{patientId}")
    PatientResponse get(@PathVariable UUID patientId) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new com.homehealthcare.patient.application.PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);
        return toResponse(patient);
    }

    @PostMapping
    PatientResponse create(@Valid @RequestBody ManagePatientRequest request) {
        Patient saved = patientRecordService.create(configurationActorResolver.requireActorMembership(), toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/{patientId}")
    PatientResponse update(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientRequest request) {
        Patient saved = patientRecordService.update(configurationActorResolver.requireActorMembership(), patientId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/{patientId}")
    PatientResponse deactivate(@PathVariable UUID patientId) {
        Patient saved = patientRecordService.deactivate(configurationActorResolver.requireActorMembership(), patientId);
        return toResponse(saved);
    }

    private static boolean matchesSearch(Patient item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return item.getFirstName().toLowerCase(Locale.ROOT).contains(normalized)
                || item.getLastName().toLowerCase(Locale.ROOT).contains(normalized)
                || (item.getPreferredName() != null && item.getPreferredName().toLowerCase(Locale.ROOT).contains(normalized))
                || (item.getExternalReference() != null && item.getExternalReference().toLowerCase(Locale.ROOT).contains(normalized))
                || (item.getEmail() != null && item.getEmail().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private static PagedResponse<PatientResponse> page(List<PatientResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static PatientRecordService.ManagePatientCommand toCommand(ManagePatientRequest request) {
        return new PatientRecordService.ManagePatientCommand(
                request.externalReference(),
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.preferredName(),
                request.dateOfBirth(),
                request.sexMarker(),
                request.primaryPhone(),
                request.secondaryPhone(),
                request.email(),
                request.language(),
                request.notesSummary());
    }

    private static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getAgencyId(),
                patient.getStatus(),
                patient.getExternalReference(),
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName(),
                patient.getPreferredName(),
                patient.getDateOfBirth(),
                patient.getSexMarker(),
                patient.getPrimaryPhone(),
                patient.getSecondaryPhone(),
                patient.getEmail(),
                patient.getLanguage(),
                patient.getNotesSummary());
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new com.homehealthcare.patient.application.PatientEntityNotFoundException(entityType, entityId);
        }
    }

    record ManagePatientRequest(
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

    record PatientResponse(
            UUID id,
            UUID agencyId,
            PatientLifecycleStatus status,
            String externalReference,
            String firstName,
            String middleName,
            String lastName,
            String preferredName,
            LocalDate dateOfBirth,
            String sexMarker,
            String primaryPhone,
            String secondaryPhone,
            String email,
            String language,
            String notesSummary) {
    }
}
