package com.homehealthcare.patientattachment.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.application.PatientEntityNotFoundException;
import com.homehealthcare.patient.application.UnauthorizedPatientActorException;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patient.foundation.Epic3PatientTargetType;
import com.homehealthcare.patient.foundation.PatientAuditService;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachmentStatus;
import com.homehealthcare.patientattachment.storage.PatientAttachmentStorage;
import com.homehealthcare.patientattachment.storage.PatientAttachmentStorageProperties;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PatientAttachmentService {

    private final PatientRepository patientRepository;
    private final PatientAttachmentRepository patientAttachmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAttachmentStorage patientAttachmentStorage;
    private final PatientAttachmentStorageProperties patientAttachmentStorageProperties;
    private final PatientAuditService patientAuditService;

    @Transactional
    public PatientAttachment upload(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId, @NotNull UploadPatientAttachmentCommand command) {
        requireManageAttachments(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);

        String sanitizedFileName = sanitizeFileName(command.fileName());
        String normalizedContentType = normalizeContentType(command.contentType());
        byte[] content = command.content();
        validateUpload(normalizedContentType, content);

        PatientAttachmentStorage.StoredAttachment stored = patientAttachmentStorage.store(
                new PatientAttachmentStorage.StoreAttachmentRequest(
                        sanitizedFileName,
                        normalizedContentType,
                        content));

        PatientAttachment saved = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                actorMembership,
                sanitizedFileName,
                normalizedContentType,
                stored.sizeBytes(),
                stored.storageKey(),
                command.category(),
                command.description()));
        patientAuditService.recordAttachmentUploaded(actorMembership, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PatientAttachment> list(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId) {
        requireViewAttachments(actorMembership);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientEntityNotFoundException("Patient", patientId));
        assertSameAgency(actorMembership.getAgencyId(), patient.getAgencyId(), "Patient", patientId);
        return patientAttachmentRepository.findAllByPatient_IdOrderByCreatedAtDesc(patientId);
    }

    @Transactional
    public PatientAttachment updateMetadata(@NotNull AgencyMembership actorMembership, @NotNull UUID attachmentId, @NotNull UpdatePatientAttachmentMetadataCommand command) {
        requireManageAttachments(actorMembership);
        PatientAttachment attachment = patientAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientAttachment", attachmentId));
        assertSameAgency(actorMembership.getAgencyId(), attachment.getAgencyId(), "PatientAttachment", attachmentId);
        attachment.updateMetadata(command.category(), command.description());
        PatientAttachment saved = patientAttachmentRepository.saveAndFlush(attachment);
        patientAuditService.recordUpdated(actorMembership, Epic3PatientTargetType.PATIENT_ATTACHMENT, saved.getId(), null, metadata(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public DownloadedPatientAttachment download(@NotNull AgencyMembership actorMembership, @NotNull UUID attachmentId) {
        requireViewAttachments(actorMembership);
        PatientAttachment attachment = patientAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new PatientEntityNotFoundException("PatientAttachment", attachmentId));
        assertSameAgency(actorMembership.getAgencyId(), attachment.getAgencyId(), "PatientAttachment", attachmentId);
        PatientAttachmentStorage.LoadedAttachment loaded = patientAttachmentStorage.load(attachment.getStorageKey());
        patientAuditService.recordAttachmentDownloaded(actorMembership, attachment.getId(), null, metadata(attachment));
        return new DownloadedPatientAttachment(
                attachment.getFileName(),
                attachment.getContentType(),
                loaded.content());
    }

    private void requireViewAttachments(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_PATIENT_ATTACHMENTS,
                UnauthorizedPatientActorException::new);
    }

    private void requireManageAttachments(AgencyMembership actorMembership) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_PATIENT_ATTACHMENTS,
                UnauthorizedPatientActorException::new);
    }

    private void validateUpload(String contentType, byte[] content) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Attachment content must not be empty");
        }
        if (content.length > patientAttachmentStorageProperties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Attachment exceeds maximum allowed size");
        }
        boolean allowed = patientAttachmentStorageProperties.getAllowedContentTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(contentType));
        if (!allowed) {
            throw new IllegalArgumentException("Attachment content type is not allowed");
        }
    }

    private static String sanitizeFileName(String rawFileName) {
        String normalized = rawFileName == null ? null : rawFileName.replace('\\', '/');
        if (normalized == null) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        String sanitized = baseName.replaceAll("[\\p{Cntrl}]", "").trim();
        sanitized = sanitized.replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        return sanitized;
    }

    private static String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new PatientEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(PatientAttachment attachment) {
        return "{\"fileName\":\"" + attachment.getFileName()
                + "\",\"category\":\"" + attachment.getCategory()
                + "\",\"status\":\"" + attachment.getStatus().name() + "\"}";
    }

    public record UploadPatientAttachmentCommand(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull byte[] content,
            @NotBlank String category,
            String description) {
    }

    public record UpdatePatientAttachmentMetadataCommand(
            @NotBlank String category,
            String description) {
    }

    public record DownloadedPatientAttachment(
            String fileName,
            String contentType,
            byte[] content) {
    }
}
