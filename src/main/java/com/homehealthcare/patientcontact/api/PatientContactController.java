package com.homehealthcare.patientcontact.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patientcontact.application.PatientContactService;
import com.homehealthcare.patientcontact.domain.PatientContact;
import com.homehealthcare.patientcontact.domain.PatientContactRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class PatientContactController {

    private final PatientContactRepository patientContactRepository;
    private final PatientContactService patientContactService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientContactController(
            PatientContactRepository patientContactRepository,
            PatientContactService patientContactService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientContactRepository = patientContactRepository;
        this.patientContactService = patientContactService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/patients/{patientId}/contacts")
    List<PatientContactResponse> list(@PathVariable UUID patientId) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);
        return patientContactRepository.findAllByPatient_IdOrderByPrimaryContactDescEmergencyContactDescFullNameAsc(patientId).stream()
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .map(PatientContactController::toResponse)
                .toList();
    }

    @PostMapping("/patients/{patientId}/contacts")
    PatientContactResponse create(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientContactRequest request) {
        PatientContact saved = patientContactService.create(
                configurationActorResolver.requireActorMembership(),
                patientId,
                toCommand(request));
        return toResponse(saved);
    }

    @PutMapping("/patient-contacts/{contactId}")
    PatientContactResponse update(@PathVariable UUID contactId, @Valid @RequestBody ManagePatientContactRequest request) {
        PatientContact saved = patientContactService.update(configurationActorResolver.requireActorMembership(), contactId, toCommand(request));
        return toResponse(saved);
    }

    @DeleteMapping("/patient-contacts/{contactId}")
    PatientContactResponse deactivate(@PathVariable UUID contactId) {
        PatientContact saved = patientContactService.deactivate(configurationActorResolver.requireActorMembership(), contactId);
        return toResponse(saved);
    }

    private static PatientContactService.ManagePatientContactCommand toCommand(ManagePatientContactRequest request) {
        return new PatientContactService.ManagePatientContactCommand(
                request.relationshipType(),
                request.fullName(),
                request.phone(),
                request.email(),
                request.address(),
                request.emergencyContact(),
                request.primaryContact(),
                request.responsibleParty(),
                request.notes());
    }

    private static PatientContactResponse toResponse(PatientContact contact) {
        return new PatientContactResponse(
                contact.getId(),
                contact.getPatient().getId(),
                contact.getRelationshipType(),
                contact.getFullName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getAddress(),
                contact.isEmergencyContact(),
                contact.isPrimaryContact(),
                contact.isResponsibleParty(),
                contact.getNotes(),
                contact.getStatus());
    }

    record ManagePatientContactRequest(
            String relationshipType,
            @NotBlank String fullName,
            String phone,
            String email,
            String address,
            boolean emergencyContact,
            boolean primaryContact,
            boolean responsibleParty,
            String notes) {
    }

    record PatientContactResponse(
            UUID id,
            UUID patientId,
            String relationshipType,
            String fullName,
            String phone,
            String email,
            String address,
            boolean emergencyContact,
            boolean primaryContact,
            boolean responsibleParty,
            String notes,
            com.homehealthcare.patient.foundation.PatientLifecycleStatus status) {
    }
}
