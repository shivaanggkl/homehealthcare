package com.homehealthcare.messaging.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import com.homehealthcare.messaging.domain.BranchBroadcast;
import com.homehealthcare.messaging.domain.BranchBroadcastRepository;
import com.homehealthcare.messaging.domain.CommunicationContextLink;
import com.homehealthcare.messaging.domain.CommunicationContextLinkRepository;
import com.homehealthcare.messaging.domain.CommunicationMessage;
import com.homehealthcare.messaging.domain.CommunicationMessageRepository;
import com.homehealthcare.messaging.domain.CommunicationThread;
import com.homehealthcare.messaging.domain.CommunicationThreadRepository;
import com.homehealthcare.messaging.domain.EscalationMarker;
import com.homehealthcare.messaging.domain.EscalationMarkerRepository;
import com.homehealthcare.messaging.domain.MessageReadReceipt;
import com.homehealthcare.messaging.domain.MessageReadReceiptRepository;
import com.homehealthcare.messaging.domain.StaffGroup;
import com.homehealthcare.messaging.domain.StaffGroupMember;
import com.homehealthcare.messaging.domain.StaffGroupMemberRepository;
import com.homehealthcare.messaging.domain.StaffGroupRepository;
import com.homehealthcare.messaging.domain.ThreadParticipant;
import com.homehealthcare.messaging.domain.ThreadParticipantRepository;
import com.homehealthcare.messaging.foundation.BranchBroadcastStatus;
import com.homehealthcare.messaging.foundation.CommunicationMessageType;
import com.homehealthcare.messaging.foundation.CoordinationContextType;
import com.homehealthcare.messaging.foundation.MessagingAuditService;
import com.homehealthcare.messaging.foundation.MessagingDeliveryState;
import com.homehealthcare.messaging.foundation.MessagingEscalationStatus;
import com.homehealthcare.messaging.foundation.MessagingThreadType;
import com.homehealthcare.messaging.foundation.UnreadDeliveryProjection;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
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
public class MessagingCoordinationService {

    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchRepository branchRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final PatientRepository patientRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final CommunicationThreadRepository communicationThreadRepository;
    private final ThreadParticipantRepository threadParticipantRepository;
    private final CommunicationMessageRepository communicationMessageRepository;
    private final MessageReadReceiptRepository messageReadReceiptRepository;
    private final CommunicationContextLinkRepository communicationContextLinkRepository;
    private final StaffGroupRepository staffGroupRepository;
    private final StaffGroupMemberRepository staffGroupMemberRepository;
    private final BranchBroadcastRepository branchBroadcastRepository;
    private final EscalationMarkerRepository escalationMarkerRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final MessagingAuditService messagingAuditService;

    @Transactional
    public CommunicationThread createThread(
            @NotNull AgencyMembership actorMembership,
            @Valid @NotNull ManageThreadCommand command) {
        requirePermission(actorMembership, AgencyPermission.SEND_SECURE_MESSAGES);
        Branch branch = command.branchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertBranchAccess(actorMembership, branch);

        Patient patient = command.patientId() == null ? null : resolvePatient(actorMembership.getAgencyId(), command.patientId());
        VisitOccurrence visit = command.visitOccurrenceId() == null ? null : resolveVisit(actorMembership.getAgencyId(), command.visitOccurrenceId());
        TaskTemplate taskTemplate = command.taskTemplateId() == null ? null : resolveTaskTemplate(actorMembership.getAgencyId(), command.taskTemplateId());
        validateContextConsistency(branch, patient, visit);

        CommunicationThread thread = communicationThreadRepository.saveAndFlush(CommunicationThread.create(
                command.threadType(),
                command.subject(),
                actorMembership,
                branch == null && visit != null ? resolveBranchFromVisit(visit) : branch,
                patient == null && visit != null ? visit.getPatient() : patient,
                visit));

        Set<UUID> participantMembershipIds = new LinkedHashSet<>();
        participantMembershipIds.add(actorMembership.getId());
        participantMembershipIds.addAll(command.participantMembershipIds());
        participantMembershipIds.addAll(resolveGroupMembershipIds(actorMembership.getAgencyId(), command.staffGroupIds(), branch));

        for (UUID participantMembershipId : participantMembershipIds) {
            AgencyMembership participantMembership = resolveMembership(actorMembership.getAgencyId(), participantMembershipId);
            assertRecipientAccess(participantMembership, thread.getBranchId());
            addParticipantInternal(actorMembership, thread, participantMembership, null, OffsetDateTime.now());
        }

        createContextLinks(thread, patient, visit, taskTemplate, command.staffGroupIds(), branch);
        messagingAuditService.recordThreadCreated(actorMembership, thread.getId(), thread.getBranchId(), metadata(
                "threadType", thread.getThreadType().name(),
                "participantCount", String.valueOf(participantMembershipIds.size())));
        return thread;
    }

