package com.homehealthcare.platform.audit.api;

import com.homehealthcare.platform.audit.application.AuditEventQueryService;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventOutcome;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-events")
class AuditEventController {

    private final AuditEventQueryService auditEventQueryService;

    AuditEventController(AuditEventQueryService auditEventQueryService) {
        this.auditEventQueryService = auditEventQueryService;
    }

    @GetMapping
    AuditEventPageResponse events(
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "actorId", required = false) UUID actorId,
            @RequestParam(name = "actionType", required = false) String actionType,
            @RequestParam(name = "targetUserId", required = false) UUID targetUserId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<AuditEvent> result = auditEventQueryService.viewEvents(
                new AuditEventQueryService.AuditEventFilter(from, to, actorId, actionType, targetUserId),
                PageRequest.of(page, size));
        return new AuditEventPageResponse(
                result.getContent().stream().map(AuditEventController::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    String export(
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "actorId", required = false) UUID actorId,
            @RequestParam(name = "actionType", required = false) String actionType,
            @RequestParam(name = "targetUserId", required = false) UUID targetUserId) {
        List<AuditEvent> events = auditEventQueryService.exportEvents(
                new AuditEventQueryService.AuditEventFilter(from, to, actorId, actionType, targetUserId));
        StringBuilder csv = new StringBuilder()
                .append("occurredAt,actionType,outcome,actorEmail,actorId,targetType,targetId,agencyId,branchId,metadataJson\n");
        for (AuditEvent event : events) {
            csv.append(csv(event.getOccurredAt()))
                    .append(',').append(csv(event.getActionType()))
                    .append(',').append(csv(event.getOutcome().name()))
                    .append(',').append(csv(event.getActorEmail()))
                    .append(',').append(csv(event.getActorId()))
                    .append(',').append(csv(event.getTargetType()))
                    .append(',').append(csv(event.getTargetId()))
                    .append(',').append(csv(event.getAgencyId()))
                    .append(',').append(csv(event.getBranchId()))
                    .append(',').append(csv(event.getMetadataJson()))
                    .append('\n');
        }
        return csv.toString();
    }

    private static AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getActorType(),
                event.getActorId(),
                event.getActorEmail(),
                event.getActionType(),
                event.getTargetType(),
                event.getTargetId(),
                event.getAgencyId(),
                event.getBranchId(),
                event.getOutcome(),
                event.getMetadataJson());
    }

    private static String csv(Object value) {
        String raw = value == null ? "" : String.valueOf(value);
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    record AuditEventPageResponse(
            List<AuditEventResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    record AuditEventResponse(
            UUID id,
            Instant occurredAt,
            String actorType,
            UUID actorId,
            String actorEmail,
            String actionType,
            String targetType,
            UUID targetId,
            UUID agencyId,
            UUID branchId,
            AuditEventOutcome outcome,
            String metadataJson) {
    }
}
