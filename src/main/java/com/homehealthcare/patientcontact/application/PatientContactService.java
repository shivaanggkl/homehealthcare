package com.homehealthcare.patientcontact.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patientcontact.domain.PatientContact;
import com.homehealthcare.patientcontact.domain.PatientContactRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PatientContactService {

    private final PatientRepository patientRepository;
    private final PatientContactRepository patientContactRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientContact create(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientContactCommand command) {
        requireManageContacts(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        PatientContact saved = patientContactRepository.saveAndFlush(PatientContact.create(
                patient,
                command.relationshipType(),
                command.fullName(),
                command.phone(),
                command.email(),
                command.address(),
                command.emergencyContact(),
                command.primaryContact(),
                command.responsibleParty(),
                command.notes()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_CONTACT, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientContact update(@NotNull AgencyMembership actorMembership, @NotNull UUID contactId, @Valid ManagePatientContactCommand command) {
        requireManageContacts(actorMembership);
        PatientContact contact = patientContactRepository.findById(contactId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientContact", contactId));
        assertSameAgency(actorMembership.getAgencyId(), contact.getAgencyId(), "PatientContact", contactId);

        contact.updateDetails(
                command.relationshipType(),
                command.fullName(),
                command.phone(),
                command.email(),
                command.address(),
                command.emergencyContact(),
                command.primaryContact(),
                command.responsibleParty(),
                command.notes());
        PatientContact saved = patientContactRepository.saveAndFlush(contact);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_CONTACT, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientContact deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID contactId) {
        requireManageContacts(actorMembership);
        PatientContact contact = patientContactRepository.findById(contactId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientContact", contactId));
        assertSameAgency(actorMembership.getAgencyId(), contact.getAgencyId(), "PatientContact", contactId);
        contact.deactivate();
        PatientContact saved = patientContactRepository.saveAndFlush(contact);
        patientAuditService.recordDeactivated(actorMembership, Epic3PatientTargetType.PATIENT_CONTACT, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManageContacts(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_CONTACTS,
                UnauthorizedPatientActorException::new);
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(PatientContact contact) {
        return "{\"fullName\":\"" + contact.getFullName() + "\",\"status\":\"" + contact.getStatus().name() + "\"}";
    }

    public record ManagePatientContactCommand(
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
}