    @Transactional
    public ThreadParticipant addParticipant(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID threadId,
            @NotNull UUID membershipId,
            String participantRole) {
        requirePermission(actorMembership, AgencyPermission.SEND_SECURE_MESSAGES);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadWriteAccess(actorMembership, thread);
        AgencyMembership participantMembership = resolveMembership(actorMembership.getAgencyId(), membershipId);
        assertRecipientAccess(participantMembership, thread.getBranchId());
        return addParticipantInternal(actorMembership, thread, participantMembership, participantRole, OffsetDateTime.now());
    }

    @Transactional
    public CommunicationMessage sendMessage(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID threadId,
            @Valid @NotNull SendMessageCommand command) {
        requirePermission(actorMembership, AgencyPermission.SEND_SECURE_MESSAGES);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadWriteAccess(actorMembership, thread);

        CommunicationMessage message = communicationMessageRepository.saveAndFlush(CommunicationMessage.create(
                thread,
                actorMembership,
                command.messageBody(),
                command.sentAt() == null ? OffsetDateTime.now() : command.sentAt(),
                command.messageType() == null ? CommunicationMessageType.USER_MESSAGE : command.messageType(),
                command.attachmentReference()));
        thread.touchLastMessageAt(message.getCreatedAtAtSource());
        communicationThreadRepository.saveAndFlush(thread);

        createPendingReceipts(thread, actorMembership.getId(), message);
        messagingAuditService.recordMessageSent(actorMembership, message.getId(), thread.getBranchId(), metadata(
                "threadId", thread.getId().toString(),
                "messageType", message.getMessageType().name()));
        return message;
    }

