package com.homehealthcare.patienteligibility.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibility;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityRepository;
import com.homehealthcare.patienteligibility.domain.PatientServiceEligibilityStatus;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import jakarta.validation.Valid;
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
public class PatientServiceEligibilityService {

    private final PatientRepository patientRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final PatientServiceEligibilityRepository patientServiceEligibilityRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientServiceEligibility create(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientServiceEligibilityCommand command) {
        requireManageEligibility(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        PatientServiceEligibility saved = patientServiceEligibilityRepository.saveAndFlush(PatientServiceEligibility.create(
                patient,
                serviceLine,
                command.status(),
                command.effectiveFrom(),
                command.effectiveTo(),
                command.verificationSource(),
                command.notes()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_SERVICE_ELIGIBILITY, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientServiceEligibility update(@NotNull AgencyMembership actorMembership, @NotNull UUID eligibilityId, @Valid ManagePatientServiceEligibilityCommand command) {
        requireManageEligibility(actorMembership);
        PatientServiceEligibility eligibility = patientServiceEligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientServiceEligibility", eligibilityId));
        assertSameAgency(actorMembership.getAgencyId(), eligibility.getAgencyId(), "PatientServiceEligibility", eligibilityId);

        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        eligibility.update(
                serviceLine,
                command.status(),
                command.effectiveFrom(),
                command.effectiveTo(),
                command.verificationSource(),
                command.notes());
        PatientServiceEligibility saved = patientServiceEligibilityRepository.saveAndFlush(eligibility);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_SERVICE_ELIGIBILITY, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientServiceEligibility deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID eligibilityId) {
        requireManageEligibility(actorMembership);
        PatientServiceEligibility eligibility = patientServiceEligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientServiceEligibility", eligibilityId));
        assertSameAgency(actorMembership.getAgencyId(), eligibility.getAgencyId(), "PatientServiceEligibility", eligibilityId);
        eligibility.deactivate();
        PatientServiceEligibility saved = patientServiceEligibilityRepository.saveAndFlush(eligibility);
        patientAuditService.recordDeactivated(actorMembership, Epic3PatientTargetType.PATIENT_SERVICE_ELIGIBILITY, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManageEligibility(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_ELIGIBILITY,
                UnauthorizedPatientActorException::new);
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> agencyId.equals(serviceLine.getAgencyId()))
                .orElseThrow(() -> new PatientEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(PatientServiceEligibility eligibility) {
        return "{\"status\":\"" + eligibility.getStatus().name() + "\",\"effectiveFrom\":\"" + eligibility.getEffectiveFrom() + "\"}";
    }

    public record ManagePatientServiceEligibilityCommand(
            UUID serviceLineId,
            @NotNull PatientServiceEligibilityStatus status,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String verificationSource,
            String notes) {
    }
}
