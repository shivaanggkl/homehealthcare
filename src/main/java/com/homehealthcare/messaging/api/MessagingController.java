package com.homehealthcare.messaging.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.messaging.application.MessagingCoordinationService;
import com.homehealthcare.messaging.application.MessagingCoordinationService.CoordinationSummary;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageBranchBroadcastCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageEscalationCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageStaffGroupCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ManageThreadCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.SendMessageCommand;
import com.homehealthcare.messaging.application.MessagingCoordinationService.StaffGroupAggregate;
import com.homehealthcare.messaging.application.MessagingCoordinationService.ThreadDetail;
import com.homehealthcare.messaging.domain.BranchBroadcast;
import com.homehealthcare.messaging.domain.CommunicationContextLink;
import com.homehealthcare.messaging.domain.CommunicationMessage;
import com.homehealthcare.messaging.domain.CommunicationThread;
import com.homehealthcare.messaging.domain.MessageReadReceipt;
import com.homehealthcare.messaging.domain.StaffGroup;
import com.homehealthcare.messaging.domain.StaffGroupMember;
import com.homehealthcare.messaging.domain.ThreadParticipant;
import com.homehealthcare.messaging.foundation.BranchBroadcastStatus;
import com.homehealthcare.messaging.foundation.CommunicationMessageType;
import com.homehealthcare.messaging.foundation.CommunicationThreadStatus;
import com.homehealthcare.messaging.foundation.CoordinationContextType;
import com.homehealthcare.messaging.foundation.MessagingDeliveryState;
import com.homehealthcare.messaging.foundation.MessagingEscalationStatus;
import com.homehealthcare.messaging.foundation.MessagingThreadType;
import com.homehealthcare.messaging.foundation.UnreadDeliveryProjection;
import com.homehealthcare.security.branch.AgencyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging")
class MessagingController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final MessagingCoordinationService messagingCoordinationService;

    MessagingController(
            ConfigurationActorResolver configurationActorResolver,
            MessagingCoordinationService messagingCoordinationService) {
        this.configurationActorResolver = configurationActorResolver;
        this.messagingCoordinationService = messagingCoordinationService;
    }

    @GetMapping("/threads")
    List<ThreadSummaryResponse> listThreads() {
        return messagingCoordinationService.listVisibleThreads(configurationActorResolver.requireActorMembership()).stream()
                .map(MessagingController::toThreadSummaryResponse)
                .toList();
    }

    @PostMapping("/threads")
    ThreadSummaryResponse createThread(@Valid @RequestBody CreateThreadRequest request) {
        return toThreadSummaryResponse(messagingCoordinationService.createThread(
                configurationActorResolver.requireActorMembership(),
                new ManageThreadCommand(
                        request.threadType(),
                        request.subject(),
                        request.branchId(),
                        request.patientId(),
                        request.visitOccurrenceId(),
                        request.taskTemplateId(),
                        request.participantMembershipIds(),
                        request.staffGroupIds())));
    }

    @GetMapping("/threads/{threadId}")
    ThreadDetailResponse getThread(@PathVariable UUID threadId) {
        return toThreadDetailResponse(messagingCoordinationService.getThreadDetail(
                configurationActorResolver.requireActorMembership(),
                threadId));
    }

    @PostMapping("/threads/{threadId}/messages")
    MessageResponse sendMessage(@PathVariable UUID threadId, @Valid @RequestBody SendMessageRequest request) {
        return toMessageResponse(messagingCoordinationService.sendMessage(
                configurationActorResolver.requireActorMembership(),
                threadId,
                new SendMessageCommand(
                        request.messageBody(),
                        request.sentAt(),
                        request.messageType(),
                        request.attachmentReference())));
    }

    @PostMapping("/messages/{messageId}/read")
    ReadReceiptResponse markMessageRead(@PathVariable UUID messageId, @Valid @RequestBody(required = false) MarkReadRequest request) {
        return toReadReceiptResponse(messagingCoordinationService.markMessageRead(
                configurationActorResolver.requireActorMembership(),
                messageId,
                request == null ? null : request.readAt()));
    }

    @PostMapping("/threads/{threadId}/read")
    List<ReadReceiptResponse> markThreadRead(@PathVariable UUID threadId, @Valid @RequestBody(required = false) MarkReadRequest request) {
        return messagingCoordinationService.markThreadRead(
                        configurationActorResolver.requireActorMembership(),
                        threadId,
                        request == null ? null : request.readAt())
                .stream()
                .map(MessagingController::toReadReceiptResponse)
                .toList();
    }

    @GetMapping("/threads/by-patient/{patientId}")
    List<ThreadSummaryResponse> listThreadsByPatient(@PathVariable UUID patientId) {
        return messagingCoordinationService.listThreadsForPatient(configurationActorResolver.requireActorMembership(), patientId).stream()
                .map(MessagingController::toThreadSummaryResponse)
                .toList();
    }

    @GetMapping("/threads/by-visit/{visitOccurrenceId}")
    List<ThreadSummaryResponse> listThreadsByVisit(@PathVariable UUID visitOccurrenceId) {
        return messagingCoordinationService.listThreadsForVisit(configurationActorResolver.requireActorMembership(), visitOccurrenceId).stream()
                .map(MessagingController::toThreadSummaryResponse)
                .toList();
    }

    @GetMapping("/threads/by-task/{taskTemplateId}")
    List<ThreadSummaryResponse> listThreadsByTask(@PathVariable UUID taskTemplateId) {
        return messagingCoordinationService.listThreadsForTask(configurationActorResolver.requireActorMembership(), taskTemplateId).stream()
                .map(MessagingController::toThreadSummaryResponse)
                .toList();
    }

    @GetMapping("/groups")
    List<StaffGroupResponse> listStaffGroups() {
        return messagingCoordinationService.listStaffGroups(configurationActorResolver.requireActorMembership()).stream()
                .map(MessagingController::toStaffGroupResponse)
                .toList();
    }

    @PostMapping("/groups")
    StaffGroupResponse createStaffGroup(@Valid @RequestBody ManageStaffGroupRequest request) {
        StaffGroup group = messagingCoordinationService.createStaffGroup(
                configurationActorResolver.requireActorMembership(),
                new ManageStaffGroupCommand(request.name(), request.description(), request.branchId()));
        return toStaffGroupResponse(new StaffGroupAggregate(group, List.of()));
    }

    @PutMapping("/groups/{staffGroupId}")
    StaffGroupResponse updateStaffGroup(@PathVariable UUID staffGroupId, @Valid @RequestBody ManageStaffGroupRequest request) {
        StaffGroup group = messagingCoordinationService.updateStaffGroup(
                configurationActorResolver.requireActorMembership(),
                staffGroupId,
                new ManageStaffGroupCommand(request.name(), request.description(), request.branchId()));
        List<StaffGroupAggregate> groups = messagingCoordinationService.listStaffGroups(configurationActorResolver.requireActorMembership());
        return groups.stream()
                .filter(item -> item.group().getId().equals(group.getId()))
                .findFirst()
                .map(MessagingController::toStaffGroupResponse)
                .orElseGet(() -> toStaffGroupResponse(new StaffGroupAggregate(group, List.of())));
    }

    @DeleteMapping("/groups/{staffGroupId}")
    StaffGroupResponse deactivateStaffGroup(@PathVariable UUID staffGroupId) {
        StaffGroup group = messagingCoordinationService.deactivateStaffGroup(configurationActorResolver.requireActorMembership(), staffGroupId);
        return toStaffGroupResponse(new StaffGroupAggregate(group, List.of()));
    }

    @PostMapping("/groups/{staffGroupId}/members")
    StaffGroupMemberResponse addStaffGroupMember(@PathVariable UUID staffGroupId, @Valid @RequestBody ManageGroupMemberRequest request) {
        return toStaffGroupMemberResponse(messagingCoordinationService.addStaffGroupMember(
                configurationActorResolver.requireActorMembership(),
                staffGroupId,
                request.membershipId()));
    }

    @DeleteMapping("/groups/{staffGroupId}/members/{membershipId}")
    StaffGroupMemberResponse removeStaffGroupMember(@PathVariable UUID staffGroupId, @PathVariable UUID membershipId) {
        return toStaffGroupMemberResponse(messagingCoordinationService.removeStaffGroupMember(
                configurationActorResolver.requireActorMembership(),
                staffGroupId,
                membershipId));
    }

    @GetMapping("/broadcasts")
    List<BranchBroadcastResponse> listBranchBroadcasts() {
        return messagingCoordinationService.listBranchBroadcasts(configurationActorResolver.requireActorMembership()).stream()
                .map(MessagingController::toBranchBroadcastResponse)
                .toList();
    }

    @PostMapping("/broadcasts")
    BranchBroadcastResponse createBranchBroadcast(@Valid @RequestBody CreateBranchBroadcastRequest request) {
        return toBranchBroadcastResponse(messagingCoordinationService.createBranchBroadcast(
                configurationActorResolver.requireActorMembership(),
                new ManageBranchBroadcastCommand(
                        request.branchId(),
                        request.eligibleRoles(),
                        request.subject(),
                        request.body(),
                        request.expiresAt())));
    }

    @DeleteMapping("/broadcasts/{broadcastId}")
    BranchBroadcastResponse cancelBranchBroadcast(@PathVariable UUID broadcastId) {
        return toBranchBroadcastResponse(messagingCoordinationService.cancelBranchBroadcast(
                configurationActorResolver.requireActorMembership(),
                broadcastId));
    }

    @PostMapping("/threads/{threadId}/escalation")
    EscalationResponse tagEscalation(@PathVariable UUID threadId, @Valid @RequestBody ManageEscalationRequest request) {
        return toEscalationResponse(messagingCoordinationService.tagEscalation(
                configurationActorResolver.requireActorMembership(),
                threadId,
                new ManageEscalationCommand(request.status(), request.tag(), request.reason())));
    }

    @DeleteMapping("/threads/{threadId}/escalation")
    EscalationResponse resolveEscalation(@PathVariable UUID threadId) {
        return toEscalationResponse(messagingCoordinationService.resolveEscalation(
                configurationActorResolver.requireActorMembership(),
                threadId));
    }

    @GetMapping("/summary")
    CoordinationSummaryResponse summary() {
        return toCoordinationSummaryResponse(messagingCoordinationService.coordinationSummary(
                configurationActorResolver.requireActorMembership()));
    }

    @GetMapping("/messages/{messageId}/read-receipts")
    List<ReadReceiptResponse> readReceipts(@PathVariable UUID messageId) {
        return messagingCoordinationService.readReceipts(configurationActorResolver.requireActorMembership(), messageId).stream()
                .map(MessagingController::toReadReceiptResponse)
                .toList();
    }

    private static ThreadSummaryResponse toThreadSummaryResponse(CommunicationThread thread) {
        return new ThreadSummaryResponse(
                thread.getId(),
                thread.getThreadType(),
                thread.getSubject(),
                thread.getStatus(),
                thread.getBranchId(),
                thread.getPatientId(),
                thread.getVisitOccurrenceId(),
                thread.getEscalationStatus(),
                thread.getLastMessageAt(),
                thread.getCreatedAt(),
                thread.getCreatedByMembership().getId());
    }

    private static ThreadDetailResponse toThreadDetailResponse(ThreadDetail detail) {
        return new ThreadDetailResponse(
                toThreadSummaryResponse(detail.thread()),
                detail.participants().stream().map(MessagingController::toParticipantResponse).toList(),
                detail.messages().stream().map(MessagingController::toMessageResponse).toList(),
                detail.contextLinks().stream().map(MessagingController::toContextLinkResponse).toList());
    }

    private static ParticipantResponse toParticipantResponse(ThreadParticipant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getMembership().getId(),
                participant.getMembership().getRole(),
                participant.getParticipantRole(),
                participant.getAddedAt(),
                participant.getRemovedAt(),
                participant.isMuted());
    }

    private static MessageResponse toMessageResponse(CommunicationMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getThreadId(),
                message.getSenderMembership().getId(),
                message.getMessageBody(),
                message.getCreatedAtAtSource(),
                message.getEditedAt(),
                message.getMessageType(),
                message.getAttachmentReference());
    }

    private static ContextLinkResponse toContextLinkResponse(CommunicationContextLink link) {
        return new ContextLinkResponse(link.getId(), link.getContextType(), link.getContextId());
    }

    private static StaffGroupResponse toStaffGroupResponse(StaffGroupAggregate aggregate) {
        return new StaffGroupResponse(
                aggregate.group().getId(),
                aggregate.group().getName(),
                aggregate.group().getDescription(),
                aggregate.group().getBranchId(),
                aggregate.group().isActive(),
                aggregate.members().stream().map(MessagingController::toStaffGroupMemberResponse).toList());
    }

    private static StaffGroupMemberResponse toStaffGroupMemberResponse(StaffGroupMember member) {
        return new StaffGroupMemberResponse(
                member.getId(),
                member.getStaffGroup().getId(),
                member.getMembership().getId(),
                member.getMembership().getRole(),
                member.getAddedAt(),
                member.getRemovedAt());
    }

    private static BranchBroadcastResponse toBranchBroadcastResponse(BranchBroadcast broadcast) {
        return new BranchBroadcastResponse(
                broadcast.getId(),
                broadcast.getBranchId(),
                broadcast.getEligibleRolesCsv(),
                broadcast.getSubject(),
                broadcast.getBody(),
                broadcast.getThreadId(),
                broadcast.getExpiresAt(),
                broadcast.getStatus(),
                broadcast.getCreatedAt());
    }

    private static EscalationResponse toEscalationResponse(com.homehealthcare.messaging.domain.EscalationMarker marker) {
        return new EscalationResponse(
                marker.getId(),
                marker.getThread().getId(),
                marker.getStatus(),
                marker.getTag(),
                marker.getReason(),
                marker.getTaggedByMembership().getId(),
                marker.getTaggedAt(),
                marker.getClearedByMembership() == null ? null : marker.getClearedByMembership().getId(),
                marker.getClearedAt());
    }

    private static CoordinationSummaryResponse toCoordinationSummaryResponse(CoordinationSummary summary) {
        return new CoordinationSummaryResponse(
                summary.unread(),
                summary.recentBroadcasts().stream().map(MessagingController::toBranchBroadcastResponse).toList(),
                summary.recentContextThreads().stream().map(MessagingController::toThreadSummaryResponse).toList());
    }

    record CreateThreadRequest(
            @NotNull MessagingThreadType threadType,
            String subject,
            UUID branchId,
            UUID patientId,
            UUID visitOccurrenceId,
            UUID taskTemplateId,
            Set<UUID> participantMembershipIds,
            Set<UUID> staffGroupIds) {
    }

    record SendMessageRequest(
            @NotBlank String messageBody,
            OffsetDateTime sentAt,
            CommunicationMessageType messageType,
            String attachmentReference) {
    }

    record MarkReadRequest(
            OffsetDateTime readAt) {
    }

    record ManageStaffGroupRequest(
            @NotBlank String name,
            String description,
            UUID branchId) {
    }

    record ManageGroupMemberRequest(
            @NotNull UUID membershipId) {
    }

    record CreateBranchBroadcastRequest(
            @NotNull UUID branchId,
            Set<AgencyRole> eligibleRoles,
            @NotBlank String subject,
            @NotBlank String body,
            OffsetDateTime expiresAt) {
    }

    record ManageEscalationRequest(
            @NotNull MessagingEscalationStatus status,
            @NotBlank String tag,
            String reason) {
    }

    record ThreadSummaryResponse(
            UUID id,
            MessagingThreadType threadType,
            String subject,
            CommunicationThreadStatus status,
            UUID branchId,
            UUID patientId,
            UUID visitOccurrenceId,
            MessagingEscalationStatus escalationStatus,
            OffsetDateTime lastMessageAt,
            Instant createdAt,
            UUID createdByMembershipId) {
    }

    record ThreadDetailResponse(
            ThreadSummaryResponse thread,
            List<ParticipantResponse> participants,
            List<MessageResponse> messages,
            List<ContextLinkResponse> contextLinks) {
    }

    record ParticipantResponse(
            UUID id,
            UUID membershipId,
            AgencyRole role,
            String participantRole,
            OffsetDateTime addedAt,
            OffsetDateTime removedAt,
            boolean muted) {
    }

    record MessageResponse(
            UUID id,
            UUID threadId,
            UUID senderMembershipId,
            String messageBody,
            OffsetDateTime createdAt,
            OffsetDateTime editedAt,
            CommunicationMessageType messageType,
            String attachmentReference) {
    }

    record ContextLinkResponse(
            UUID id,
            CoordinationContextType contextType,
            UUID contextId) {
    }

    record ReadReceiptResponse(
            UUID id,
            UUID messageId,
            UUID recipientMembershipId,
            OffsetDateTime readAt,
            MessagingDeliveryState deliveryState) {
    }

    record StaffGroupResponse(
            UUID id,
            String name,
            String description,
            UUID branchId,
            boolean active,
            List<StaffGroupMemberResponse> members) {
    }

    record StaffGroupMemberResponse(
            UUID id,
            UUID staffGroupId,
            UUID membershipId,
            AgencyRole role,
            OffsetDateTime addedAt,
            OffsetDateTime removedAt) {
    }

    record BranchBroadcastResponse(
            UUID id,
            UUID branchId,
            String eligibleRolesCsv,
            String subject,
            String body,
            UUID threadId,
            OffsetDateTime expiresAt,
            BranchBroadcastStatus status,
            Instant createdAt) {
    }

    record EscalationResponse(
            UUID id,
            UUID threadId,
            MessagingEscalationStatus status,
            String tag,
            String reason,
            UUID taggedByMembershipId,
            OffsetDateTime taggedAt,
            UUID clearedByMembershipId,
            OffsetDateTime clearedAt) {
    }

    record CoordinationSummaryResponse(
            UnreadDeliveryProjection unread,
            List<BranchBroadcastResponse> recentBroadcasts,
            List<ThreadSummaryResponse> recentContextThreads) {
    }

    private static ReadReceiptResponse toReadReceiptResponse(MessageReadReceipt receipt) {
        return new ReadReceiptResponse(
                receipt.getId(),
                receipt.getMessageId(),
                receipt.getRecipientMembership().getId(),
                receipt.getReadAt(),
                receipt.getDeliveryState());
    }
}
