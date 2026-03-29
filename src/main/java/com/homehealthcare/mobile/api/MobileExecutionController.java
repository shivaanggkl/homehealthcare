package com.homehealthcare.mobile.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.CaregiverRouteProjection;
import com.homehealthcare.mobile.application.MobileExecutionService.CaregiverTodayWorkItem;
import com.homehealthcare.mobile.application.MobileExecutionService.EndVisitExecutionCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.MobileCareInstructionSummary;
import com.homehealthcare.mobile.application.MobileExecutionService.MobilePatientSummary;
import com.homehealthcare.mobile.application.MobileExecutionService.SaveQuickNoteCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.SaveTaskChecklistItemCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService;
import com.homehealthcare.mobile.application.MobileFieldSupportService.CreateMobileIncidentCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.CreateMobileMessageThreadCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.DownloadedMobileArtifact;
import com.homehealthcare.mobile.application.MobileFieldSupportService.MobileMessageThreadDetail;
import com.homehealthcare.mobile.application.MobileFieldSupportService.MobileMessageThreadSummary;
import com.homehealthcare.mobile.application.MobileFieldSupportService.SendMobileMessageCommand;
import com.homehealthcare.mobile.application.MobileFieldSupportService.UploadMobileArtifactCommand;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.mobile.domain.MobileIncidentReport;
import com.homehealthcare.mobile.domain.MobileMessageEntry;
import com.homehealthcare.mobile.domain.MobileMessageThread;
import com.homehealthcare.mobile.domain.MobileQuickNoteEntry;
import com.homehealthcare.mobile.domain.MobileQuickNoteStatus;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitTaskChecklistEntry;
import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import com.homehealthcare.mobile.foundation.MobileSyncDisposition;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mobile")
class MobileExecutionController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final MobileExecutionService mobileExecutionService;
    private final MobileFieldSupportService mobileFieldSupportService;

    MobileExecutionController(
            ConfigurationActorResolver configurationActorResolver,
            MobileExecutionService mobileExecutionService,
            MobileFieldSupportService mobileFieldSupportService) {
        this.configurationActorResolver = configurationActorResolver;
        this.mobileExecutionService = mobileExecutionService;
        this.mobileFieldSupportService = mobileFieldSupportService;
    }

    @GetMapping("/home")
    MobileHomeResponse home(
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam(name = "timezone", defaultValue = "America/Chicago") String timezone) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        List<CaregiverTodayWorkItem> items = mobileExecutionService.getTodayWork(actorMembership, day, timezone);
        return new MobileHomeResponse(day, timezone, items.stream().map(MobileExecutionController::toTodayWorkItemResponse).toList());
    }

    @GetMapping("/route")
    RouteProjectionResponse route(
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam(name = "timezone", defaultValue = "America/Chicago") String timezone) {
        CaregiverRouteProjection projection = mobileExecutionService.getRouteProjection(
                configurationActorResolver.requireActorMembership(),
                day,
                timezone);
        return new RouteProjectionResponse(
                projection.caregiverProfileId(),
                projection.day(),
                projection.timezone(),
                projection.stops().stream().map(stop -> new RouteStopResponse(
                        stop.visitId(),
                        stop.patientDisplaySummary(),
                        stop.addressSummary(),
                        stop.plannedStartAt(),
                        stop.plannedEndAt(),
                        stop.sortOrder(),
                        stop.executionStatus())).toList());
    }

    @GetMapping("/visits/{visitId}")
    MobileVisitDetailResponse visitDetail(@PathVariable UUID visitId) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        MobilePatientSummary patientSummary = mobileExecutionService.getPatientSummary(actorMembership, visitId);
        MobileCareInstructionSummary careInstructions = mobileExecutionService.getCareInstructionSummary(actorMembership, visitId);
        return new MobileVisitDetailResponse(visitId, patientSummary, careInstructions);
    }

    @PostMapping("/visits/{visitId}/execution/start")
    MobileVisitExecutionSessionResponse startExecution(@PathVariable UUID visitId, @Valid @RequestBody StartExecutionRequest request) {
        MobileVisitExecutionSession session = mobileExecutionService.startVisitExecution(
                configurationActorResolver.requireActorMembership(),
                visitId,
                new StartVisitExecutionCommand(
                        request.startedAt(),
                        request.startedLatitude(),
                        request.startedLongitude(),
                        request.startSource(),
                        request.syncStatus()));
        return toExecutionSessionResponse(session);
    }

    @PostMapping("/execution-sessions/{executionSessionId}/end")
    MobileVisitExecutionSessionResponse endExecution(
            @PathVariable UUID executionSessionId,
            @Valid @RequestBody EndExecutionRequest request) {
        MobileVisitExecutionSession session = mobileExecutionService.endVisitExecution(
                configurationActorResolver.requireActorMembership(),
                executionSessionId,
                new EndVisitExecutionCommand(
                        request.endedAt(),
                        request.endedLatitude(),
                        request.endedLongitude(),
                        request.endSource(),
                        request.syncStatus()));
        return toExecutionSessionResponse(session);
    }

    @GetMapping("/visits/{visitId}/patient-summary")
    MobilePatientSummary patientSummary(@PathVariable UUID visitId) {
        return mobileExecutionService.getPatientSummary(configurationActorResolver.requireActorMembership(), visitId);
    }

    @GetMapping("/visits/{visitId}/care-instructions")
    MobileCareInstructionSummary careInstructions(@PathVariable UUID visitId) {
        return mobileExecutionService.getCareInstructionSummary(configurationActorResolver.requireActorMembership(), visitId);
    }

    @PutMapping("/execution-sessions/{executionSessionId}/task-checklist")
    List<MobileTaskChecklistResponse> saveTaskChecklist(
            @PathVariable UUID executionSessionId,
            @Valid @RequestBody List<SaveTaskChecklistItemRequest> request) {
        return mobileExecutionService.saveTaskChecklist(
                        configurationActorResolver.requireActorMembership(),
                        executionSessionId,
                        request.stream().map(item -> new SaveTaskChecklistItemCommand(
                                item.taskTemplateId(),
                                item.title(),
                                item.description(),
                                item.category(),
                                item.sortOrder(),
                                item.completed(),
                                item.completedAt(),
                                item.completionNotes())).toList())
                .stream()
                .map(MobileExecutionController::toTaskChecklistResponse)
                .toList();
    }

    @PostMapping("/execution-sessions/{executionSessionId}/quick-notes")
    MobileQuickNoteResponse saveQuickNote(
            @PathVariable UUID executionSessionId,
            @Valid @RequestBody SaveQuickNoteRequest request) {
        MobileQuickNoteEntry note = mobileExecutionService.saveQuickNote(
                configurationActorResolver.requireActorMembership(),
                executionSessionId,
                new SaveQuickNoteCommand(request.status(), request.noteText(), request.authoredAt()));
        return toQuickNoteResponse(note);
    }

    @PostMapping(path = "/execution-sessions/{executionSessionId}/artifacts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MobileFieldArtifactResponse uploadArtifact(
            @PathVariable UUID executionSessionId,
            @ModelAttribute UploadArtifactRequest request) throws Exception {
        MobileFieldArtifact artifact = mobileFieldSupportService.uploadArtifact(
                configurationActorResolver.requireActorMembership(),
                executionSessionId,
                new UploadMobileArtifactCommand(
                        request.artifactType(),
                        request.file().getOriginalFilename(),
                        request.file().getContentType(),
                        request.file().getBytes(),
                        request.description()));
        return toFieldArtifactResponse(artifact);
    }

    @GetMapping("/artifacts/{artifactId}/download")
    ResponseEntity<ByteArrayResource> downloadArtifact(@PathVariable UUID artifactId) {
        DownloadedMobileArtifact downloaded = mobileFieldSupportService.downloadArtifact(
                configurationActorResolver.requireActorMembership(),
                artifactId);
        ByteArrayResource resource = new ByteArrayResource(downloaded.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .contentLength(downloaded.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(downloaded.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    @PostMapping("/execution-sessions/{executionSessionId}/incidents")
    MobileIncidentResponse createIncident(
            @PathVariable UUID executionSessionId,
            @Valid @RequestBody CreateIncidentRequest request) {
        MobileIncidentReport incident = mobileFieldSupportService.createIncident(
                configurationActorResolver.requireActorMembership(),
                executionSessionId,
                new CreateMobileIncidentCommand(
                        request.incidentType(),
                        request.severity(),
                        request.narrative(),
                        request.reportedAt(),
                        request.escalationHook(),
                        request.artifactIds()));
        return toIncidentResponse(incident);
    }

    @GetMapping("/messages/threads")
    List<MobileMessageThreadSummary> threadSummaries() {
        return mobileFieldSupportService.getThreadSummaries(configurationActorResolver.requireActorMembership());
    }

    @PostMapping("/execution-sessions/{executionSessionId}/messages/threads")
    MobileMessageThreadResponse createThread(
            @PathVariable UUID executionSessionId,
            @Valid @RequestBody CreateMessageThreadRequest request) {
        MobileMessageThread thread = mobileFieldSupportService.createThread(
                configurationActorResolver.requireActorMembership(),
                executionSessionId,
                new CreateMobileMessageThreadCommand(request.subject()));
        return toThreadResponse(thread);
    }

    @GetMapping("/messages/threads/{threadId}")
    MobileMessageThreadDetail threadDetail(@PathVariable UUID threadId) {
        return mobileFieldSupportService.getThreadDetail(configurationActorResolver.requireActorMembership(), threadId);
    }

    @PostMapping("/messages/threads/{threadId}/messages")
    MobileMessageEntryResponse sendMessage(
            @PathVariable UUID threadId,
            @Valid @RequestBody SendMessageRequest request) {
        MobileMessageEntry message = mobileFieldSupportService.sendMessage(
                configurationActorResolver.requireActorMembership(),
                threadId,
                new SendMobileMessageCommand(request.messageText(), request.sentAt()));
        return new MobileMessageEntryResponse(
                message.getId(),
                message.getThread().getId(),
                message.getSenderMembership().getId(),
                message.getSentAt(),
                message.getMessageText());
    }

    private static MobileHomeTodayWorkItemResponse toTodayWorkItemResponse(CaregiverTodayWorkItem item) {
        return new MobileHomeTodayWorkItemResponse(
                item.visitId(),
                item.patientDisplaySummary(),
                item.branchName(),
                item.plannedStartAt(),
                item.plannedEndAt(),
                item.timezone(),
                item.scheduleStatus(),
                item.routeOrder(),
                item.executionStatus());
    }

    private static MobileVisitExecutionSessionResponse toExecutionSessionResponse(MobileVisitExecutionSession session) {
        return new MobileVisitExecutionSessionResponse(
                session.getId(),
                session.getVisitOccurrenceId(),
                session.getCaregiverProfileId(),
                session.getPatientId(),
                session.getBranchId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getStartedLatitude(),
                session.getStartedLongitude(),
                session.getEndedLatitude(),
                session.getEndedLongitude(),
                session.getStartSource(),
                session.getEndSource(),
                session.getExecutionStatus(),
                session.getSyncStatus());
    }

    private static MobileTaskChecklistResponse toTaskChecklistResponse(MobileVisitTaskChecklistEntry entry) {
        return new MobileTaskChecklistResponse(
                entry.getId(),
                entry.getExecutionSessionId(),
                entry.getTaskTemplate() == null ? null : entry.getTaskTemplate().getId(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getCategory(),
                entry.getSortOrder(),
                entry.isCompleted(),
                entry.getCompletedAt(),
                entry.getCompletionNotes());
    }

    private static MobileQuickNoteResponse toQuickNoteResponse(MobileQuickNoteEntry entry) {
        return new MobileQuickNoteResponse(
                entry.getId(),
                entry.getExecutionSession().getId(),
                entry.getCaregiverProfile().getId(),
                entry.getAuthoredAt(),
                entry.getNoteText(),
                entry.getStatus());
    }

    private static MobileFieldArtifactResponse toFieldArtifactResponse(MobileFieldArtifact artifact) {
        return new MobileFieldArtifactResponse(
                artifact.getId(),
                artifact.getExecutionSessionId(),
                artifact.getVisitOccurrenceId(),
                artifact.getArtifactType(),
                artifact.getFileName(),
                artifact.getContentType(),
                artifact.getSizeBytes(),
                artifact.getDescription(),
                artifact.getStatus(),
                artifact.getCreatedAt());
    }

    private static MobileIncidentResponse toIncidentResponse(MobileIncidentReport incident) {
        return new MobileIncidentResponse(
                incident.getId(),
                incident.getExecutionSession().getId(),
                incident.getVisitOccurrence().getId(),
                incident.getIncidentType(),
                incident.getSeverity(),
                incident.getNarrative(),
                incident.getReportedAt(),
                incident.getStatus(),
                incident.getEscalationHook(),
                incident.getArtifacts().stream().map(MobileFieldArtifact::getId).toList());
    }

    private static MobileMessageThreadResponse toThreadResponse(MobileMessageThread thread) {
        return new MobileMessageThreadResponse(
                thread.getId(),
                thread.getSubject(),
                thread.getPatient() == null ? null : thread.getPatient().getId(),
                thread.getVisitOccurrence() == null ? null : thread.getVisitOccurrence().getId(),
                thread.getLastMessageAt(),
                thread.getStatus());
    }

    record MobileHomeResponse(
            LocalDate day,
            String timezone,
            List<MobileHomeTodayWorkItemResponse> visits) {
    }

    record MobileHomeTodayWorkItemResponse(
            UUID visitId,
            String patientDisplaySummary,
            String branchName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String timezone,
            String scheduleStatus,
            Integer routeOrder,
            MobileExecutionSessionStatus executionStatus) {
    }

    record RouteProjectionResponse(
            UUID caregiverProfileId,
            LocalDate day,
            String timezone,
            List<RouteStopResponse> stops) {
    }

    record RouteStopResponse(
            UUID visitId,
            String patientDisplaySummary,
            String addressSummary,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            int sortOrder,
            MobileExecutionSessionStatus executionStatus) {
    }

    record MobileVisitDetailResponse(
            UUID visitId,
            MobilePatientSummary patientSummary,
            MobileCareInstructionSummary careInstructions) {
    }

    record StartExecutionRequest(
            @NotNull OffsetDateTime startedAt,
            BigDecimal startedLatitude,
            BigDecimal startedLongitude,
            String startSource,
            MobileSyncDisposition syncStatus) {
    }

    record EndExecutionRequest(
            @NotNull OffsetDateTime endedAt,
            BigDecimal endedLatitude,
            BigDecimal endedLongitude,
            String endSource,
            MobileSyncDisposition syncStatus) {
    }

    record MobileVisitExecutionSessionResponse(
            UUID id,
            UUID visitOccurrenceId,
            UUID caregiverProfileId,
            UUID patientId,
            UUID branchId,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            BigDecimal startedLatitude,
            BigDecimal startedLongitude,
            BigDecimal endedLatitude,
            BigDecimal endedLongitude,
            String startSource,
            String endSource,
            MobileExecutionSessionStatus executionStatus,
            MobileSyncDisposition syncStatus) {
    }

    record SaveTaskChecklistItemRequest(
            UUID taskTemplateId,
            @NotBlank String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
    }

    record MobileTaskChecklistResponse(
            UUID id,
            UUID executionSessionId,
            UUID taskTemplateId,
            String title,
            String description,
            TaskTemplateCategory category,
            int sortOrder,
            boolean completed,
            OffsetDateTime completedAt,
            String completionNotes) {
    }

    record SaveQuickNoteRequest(
            @NotNull MobileQuickNoteStatus status,
            @NotBlank String noteText,
            OffsetDateTime authoredAt) {
    }

    record MobileQuickNoteResponse(
            UUID id,
            UUID executionSessionId,
            UUID caregiverProfileId,
            OffsetDateTime authoredAt,
            String noteText,
            MobileQuickNoteStatus status) {
    }

    record UploadArtifactRequest(
            @NotNull MobileFieldArtifactType artifactType,
            MultipartFile file,
            String description) {
    }

    record MobileFieldArtifactResponse(
            UUID id,
            UUID executionSessionId,
            UUID visitOccurrenceId,
            MobileFieldArtifactType artifactType,
            String fileName,
            String contentType,
            long sizeBytes,
            String description,
            com.homehealthcare.mobile.domain.MobileFieldArtifactStatus status,
            java.time.Instant uploadedAt) {
    }

    record CreateIncidentRequest(
            @NotBlank String incidentType,
            String severity,
            @NotBlank String narrative,
            OffsetDateTime reportedAt,
            String escalationHook,
            Set<UUID> artifactIds) {
    }

    record MobileIncidentResponse(
            UUID id,
            UUID executionSessionId,
            UUID visitOccurrenceId,
            String incidentType,
            String severity,
            String narrative,
            OffsetDateTime reportedAt,
            com.homehealthcare.mobile.domain.MobileIncidentStatus status,
            String escalationHook,
            List<UUID> artifactIds) {
    }

    record CreateMessageThreadRequest(@NotBlank String subject) {
    }

    record MobileMessageThreadResponse(
            UUID id,
            String subject,
            UUID patientId,
            UUID visitOccurrenceId,
            OffsetDateTime lastMessageAt,
            com.homehealthcare.mobile.domain.MobileMessageThreadStatus status) {
    }

    record SendMessageRequest(@NotBlank String messageText, OffsetDateTime sentAt) {
    }

    record MobileMessageEntryResponse(
            UUID id,
            UUID threadId,
            UUID senderMembershipId,
            OffsetDateTime sentAt,
            String messageText) {
    }
}
