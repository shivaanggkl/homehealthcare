package com.homehealthcare.patientauthorization.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.InvalidPatientStateTransitionException;
import com.homehealthcare.patient.application.PatientConflictException;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
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
public class PatientEpisodeAuthorizationService {

    private final PatientRepository patientRepository;
    private final PatientPayerLinkRepository patientPayerLinkRepository;
    private final ServiceLineRepository serviceLineRepository;
    private final PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientEpisodeAuthorization create(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID patientId,
            @Valid ManagePatientEpisodeAuthorizationCommand command) {
        requireManageAuthorizations(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        PatientPayerLink payerLink = resolvePayerLink(patient.getId(), actorMembership.getAgencyId(), command.patientPayerLinkId());
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        assertNoWindowConflict(patientId, command.authorizationNumber(), command.serviceLineId(), command.startDate(), command.endDate(), null);

        PatientEpisodeAuthorization saved = patientEpisodeAuthorizationRepository.saveAndFlush(PatientEpisodeAuthorization.create(
                patient,
                payerLink,
                serviceLine,
                command.authorizationNumber(),
                command.startDate(),
                command.endDate(),
                command.authorizedUnits(),
                command.usedUnits(),
                command.status(),
                command.notes()));
        patientAuditService.recordCreated(actorMembership, Epic3PatientTargetType.PATIENT_EPISODE_AUTHORIZATION, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional
    public PatientEpisodeAuthorization update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID authorizationId,
            @Valid ManagePatientEpisodeAuthorizationCommand command) {
        requireManageAuthorizations(actorMembership);
        PatientEpisodeAuthorization authorization = patientEpisodeAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientEpisodeAuthorization", authorizationId));
        assertSameAgency(actorMembership.getAgencyId(), authorization.getAgencyId(), "PatientEpisodeAuthorization", authorizationId);
        validateStatusTransition(authorization.getStatus(), command.status());

        PatientPayerLink payerLink = resolvePayerLink(authorization.getPatient().getId(), actorMembership.getAgencyId(), command.patientPayerLinkId());
        ServiceLine serviceLine = resolveServiceLine(actorMembership.getAgencyId(), command.serviceLineId());
        assertNoWindowConflict(
                authorization.getPatient().getId(),
                command.authorizationNumber(),
                command.serviceLineId(),
                command.startDate(),
                command.endDate(),
                authorizationId);

        authorization.update(
                payerLink,
                serviceLine,
                command.authorizationNumber(),
                command.startDate(),
                command.endDate(),
                command.authorizedUnits(),
                command.usedUnits(),
                command.status(),
                command.notes());
        PatientEpisodeAuthorization saved = patientEpisodeAuthorizationRepository.saveAndFlush(authorization);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_EPISODE_AUTHORIZATION, saved.getId(), null, metadata(saved));
        return saved;
    }

    private void requireManageAuthorizations(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_AUTHORIZATIONS,
                UnauthorizedPatientActorException::new);
    }

    private PatientPayerLink resolvePayerLink(UUID patientId, UUID agencyId, UUID payerLinkId) {
        if (payerLinkId == null) {
            return null;
        }
        return patientPayerLinkRepository.findById(payerLinkId)
                .filter(link -> agencyId.equals(link.getAgencyId()) && patientId.equals(link.getPatient().getId()))
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientPayerLink", payerLinkId));
    }

    private ServiceLine resolveServiceLine(UUID agencyId, UUID serviceLineId) {
        if (serviceLineId == null) {
            return null;
        }
        return serviceLineRepository.findById(serviceLineId)
                .filter(serviceLine -> agencyId.equals(serviceLine.getAgencyId()))
                .orElseThrow(() -> new PatientEntityNotFoundException("ServiceLine", serviceLineId));
    }

    private void assertNoWindowConflict(
            UUID patientId,
            String authorizationNumber,
            UUID serviceLineId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeId) {
        boolean conflict = patientEpisodeAuthorizationRepository.existsConflictingWindow(
                patientId,
                normalize(authorizationNumber),
                serviceLineId,
                startDate,
                endDate,
                excludeId);
        if (conflict) {
            throw new PatientConflictException("Overlapping patient authorization window exists for the same authorization identity");
        }
    }

    private void validateStatusTransition(PatientEpisodeAuthorizationStatus current, PatientEpisodeAuthorizationStatus next) {
        if (current == PatientEpisodeAuthorizationStatus.CANCELLED && next == PatientEpisodeAuthorizationStatus.ACTIVE) {
            throw new InvalidPatientStateTransitionException("Cancelled authorizations cannot transition back to ACTIVE");
        }
        if (current == PatientEpisodeAuthorizationStatus.EXPIRED && next == PatientEpisodeAuthorizationStatus.PENDING) {
            throw new InvalidPatientStateTransitionException("Expired authorizations cannot transition back to PENDING");
        }
        if (current == PatientEpisodeAuthorizationStatus.EXHAUSTED && next == PatientEpisodeAuthorizationStatus.PENDING) {
            throw new InvalidPatientStateTransitionException("Exhausted authorizations cannot transition back to PENDING");
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

    private static String metadata(PatientEpisodeAuthorization authorization) {
        return "{\"status\":\"" + authorization.getStatus().name() + "\",\"startDate\":\"" + authorization.getStartDate() + "\"}";
    }

    public record ManagePatientEpisodeAuthorizationCommand(
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
}
