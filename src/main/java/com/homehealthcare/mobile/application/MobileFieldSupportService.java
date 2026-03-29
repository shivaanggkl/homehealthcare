package com.homehealthcare.mobile.application;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.mobile.domain.MobileIncidentReport;
import com.homehealthcare.mobile.domain.MobileIncidentReportRepository;
import com.homehealthcare.mobile.domain.MobileMessageEntry;
import com.homehealthcare.mobile.domain.MobileMessageEntryRepository;
import com.homehealthcare.mobile.domain.MobileMessageThread;
import com.homehealthcare.mobile.domain.MobileMessageThreadRepository;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.foundation.MobileAuditService;
import com.homehealthcare.mobile.storage.MobileFieldArtifactStorageProperties;
import com.homehealthcare.patientattachment.storage.PatientAttachmentStorage;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MobileFieldSupportService {

    private final MobileExecutionService mobileExecutionService;
    private final MobileFieldArtifactRepository mobileFieldArtifactRepository;
    private final MobileIncidentReportRepository mobileIncidentReportRepository;
    private final MobileMessageThreadRepository mobileMessageThreadRepository;
    private final MobileMessageEntryRepository mobileMessageEntryRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final PatientAttachmentStorage patientAttachmentStorage;
    private final MobileFieldArtifactStorageProperties mobileFieldArtifactStorageProperties;
    private final MobileAuditService mobileAuditService;

    @Transactional
    public MobileFieldArtifact uploadArtifact(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @Valid UploadMobileArtifactCommand command) {
        requirePermission(actorMembership, AgencyPermission.UPLOAD_MOBILE_VISIT_ARTIFACTS);
        MobileVisitExecutionSession session = mobileExecutionService.resolveOwnedSessionContext(actorMembership, executionSessionId);

        String fileName = sanitizeFileName(command.fileName());
        String contentType = normalizeContentType(command.contentType());
        validateUpload(contentType, command.content());

        PatientAttachmentStorage.StoredAttachment stored = patientAttachmentStorage.store(
                new PatientAttachmentStorage.StoreAttachmentRequest(fileName, contentType, command.content()));

        MobileFieldArtifact saved = mobileFieldArtifactRepository.saveAndFlush(MobileFieldArtifact.create(
                session,
                session.getVisitOccurrence(),
                session.getCaregiverProfile(),
                session.getPatient(),
                session.getBranch(),
                command.artifactType(),
                fileName,
                contentType,
                stored.sizeBytes(),
                stored.storageKey(),
                command.description()));
        if (command.artifactType() == MobileFieldArtifactType.PHOTO) {
            mobileAuditService.recordPhotoUploaded(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), artifactMetadata(saved));
        } else {
            mobileAuditService.recordSignatureCaptured(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), artifactMetadata(saved));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public DownloadedMobileArtifact downloadArtifact(@NotNull AgencyMembership actorMembership, @NotNull UUID artifactId) {
        requirePermission(actorMembership, AgencyPermission.UPLOAD_MOBILE_VISIT_ARTIFACTS);
        CaregiverProfile caregiverProfile = mobileExecutionService.resolveActorCaregiverProfile(actorMembership);
        MobileFieldArtifact artifact = mobileFieldArtifactRepository.findByIdAndAgency_Id(artifactId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MobileEntityNotFoundException("MobileFieldArtifact", artifactId));
        if (!Objects.equals(artifact.getCaregiverProfile().getId(), caregiverProfile.getId())) {
            throw new UnauthorizedMobileActorException(actorMembership.getId());
        }
        PatientAttachmentStorage.LoadedAttachment loaded = patientAttachmentStorage.load(artifact.getStorageKey());
        return new DownloadedMobileArtifact(artifact.getFileName(), artifact.getContentType(), loaded.content());
    }

    @Transactional
    public MobileIncidentReport createIncident(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @Valid CreateMobileIncidentCommand command) {
        requirePermission(actorMembership, AgencyPermission.CREATE_MOBILE_INCIDENTS);
        MobileVisitExecutionSession session = mobileExecutionService.resolveOwnedSessionContext(actorMembership, executionSessionId);
        Set<MobileFieldArtifact> artifacts = command.artifactIds() == null
                ? Set.of()
                : command.artifactIds().stream()
                        .map(id -> mobileFieldArtifactRepository.findByIdAndAgency_Id(id, actorMembership.getAgencyId())
                                .orElseThrow(() -> new MobileEntityNotFoundException("MobileFieldArtifact", id)))
                        .peek(artifact -> {
                            if (!Objects.equals(artifact.getExecutionSessionId(), session.getId())) {
                                throw new MobileConflictException("Incident artifacts must belong to the same execution session.");
                            }
                        })
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        MobileIncidentReport saved = mobileIncidentReportRepository.saveAndFlush(MobileIncidentReport.create(
                session,
                session.getVisitOccurrence(),
                session.getCaregiverProfile(),
                session.getPatient(),
                session.getBranch(),
                command.incidentType(),
                command.severity(),
                command.narrative(),
                coalesce(command.reportedAt(), OffsetDateTime.now()),
                command.escalationHook(),
                artifacts));
        mobileAuditService.recordIncidentFlagged(actorMembership, saved.getId(), saved.getBranch() == null ? null : saved.getBranch().getId(), incidentMetadata(saved));
        return saved;
    }

    @Transactional
    public MobileMessageThread createThread(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID executionSessionId,
            @Valid CreateMobileMessageThreadCommand command) {
        requirePermission(actorMembership, AgencyPermission.SEND_MOBILE_MESSAGES);
        MobileVisitExecutionSession session = mobileExecutionService.resolveOwnedSessionContext(actorMembership, executionSessionId);
        return mobileMessageThreadRepository.saveAndFlush(MobileMessageThread.create(
                session.getCaregiverProfile(),
                session.getBranch(),
                session.getPatient(),
                session.getVisitOccurrence(),
                command.subject()));
    }

    @Transactional(readOnly = true)
    public List<MobileMessageThreadSummary> getThreadSummaries(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MOBILE_MESSAGES);
        CaregiverProfile caregiverProfile = mobileExecutionService.resolveActorCaregiverProfile(actorMembership);
        return mobileMessageThreadRepository.findAllByCaregiverProfile_IdOrderByLastMessageAtDescCreatedAtDesc(caregiverProfile.getId()).stream()
                .map(thread -> {
                    List<MobileMessageEntry> messages = mobileMessageEntryRepository.findAllByThread_IdOrderBySentAtAsc(thread.getId());
                    MobileMessageEntry lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
                    return new MobileMessageThreadSummary(
                            thread.getId(),
                            List.of(caregiverProfile.getDisplayName() == null ? "Caregiver" : caregiverProfile.getDisplayName(), "Agency operations"),
                            lastMessage == null ? null : truncate(lastMessage.getMessageText(), 120),
                            0L,
                            thread.getPatient() == null ? null : thread.getPatient().getId(),
                            thread.getVisitOccurrence() == null ? null : thread.getVisitOccurrence().getId(),
                            thread.getLastMessageAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MobileMessageThreadDetail getThreadDetail(@NotNull AgencyMembership actorMembership, @NotNull UUID threadId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MOBILE_MESSAGES);
        MobileMessageThread thread = resolveOwnedThread(actorMembership, threadId);
        List<MobileMessageThreadMessage> messages = mobileMessageEntryRepository.findAllByThread_IdOrderBySentAtAsc(thread.getId()).stream()
                .map(message -> new MobileMessageThreadMessage(
                        message.getId(),
                        message.getSenderMembership().getId(),
                        message.getSenderMembership().getUser().getEmail(),
                        message.getSentAt(),
                        message.getMessageText()))
                .toList();
        return new MobileMessageThreadDetail(
                thread.getId(),
                thread.getSubject(),
                thread.getPatient() == null ? null : thread.getPatient().getId(),
                thread.getVisitOccurrence() == null ? null : thread.getVisitOccurrence().getId(),
                messages);
    }

    @Transactional
    public MobileMessageEntry sendMessage(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID threadId,
            @Valid SendMobileMessageCommand command) {
        requirePermission(actorMembership, AgencyPermission.SEND_MOBILE_MESSAGES);
        MobileMessageThread thread = resolveOwnedThread(actorMembership, threadId);
        MobileMessageEntry saved = mobileMessageEntryRepository.saveAndFlush(MobileMessageEntry.create(
                thread,
                actorMembership,
                coalesce(command.sentAt(), OffsetDateTime.now()),
                command.messageText()));
        thread.touchMessage(saved.getSentAt());
        mobileMessageThreadRepository.saveAndFlush(thread);
        mobileAuditService.recordMessageSent(actorMembership, thread.getId(), thread.getBranch() == null ? null : thread.getBranch().getId(), messageMetadata(thread, saved));
        return saved;
    }

    private MobileMessageThread resolveOwnedThread(AgencyMembership actorMembership, UUID threadId) {
        CaregiverProfile caregiverProfile = mobileExecutionService.resolveActorCaregiverProfile(actorMembership);
        MobileMessageThread thread = mobileMessageThreadRepository.findByIdAndAgency_Id(threadId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MobileEntityNotFoundException("MobileMessageThread", threadId));
        if (!Objects.equals(thread.getCaregiverProfile().getId(), caregiverProfile.getId())) {
            throw new UnauthorizedMobileActorException(actorMembership.getId());
        }
        return thread;
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedMobileActorException::new);
    }

    private void validateUpload(String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Artifact content must not be empty");
        }
        if (content.length > mobileFieldArtifactStorageProperties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("Artifact exceeds maximum allowed size");
        }
        boolean allowed = mobileFieldArtifactStorageProperties.getAllowedContentTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(contentType));
        if (!allowed) {
            throw new IllegalArgumentException("Artifact content type is not allowed");
        }
    }

    private static String sanitizeFileName(String rawFileName) {
        String normalized = rawFileName == null ? null : rawFileName.replace('\\', '/');
        if (normalized == null) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        String sanitized = baseName.replaceAll("[\\p{Cntrl}]", "").trim().replaceAll("\\s+", " ");
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

    private static <T> T coalesce(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static String artifactMetadata(MobileFieldArtifact artifact) {
        return "{\"artifactType\":\"" + artifact.getArtifactType().name() + "\",\"fileName\":\"" + artifact.getFileName() + "\"}";
    }

    private static String incidentMetadata(MobileIncidentReport incident) {
        return "{\"incidentType\":\"" + incident.getIncidentType() + "\",\"artifactCount\":" + incident.getArtifacts().size() + "}";
    }

    private static String messageMetadata(MobileMessageThread thread, MobileMessageEntry message) {
        return "{\"threadId\":\"" + thread.getId() + "\",\"messageId\":\"" + message.getId() + "\"}";
    }

    public record UploadMobileArtifactCommand(
            @NotNull MobileFieldArtifactType artifactType,
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull byte[] content,
            String description) {
    }

    public record DownloadedMobileArtifact(String fileName, String contentType, byte[] content) {
    }

    public record CreateMobileIncidentCommand(
            @NotBlank String incidentType,
            String severity,
            @NotBlank String narrative,
            OffsetDateTime reportedAt,
            String escalationHook,
            Set<UUID> artifactIds) {
    }

    public record CreateMobileMessageThreadCommand(@NotBlank String subject) {
    }

    public record SendMobileMessageCommand(@NotBlank String messageText, OffsetDateTime sentAt) {
    }

    public record MobileMessageThreadSummary(
            UUID threadId,
            List<String> participantsSummary,
            String lastMessagePreview,
            long unreadCount,
            UUID patientId,
            UUID visitOccurrenceId,
            OffsetDateTime lastMessageAt) {
    }

    public record MobileMessageThreadDetail(
            UUID threadId,
            String subject,
            UUID patientId,
            UUID visitOccurrenceId,
            List<MobileMessageThreadMessage> messages) {
    }

    public record MobileMessageThreadMessage(
            UUID messageId,
            UUID senderMembershipId,
            String senderEmail,
            OffsetDateTime sentAt,
            String messageText) {
    }
}
