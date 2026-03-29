package com.homehealthcare.patientpayer.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.InvalidPatientStateTransitionException;
import com.homehealthcare.patient.application.PatientConflictException;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PatientPayerLinkService {

    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    private final PatientRepository patientRepository;
    private final PatientPayerLinkRepository patientPayerLinkRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientPayerLink create(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @Valid ManagePatientPayerLinkCommand command) {
        requireManagePayerLinkage(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);
        validatePrimaryPayerInvariant(patientId, command.primaryPayer(), null);
        validateNoOverlappingCoverage(patientId, command, null);

        PatientPayerLink saved = patientPayerLinkRepository.saveAndFlush(PatientPayerLink.create(
                patient,
                command.payerName(),
                command.payerExternalId(),
                command.memberPolicyNumber(),
                command.groupNumber(),
                command.effectiveFrom(),
                command.effectiveTo(),
                command.primaryPayer(),
                command.status(),
                command.notes()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_PAYER_LINK, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientPayerLink update(@NotNull AgencyMembership actorMembership, @NotNull UUID payerLinkId, @Valid ManagePatientPayerLinkCommand command) {
        requireManagePayerLinkage(actorMembership);
        PatientPayerLink payerLink = patientPayerLinkRepository.findById(payerLinkId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientPayerLink", payerLinkId));
        assertSameAgency(actorMembership.getAgencyId(), payerLink.getAgencyId(), "PatientPayerLink", payerLinkId);
        validateStatusTransition(payerLink.getStatus(), command.status());
        validatePrimaryPayerInvariant(payerLink.getPatient().getId(), command.primaryPayer(), payerLinkId);
        validateNoOverlappingCoverage(payerLink.getPatient().getId(), command, payerLinkId);

        payerLink.update(
                command.payerName(),
                command.payerExternalId(),
                command.memberPolicyNumber(),
                command.groupNumber(),
                command.effectiveFrom(),
                command.effectiveTo(),
                command.primaryPayer(),
                command.status(),
                command.notes());
        PatientPayerLink saved = patientPayerLinkRepository.saveAndFlush(payerLink);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_PAYER_LINK, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManagePayerLinkage(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_PAYER_LINKAGE,
                UnauthorizedPatientActorException::new);
    }

    private void validatePrimaryPayerInvariant(UUID patientId, boolean primaryPayer, UUID existingId) {
        if (!primaryPayer) {
            return;
        }
        long primaryCount = patientPayerLinkRepository.countByPatient_IdAndPrimaryPayerTrueAndStatusIn(
                patientId,
                List.of(PatientPayerLinkStatus.ACTIVE, PatientPayerLinkStatus.PENDING));
        if (existingId == null ? primaryCount > 0 : primaryCount > 1) {
            throw new PatientConflictException("Only one active or pending primary payer link is allowed per patient");
        }
    }

    private void validateNoOverlappingCoverage(UUID patientId, ManagePatientPayerLinkCommand command, UUID existingId) {
        boolean overlap = patientPayerLinkRepository.existsOverlappingCoverage(
                patientId,
                normalize(command.payerName()),
                normalize(command.payerExternalId()),
                normalize(command.memberPolicyNumber()),
                command.effectiveFrom(),
                command.effectiveTo() == null ? OPEN_ENDED_DATE : command.effectiveTo(),
                OPEN_ENDED_DATE,
                existingId);
        if (overlap) {
            throw new PatientConflictException("Overlapping payer coverage exists for the same patient and payer identity");
        }
    }

    private void validateStatusTransition(PatientPayerLinkStatus current, PatientPayerLinkStatus next) {
        if (current == PatientPayerLinkStatus.TERMINATED && next == PatientPayerLinkStatus.ACTIVE) {
            throw new InvalidPatientStateTransitionException("Terminated payer links cannot transition back to ACTIVE");
        }
        if (current == PatientPayerLinkStatus.EXPIRED && next == PatientPayerLinkStatus.PENDING) {
            throw new InvalidPatientStateTransitionException("Expired payer links cannot transition back to PENDING");
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String normalize(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String metadata(PatientPayerLink payerLink) {
        return "{\"status\":\"" + payerLink.getStatus().name() + "\",\"primaryPayer\":" + payerLink.isPrimaryPayer() + "}";
    }

    public record ManagePatientPayerLinkCommand(
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
}
