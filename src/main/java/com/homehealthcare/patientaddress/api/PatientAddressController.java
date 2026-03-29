package com.homehealthcare.patientaddress.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patientaddress.application.PatientAddressService;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/{patientId}/address")
class PatientAddressController {

    private final PatientAddressRepository patientAddressRepository;
    private final PatientAddressService patientAddressService;
    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    PatientAddressController(
            PatientAddressRepository patientAddressRepository,
            PatientAddressService patientAddressService,
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.patientAddressRepository = patientAddressRepository;
        this.patientAddressService = patientAddressService;
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    PatientAddressResponse get(@PathVariable UUID patientId) {
        var actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_PATIENT_DIRECTORY, UnauthorizedPatientActorException::new);
        PatientAddress address = patientAddressRepository.findByPatient_Id(patientId)
                .filter(item -> actorMembership.getAgencyId().equals(item.getAgencyId()))
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientAddress", patientId));
        return toResponse(address);
    }

    @PutMapping
    PatientAddressResponse upsert(@PathVariable UUID patientId, @Valid @RequestBody ManagePatientAddressRequest request) {
        PatientAddress saved = patientAddressService.upsert(
                configurationActorResolver.requireActorMembership(),
                patientId,
                new PatientAddressService.ManagePatientAddressCommand(
                        request.addressLine1(),
                        request.addressLine2(),
                        request.city(),
                        request.state(),
                        request.postalCode(),
                        request.country(),
                        request.latitude(),
                        request.longitude(),
                        request.geocodeStatus(),
                        request.timezone(),
                        request.locationNotes()));
        return toResponse(saved);
    }

    private static PatientAddressResponse toResponse(PatientAddress address) {
        return new PatientAddressResponse(
                address.getId(),
                address.getPatient().getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getLatitude(),
                address.getLongitude(),
                address.getGeocodeStatus(),
                address.getTimezone(),
                address.getLocationNotes());
    }

    record ManagePatientAddressRequest(
            @NotBlank String addressLine1,
            String addressLine2,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String geocodeStatus,
            String timezone,
            String locationNotes) {
    }

    record PatientAddressResponse(
            UUID id,
            UUID patientId,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String geocodeStatus,
            String timezone,
            String locationNotes) {
    }
}
