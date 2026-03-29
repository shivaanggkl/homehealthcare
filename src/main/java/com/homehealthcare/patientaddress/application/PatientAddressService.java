package com.homehealthcare.patientaddress.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PatientAddressService {

    private final PatientRepository patientRepository;
    private final PatientAddressRepository patientAddressRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientAddress upsert(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientAddressCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_ADDRESS,
                UnauthorizedPatientActorException::new);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        PatientAddress address = patientAddressRepository.findByPatient_Id(patientId)
                .orElseGet(() -> PatientAddress.create(
                        patient,
                        command.addressLine1(),
                        command.addressLine2(),
                        command.city(),
                        command.state(),
                        command.postalCode(),
                        command.country(),
                        command.latitude(),
                        command.longitude(),
                        command.geocodeStatus(),
                        command.timezone(),
                        command.locationNotes()));

        boolean created = address.getId() == null || !patientAddressRepository.existsById(address.getId());
        if (!created) {
            address.update(
                    command.addressLine1(),
                    command.addressLine2(),
                    command.city(),
                    command.state(),
                    command.postalCode(),
                    command.country(),
                    command.latitude(),
                    command.longitude(),
                    command.geocodeStatus(),
                    command.timezone(),
                    command.locationNotes());
        }

        PatientAddress saved = patientAddressRepository.saveAndFlush(address);
        if (created) {
            patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_ADDRESS, saved.getId(), null, metadata(saved));
        } else {
            patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_ADDRESS, saved.getId(), null, metadata(saved));
        }
        return saved;
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(PatientAddress address) {
        return "{\"city\":\"" + address.getCity() + "\",\"state\":\"" + address.getState() + "\",\"postalCode\":\"" + address.getPostalCode() + "\"}";
    }

    public record ManagePatientAddressCommand(
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
}