    @Transactional
    public MessageReadReceipt markMessageRead(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID messageId,
            OffsetDateTime readAt) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        CommunicationMessage message = communicationMessageRepository.findByIdAndAgency_Id(messageId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MessagingEntityNotFoundException("CommunicationMessage", messageId));
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), message.getThreadId());
        assertThreadReadAccess(actorMembership, thread);

        MessageReadReceipt receipt = messageReadReceiptRepository.findByMessage_IdAndRecipientMembership_Id(messageId, actorMembership.getId())
                .orElseGet(() -> MessageReadReceipt.createPending(message, actorMembership));
        receipt.markRead(readAt == null ? OffsetDateTime.now() : readAt);
        MessageReadReceipt saved = messageReadReceiptRepository.saveAndFlush(receipt);
        messagingAuditService.recordMessageRead(actorMembership, message.getId(), thread.getBranchId(), metadata(
                "threadId", thread.getId().toString(),
                "recipientMembershipId", actorMembership.getId().toString()));
        return saved;
    }

    @Transactional
    public StaffGroup createStaffGroup(
            @NotNull AgencyMembership actorMembership,
            @Valid @NotNull ManageStaffGroupCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_STAFF_GROUPS);
        Branch branch = command.branchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertBranchAccess(actorMembership, branch);
        StaffGroup group = staffGroupRepository.saveAndFlush(StaffGroup.create(
                actorMembership.getAgency(),
                command.name(),
                command.description(),
                branch));
        messagingAuditService.recordStaffGroupUpdated(actorMembership, group.getId(), group.getBranchId(), metadata("action", "created"));
        return group;
    }

    @Transactional
    public StaffGroupMember addStaffGroupMember(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID staffGroupId,
            @NotNull UUID membershipId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_STAFF_GROUPS);
        StaffGroup staffGroup = resolveStaffGroup(actorMembership.getAgencyId(), staffGroupId);
        assertBranchAccess(actorMembership, staffGroup.getBranchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), staffGroup.getBranchId()));
        AgencyMembership membership = resolveMembership(actorMembership.getAgencyId(), membershipId);
        assertRecipientAccess(membership, staffGroup.getBranchId());
        if (staffGroupMemberRepository.existsByStaffGroup_IdAndMembership_IdAndRemovedAtIsNull(staffGroupId, membershipId)) {
            throw new MessagingConflictException("The membership is already active in this staff group.");
        }
        StaffGroupMember member = staffGroupMemberRepository.saveAndFlush(StaffGroupMember.add(staffGroup, membership, OffsetDateTime.now()));
        messagingAuditService.recordStaffGroupUpdated(actorMembership, staffGroup.getId(), staffGroup.getBranchId(), metadata(
                "action", "member_added",
                "membershipId", membershipId.toString()));
        return member;
    }

    @Transactional
    public BranchBroadcast createBranchBroadcast(
            @NotNull AgencyMembership actorMembership,
            @Valid @NotNull ManageBranchBroadcastCommand command) {
        requirePermission(actorMembership, AgencyPermission.SEND_BRANCH_BROADCASTS);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertBranchAccess(actorMembership, branch);

        Set<UUID> recipients = resolveBroadcastRecipients(branch.getId(), command.eligibleRoles());
        if (recipients.isEmpty()) {
            throw new MessagingConflictException("Branch broadcast requires at least one eligible recipient.");
        }

        CommunicationThread thread = communicationThreadRepository.saveAndFlush(CommunicationThread.create(
                MessagingThreadType.BRANCH_BROADCAST,
                command.subject(),
                actorMembership,
                branch,
                null,
                null));
        for (UUID recipientMembershipId : recipients) {
            addParticipantInternal(actorMembership, thread, resolveMembership(actorMembership.getAgencyId(), recipientMembershipId), "RECIPIENT", OffsetDateTime.now());
        }
        if (!recipients.contains(actorMembership.getId())) {
            addParticipantInternal(actorMembership, thread, actorMembership, "SENDER", OffsetDateTime.now());
        }

        CommunicationMessage broadcastMessage = communicationMessageRepository.saveAndFlush(CommunicationMessage.create(
                thread,
                actorMembership,
                command.body(),
                OffsetDateTime.now(),
                CommunicationMessageType.BRANCH_ANNOUNCEMENT,
                null));
        thread.touchLastMessageAt(broadcastMessage.getCreatedAtAtSource());
        communicationThreadRepository.saveAndFlush(thread);
        createPendingReceipts(thread, actorMembership.getId(), broadcastMessage);

        BranchBroadcast broadcast = branchBroadcastRepository.saveAndFlush(BranchBroadcast.create(
                branch,
                toEligibleRolesCsv(command.eligibleRoles()),
                command.subject(),
                command.body(),
                actorMembership,
                thread,
                command.expiresAt()));
        createContextLinks(thread, null, null, null, Set.of(), branch);
        messagingAuditService.recordBranchBroadcastSent(actorMembership, broadcast.getId(), branch.getId(), metadata(
                "recipientCount", String.valueOf(recipients.size())));
        return broadcast;
    }

    @Transactional
    public BranchBroadcast cancelBranchBroadcast(@NotNull AgencyMembership actorMembership, @NotNull UUID broadcastId) {
        requirePermission(actorMembership, AgencyPermission.SEND_BRANCH_BROADCASTS);
        BranchBroadcast broadcast = branchBroadcastRepository.findByIdAndAgency_Id(broadcastId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MessagingEntityNotFoundException("BranchBroadcast", broadcastId));
        assertBranchAccess(actorMembership, resolveBranch(actorMembership.getAgencyId(), broadcast.getBranchId()));
        broadcast.cancel();
        BranchBroadcast saved = branchBroadcastRepository.saveAndFlush(broadcast);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), broadcast.getThreadId());
        thread.archive();
        communicationThreadRepository.saveAndFlush(thread);
        return saved;
    }

    @Transactional
    public EscalationMarker tagEscalation(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID threadId,
            @Valid @NotNull ManageEscalationCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_MESSAGE_ESCALATIONS);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadReadAccess(actorMembership, thread);

        EscalationMarker marker = escalationMarkerRepository.saveAndFlush(EscalationMarker.create(
                thread,
                command.status(),
                command.tag(),
                command.reason(),
                actorMembership,
                OffsetDateTime.now()));
        thread.updateEscalationStatus(command.status());
        communicationThreadRepository.saveAndFlush(thread);
        messagingAuditService.recordEscalationTagged(actorMembership, marker.getId(), thread.getBranchId(), metadata(
                "threadId", thread.getId().toString(),
                "status", command.status().name()));
        return marker;
    }

    @Transactional
    public EscalationMarker resolveEscalation(@NotNull AgencyMembership actorMembership, @NotNull UUID threadId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_MESSAGE_ESCALATIONS);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadReadAccess(actorMembership, thread);
        EscalationMarker marker = escalationMarkerRepository.findTopByThread_IdOrderByTaggedAtDesc(threadId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("EscalationMarker", threadId));
        marker.resolve(actorMembership, OffsetDateTime.now());
        EscalationMarker saved = escalationMarkerRepository.saveAndFlush(marker);
        thread.updateEscalationStatus(MessagingEscalationStatus.RESOLVED);
        communicationThreadRepository.saveAndFlush(thread);
        messagingAuditService.recordEscalationResolved(actorMembership, marker.getId(), thread.getBranchId(), metadata("threadId", thread.getId().toString()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CommunicationThread> listThreadsForPatient(@NotNull AgencyMembership actorMembership, @NotNull UUID patientId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        resolvePatient(actorMembership.getAgencyId(), patientId);
        List<CommunicationThread> threads = communicationThreadRepository.findAllByAgency_IdAndPatient_IdOrderByLastMessageAtDescCreatedAtDesc(
                actorMembership.getAgencyId(),
                patientId);
        return threads.stream()
                .filter(thread -> canReadThread(actorMembership, thread))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunicationThread> listVisibleThreads(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        return communicationThreadRepository.findAllByAgency_IdOrderByLastMessageAtDescCreatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(thread -> canReadThread(actorMembership, thread))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunicationThread> listThreadsForVisit(@NotNull AgencyMembership actorMembership, @NotNull UUID visitOccurrenceId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        List<CommunicationThread> threads = communicationThreadRepository.findAllByAgency_IdAndVisitOccurrence_IdOrderByLastMessageAtDescCreatedAtDesc(
                actorMembership.getAgencyId(),
                visitOccurrenceId);
        return threads.stream()
                .filter(thread -> canReadThread(actorMembership, thread))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunicationThread> listThreadsForTask(@NotNull AgencyMembership actorMembership, @NotNull UUID taskTemplateId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        resolveTaskTemplate(actorMembership.getAgencyId(), taskTemplateId);
        return communicationContextLinkRepository.findAllByContextTypeAndContextId(CoordinationContextType.TASK, taskTemplateId).stream()
                .map(CommunicationContextLink::getThread)
                .filter(thread -> canReadThread(actorMembership, thread))
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public ThreadDetail getThreadDetail(@NotNull AgencyMembership actorMembership, @NotNull UUID threadId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadReadAccess(actorMembership, thread);
        List<ThreadParticipant> participants = threadParticipantRepository.findAllByThread_IdAndRemovedAtIsNullOrderByAddedAtAsc(threadId);
        List<CommunicationMessage> messages = communicationMessageRepository.findAllByThread_IdOrderByCreatedAtAtSourceAsc(threadId);
        List<CommunicationContextLink> contextLinks = communicationContextLinkRepository.findAllByThread_IdOrderByContextTypeAsc(threadId);
        return new ThreadDetail(thread, participants, messages, contextLinks);
    }

    @Transactional
    public List<MessageReadReceipt> markThreadRead(@NotNull AgencyMembership actorMembership, @NotNull UUID threadId, OffsetDateTime readAt) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), threadId);
        assertThreadReadAccess(actorMembership, thread);
        OffsetDateTime effectiveReadAt = readAt == null ? OffsetDateTime.now() : readAt;
        return communicationMessageRepository.findAllByThread_IdOrderByCreatedAtAtSourceAsc(threadId).stream()
                .map(message -> markMessageRead(actorMembership, message.getId(), effectiveReadAt))
                .toList();
    }

    @Transactional
    public StaffGroup updateStaffGroup(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID staffGroupId,
            @Valid @NotNull ManageStaffGroupCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_STAFF_GROUPS);
        StaffGroup staffGroup = resolveStaffGroup(actorMembership.getAgencyId(), staffGroupId);
        Branch branch = command.branchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertBranchAccess(actorMembership, branch);
        staffGroup.update(command.name(), command.description(), branch);
        StaffGroup saved = staffGroupRepository.saveAndFlush(staffGroup);
        messagingAuditService.recordStaffGroupUpdated(actorMembership, saved.getId(), saved.getBranchId(), metadata("action", "updated"));
        return saved;
    }

    @Transactional
    public StaffGroup deactivateStaffGroup(@NotNull AgencyMembership actorMembership, @NotNull UUID staffGroupId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_STAFF_GROUPS);
        StaffGroup staffGroup = resolveStaffGroup(actorMembership.getAgencyId(), staffGroupId);
        assertBranchAccess(actorMembership, staffGroup.getBranchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), staffGroup.getBranchId()));
        staffGroup.deactivate();
        StaffGroup saved = staffGroupRepository.saveAndFlush(staffGroup);
        messagingAuditService.recordStaffGroupUpdated(actorMembership, saved.getId(), saved.getBranchId(), metadata("action", "deactivated"));
        return saved;
    }

    @Transactional
    public StaffGroupMember removeStaffGroupMember(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID staffGroupId,
            @NotNull UUID membershipId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_STAFF_GROUPS);
        StaffGroup staffGroup = resolveStaffGroup(actorMembership.getAgencyId(), staffGroupId);
        assertBranchAccess(actorMembership, staffGroup.getBranchId() == null ? null : resolveBranch(actorMembership.getAgencyId(), staffGroup.getBranchId()));
        StaffGroupMember member = staffGroupMemberRepository.findAllByStaffGroup_IdAndRemovedAtIsNullOrderByAddedAtAsc(staffGroupId).stream()
                .filter(item -> Objects.equals(item.getMembershipId(), membershipId))
                .findFirst()
                .orElseThrow(() -> new MessagingEntityNotFoundException("StaffGroupMember", membershipId));
        member.remove(OffsetDateTime.now());
        StaffGroupMember saved = staffGroupMemberRepository.saveAndFlush(member);
        messagingAuditService.recordStaffGroupUpdated(actorMembership, staffGroup.getId(), staffGroup.getBranchId(), metadata(
                "action", "member_removed",
                "membershipId", membershipId.toString()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StaffGroupAggregate> listStaffGroups(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        return staffGroupRepository.findAllByAgency_IdOrderByNameAsc(actorMembership.getAgencyId()).stream()
                .filter(group -> hasBranchAccess(actorMembership, group.getBranchId()))
                .map(group -> new StaffGroupAggregate(
                        group,
                        staffGroupMemberRepository.findAllByStaffGroup_IdAndRemovedAtIsNullOrderByAddedAtAsc(group.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchBroadcast> listBranchBroadcasts(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        return branchBroadcastRepository.findAllByAgency_IdOrderByCreatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(broadcast -> hasBranchAccess(actorMembership, broadcast.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CoordinationSummary coordinationSummary(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        UnreadDeliveryProjection unread = unreadProjection(actorMembership);
        List<BranchBroadcast> recentBroadcasts = listBranchBroadcasts(actorMembership).stream().limit(5).toList();
        List<CommunicationThread> recentContextThreads = listVisibleThreads(actorMembership).stream()
                .filter(thread -> thread.getPatientId() != null || thread.getVisitOccurrenceId() != null)
                .limit(5)
                .toList();
        return new CoordinationSummary(unread, recentBroadcasts, recentContextThreads);
    }

    @Transactional(readOnly = true)
    public List<MessageReadReceipt> readReceipts(@NotNull AgencyMembership actorMembership, @NotNull UUID messageId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        CommunicationMessage message = communicationMessageRepository.findByIdAndAgency_Id(messageId, actorMembership.getAgencyId())
                .orElseThrow(() -> new MessagingEntityNotFoundException("CommunicationMessage", messageId));
        CommunicationThread thread = resolveThread(actorMembership.getAgencyId(), message.getThreadId());
        assertThreadReadAccess(actorMembership, thread);
        return messageReadReceiptRepository.findAllByMessage_IdOrderByRecipientMembership_IdAsc(messageId);
    }

    @Transactional(readOnly = true)
    public UnreadDeliveryProjection unreadProjection(@NotNull AgencyMembership actorMembership) {
        requirePermission(actorMembership, AgencyPermission.VIEW_MESSAGING_WORKSPACE);
        long unreadMessages = messageReadReceiptRepository.countByRecipientMembership_IdAndDeliveryStateNot(
                actorMembership.getId(),
                MessagingDeliveryState.READ);
        long unreadThreads = messageReadReceiptRepository.findAllByRecipientMembership_IdAndDeliveryStateNot(
                        actorMembership.getId(),
                        MessagingDeliveryState.READ)
                .stream()
                .map(receipt -> receipt.getMessage().getThreadId())
                .distinct()
                .count();
        long escalatedThreads = communicationThreadRepository.countByAgency_IdAndEscalationStatusNot(
                actorMembership.getAgencyId(),
                MessagingEscalationStatus.NORMAL);
        long activeBroadcasts = branchBroadcastRepository.countByAgency_IdAndStatus(actorMembership.getAgencyId(), BranchBroadcastStatus.SENT);
        return new UnreadDeliveryProjection(unreadThreads, unreadMessages, escalatedThreads, activeBroadcasts);
    }

    private ThreadParticipant addParticipantInternal(
            AgencyMembership actorMembership,
            CommunicationThread thread,
            AgencyMembership participantMembership,
            String participantRole,
            OffsetDateTime addedAt) {
        if (threadParticipantRepository.existsByThread_IdAndMembership_IdAndRemovedAtIsNull(thread.getId(), participantMembership.getId())) {
            throw new MessagingConflictException("The membership is already an active participant in this thread.");
        }
        ThreadParticipant participant = threadParticipantRepository.saveAndFlush(ThreadParticipant.add(
                thread,
                participantMembership,
                participantRole,
                addedAt));
        messagingAuditService.recordParticipantAdded(actorMembership, participant.getId(), thread.getBranchId(), metadata(
                "threadId", thread.getId().toString(),
                "membershipId", participantMembership.getId().toString()));
        return participant;
    }

    private void createPendingReceipts(CommunicationThread thread, UUID senderMembershipId, CommunicationMessage message) {
        threadParticipantRepository.findAllByThread_IdAndRemovedAtIsNullOrderByAddedAtAsc(thread.getId()).stream()
                .filter(participant -> !Objects.equals(participant.getMembershipId(), senderMembershipId))
                .forEach(participant -> {
                    MessageReadReceipt receipt = MessageReadReceipt.createPending(message, participant.getMembership());
                    receipt.markDelivered();
                    messageReadReceiptRepository.save(receipt);
                });
    }

    private void createContextLinks(
            CommunicationThread thread,
            Patient patient,
            VisitOccurrence visit,
            TaskTemplate taskTemplate,
            Collection<UUID> staffGroupIds,
            Branch branch) {
        if (patient != null) {
            communicationContextLinkRepository.save(CommunicationContextLink.create(thread, CoordinationContextType.PATIENT, patient.getId()));
        }
        if (visit != null) {
            communicationContextLinkRepository.save(CommunicationContextLink.create(thread, CoordinationContextType.VISIT, visit.getId()));
        }
        if (taskTemplate != null) {
            communicationContextLinkRepository.save(CommunicationContextLink.create(thread, CoordinationContextType.TASK, taskTemplate.getId()));
        }
        if (branch != null) {
            communicationContextLinkRepository.save(CommunicationContextLink.create(thread, CoordinationContextType.BRANCH, branch.getId()));
        }
        for (UUID staffGroupId : staffGroupIds) {
            communicationContextLinkRepository.save(CommunicationContextLink.create(thread, CoordinationContextType.STAFF_GROUP, staffGroupId));
        }
    }

    private Set<UUID> resolveGroupMembershipIds(UUID agencyId, Collection<UUID> staffGroupIds, Branch branch) {
        Set<UUID> membershipIds = new LinkedHashSet<>();
        for (UUID staffGroupId : staffGroupIds) {
            StaffGroup staffGroup = resolveStaffGroup(agencyId, staffGroupId);
            if (!staffGroup.isActive()) {
                continue;
            }
            if (branch != null && staffGroup.getBranchId() != null && !Objects.equals(branch.getId(), staffGroup.getBranchId())) {
                throw new MessagingConflictException("Staff group branch scope must match the thread branch context.");
            }
            staffGroupMemberRepository.findAllByStaffGroup_IdAndRemovedAtIsNullOrderByAddedAtAsc(staffGroupId).stream()
                    .map(StaffGroupMember::getMembershipId)
                    .forEach(membershipIds::add);
        }
        return membershipIds;
    }

    private Set<UUID> resolveBroadcastRecipients(UUID branchId, Set<AgencyRole> eligibleRoles) {
        return branchAssignmentRepository.findAllByBranch_IdAndStatusOrderByAgencyMembership_IdAsc(branchId, BranchAssignmentStatus.ACTIVE).stream()
                .map(assignment -> assignment.getAgencyMembership())
                .filter(AgencyMembership::isActive)
                .filter(membership -> eligibleRoles.isEmpty() || eligibleRoles.contains(membership.getRole()))
                .map(AgencyMembership::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private CommunicationThread resolveThread(UUID agencyId, UUID threadId) {
        return communicationThreadRepository.findByIdAndAgency_Id(threadId, agencyId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("CommunicationThread", threadId));
    }

    private StaffGroup resolveStaffGroup(UUID agencyId, UUID staffGroupId) {
        return staffGroupRepository.findByIdAndAgency_Id(staffGroupId, agencyId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("StaffGroup", staffGroupId));
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        return branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("Branch", branchId));
    }

    private Patient resolvePatient(UUID agencyId, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("Patient", patientId));
        if (!Objects.equals(patient.getAgencyId(), agencyId)) {
            throw new MessagingEntityNotFoundException("Patient", patientId);
        }
        return patient;
    }

    private VisitOccurrence resolveVisit(UUID agencyId, UUID visitOccurrenceId) {
        return visitOccurrenceRepository.findByIdAndAgency_Id(visitOccurrenceId, agencyId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private TaskTemplate resolveTaskTemplate(UUID agencyId, UUID taskTemplateId) {
        TaskTemplate taskTemplate = taskTemplateRepository.findById(taskTemplateId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("TaskTemplate", taskTemplateId));
        if (!Objects.equals(taskTemplate.getAgencyId(), agencyId)) {
            throw new MessagingEntityNotFoundException("TaskTemplate", taskTemplateId);
        }
        return taskTemplate;
    }

    private AgencyMembership resolveMembership(UUID agencyId, UUID membershipId) {
        AgencyMembership membership = agencyMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new MessagingEntityNotFoundException("AgencyMembership", membershipId));
        if (!Objects.equals(membership.getAgencyId(), agencyId)) {
            throw new MessagingEntityNotFoundException("AgencyMembership", membershipId);
        }
        if (membership.getStatus() != AgencyMembershipStatus.ACTIVE) {
            throw new MessagingConflictException("Messaging recipients must have active agency memberships.");
        }
        return membership;
    }

    private void validateContextConsistency(Branch branch, Patient patient, VisitOccurrence visit) {
        if (visit != null && patient != null && !Objects.equals(visit.getPatient().getId(), patient.getId())) {
            throw new MessagingConflictException("Visit context must belong to the same patient as the thread patient context.");
        }
        if (visit != null && branch != null && !Objects.equals(visit.getBranchId(), branch.getId())) {
            throw new MessagingConflictException("Visit context must belong to the same branch as the thread branch context.");
        }
    }

    private void assertThreadWriteAccess(AgencyMembership actorMembership, CommunicationThread thread) {
        if (!canReadThread(actorMembership, thread)) {
            throw new UnauthorizedMessagingActorException(actorMembership.getId());
        }
        if (thread.getStatus() != com.homehealthcare.messaging.foundation.CommunicationThreadStatus.ACTIVE) {
            throw new MessagingConflictException("Archived threads cannot be modified.");
        }
    }

    private void assertThreadReadAccess(AgencyMembership actorMembership, CommunicationThread thread) {
        if (!canReadThread(actorMembership, thread)) {
            throw new UnauthorizedMessagingActorException(actorMembership.getId());
        }
    }

    private boolean canReadThread(AgencyMembership actorMembership, CommunicationThread thread) {
        if (!Objects.equals(actorMembership.getAgencyId(), thread.getAgencyId())) {
            return false;
        }
        if (!hasBranchAccess(actorMembership, thread.getBranchId())) {
            return false;
        }
        return switch (actorMembership.getRole()) {
            case AGENCY_OWNER, BRANCH_ADMIN, SCHEDULER_COORDINATOR, QA_CLINICAL_REVIEWER -> true;
            default -> threadParticipantRepository.existsByThread_IdAndMembership_IdAndRemovedAtIsNull(thread.getId(), actorMembership.getId());
        };
    }

    private void assertRecipientAccess(AgencyMembership membership, UUID branchId) {
        if (!hasBranchAccess(membership, branchId)) {
            throw new MessagingConflictException("Recipient does not have access to the branch-scoped messaging context.");
        }
    }

    private void assertBranchAccess(AgencyMembership actorMembership, Branch branch) {
        if (branch != null && !hasBranchAccess(actorMembership, branch.getId())) {
            throw new UnauthorizedMessagingActorException(actorMembership.getId());
        }
    }

    private boolean hasBranchAccess(AgencyMembership membership, UUID branchId) {
        if (branchId == null) {
            return true;
        }
        if (membership.getRole().hasAgencyWideBranchAccess() || membership.getRole() == AgencyRole.BRANCH_ADMIN) {
            return true;
        }
        return branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                membership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE);
    }

    private Branch resolveBranchFromVisit(VisitOccurrence visit) {
        return visit.getBranchId() == null ? null : resolveBranch(visit.getAgencyId(), visit.getBranchId());
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedMessagingActorException::new);
    }

    private static String toEligibleRolesCsv(Collection<AgencyRole> roles) {
        return roles == null || roles.isEmpty()
                ? null
                : roles.stream().map(Enum::name).sorted().reduce((left, right) -> left + "," + right).orElse(null);
    }

    private static String metadata(String... values) {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < values.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(values[i]).append('"').append(':').append('"').append(values[i + 1]).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    public record ManageThreadCommand(
            @NotNull MessagingThreadType threadType,
            String subject,
            UUID branchId,
            UUID patientId,
            UUID visitOccurrenceId,
            UUID taskTemplateId,
            Set<UUID> participantMembershipIds,
            Set<UUID> staffGroupIds) {
        public ManageThreadCommand {
            participantMembershipIds = participantMembershipIds == null ? Set.of() : Set.copyOf(participantMembershipIds);
            staffGroupIds = staffGroupIds == null ? Set.of() : Set.copyOf(staffGroupIds);
        }
    }

    public record SendMessageCommand(
            @NotBlank String messageBody,
            OffsetDateTime sentAt,
            CommunicationMessageType messageType,
            String attachmentReference) {
    }

    public record ManageStaffGroupCommand(
            @NotBlank String name,
            String description,
            UUID branchId) {
    }

    public record ManageBranchBroadcastCommand(
            @NotNull UUID branchId,
            Set<AgencyRole> eligibleRoles,
            @NotBlank String subject,
            @NotBlank String body,
            OffsetDateTime expiresAt) {
        public ManageBranchBroadcastCommand {
            eligibleRoles = eligibleRoles == null ? Set.of() : Set.copyOf(eligibleRoles);
        }
    }

    public record ManageEscalationCommand(
            @NotNull MessagingEscalationStatus status,
            @NotBlank String tag,
            String reason) {
        public ManageEscalationCommand {
            if (status == MessagingEscalationStatus.NORMAL || status == MessagingEscalationStatus.RESOLVED) {
                throw new IllegalArgumentException("Escalation tagging must use URGENT or ESCALATED status");
            }
        }
    }

    public record ThreadDetail(
            CommunicationThread thread,
            List<ThreadParticipant> participants,
            List<CommunicationMessage> messages,
            List<CommunicationContextLink> contextLinks) {
    }

    public record StaffGroupAggregate(
            StaffGroup group,
            List<StaffGroupMember> members) {
    }

    public record CoordinationSummary(
            UnreadDeliveryProjection unread,
            List<BranchBroadcast> recentBroadcasts,
            List<CommunicationThread> recentContextThreads) {
    }
}
