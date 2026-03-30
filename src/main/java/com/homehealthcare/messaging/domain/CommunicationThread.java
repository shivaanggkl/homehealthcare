package com.homehealthcare.messaging.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.messaging.foundation.CommunicationThreadStatus;
import com.homehealthcare.messaging.foundation.MessagingEscalationStatus;
import com.homehealthcare.messaging.foundation.MessagingThreadType;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "communication_threads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunicationThread extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "thread_type", nullable = false, length = 32)
    private MessagingThreadType threadType;

    @Column(name = "subject", length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CommunicationThreadStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_membership_id", nullable = false)
    private AgencyMembership createdByMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_occurrence_id")
    private VisitOccurrence visitOccurrence;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_status", nullable = false, length = 32)
    private MessagingEscalationStatus escalationStatus;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Builder
    private CommunicationThread(
            UUID id,
            MessagingThreadType threadType,
            String subject,
            CommunicationThreadStatus status,
            AgencyMembership createdByMembership,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence,
            MessagingEscalationStatus escalationStatus,
            OffsetDateTime lastMessageAt) {
        this.id = id;
        this.threadType = Objects.requireNonNull(threadType, "threadType must not be null");
        this.subject = subject;
        this.status = Objects.requireNonNull(status, "status must not be null");
        assignCreatedByMembership(createdByMembership);
        assignBranch(branch);
        assignPatient(patient);
        assignVisitOccurrence(visitOccurrence);
        this.escalationStatus = Objects.requireNonNull(escalationStatus, "escalationStatus must not be null");
        this.lastMessageAt = lastMessageAt;
        validateState();
    }

    public static CommunicationThread create(
            MessagingThreadType threadType,
            String subject,
            AgencyMembership createdByMembership,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence) {
        return CommunicationThread.builder()
                .id(UUID.randomUUID())
                .threadType(threadType)
                .subject(subject)
                .status(CommunicationThreadStatus.ACTIVE)
                .createdByMembership(createdByMembership)
                .branch(branch)
                .patient(patient)
                .visitOccurrence(visitOccurrence)
                .escalationStatus(MessagingEscalationStatus.NORMAL)
                .build();
    }

    public void archive() {
        this.status = CommunicationThreadStatus.ARCHIVED;
    }

    public void touchLastMessageAt(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = Objects.requireNonNull(lastMessageAt, "lastMessageAt must not be null");
    }

    public void updateEscalationStatus(MessagingEscalationStatus escalationStatus) {
        this.escalationStatus = Objects.requireNonNull(escalationStatus, "escalationStatus must not be null");
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    private void assignCreatedByMembership(AgencyMembership createdByMembership) {
        this.createdByMembership = Objects.requireNonNull(createdByMembership, "createdByMembership must not be null");
        assignAgency(createdByMembership.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the communication thread");
        }
        this.branch = branch;
    }

    private void assignPatient(Patient patient) {
        if (patient != null && !Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the communication thread");
        }
        this.patient = patient;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        if (visitOccurrence != null && !Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the communication thread");
        }
        this.visitOccurrence = visitOccurrence;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        subject = normalizeOptional(subject);
        threadType = Objects.requireNonNull(threadType, "threadType must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        escalationStatus = Objects.requireNonNull(escalationStatus, "escalationStatus must not be null");
        assignBranch(branch);
        assignPatient(patient);
        assignVisitOccurrence(visitOccurrence);
        validateState();
    }

    private void validateState() {
        if (visitOccurrence != null && patient != null && !Objects.equals(visitOccurrence.getPatient().getId(), patient.getId())) {
            throw new IllegalArgumentException("visitOccurrence patient must match thread patient when both are present");
        }
        if (visitOccurrence != null && branch != null && !Objects.equals(visitOccurrence.getBranchId(), branch.getId())) {
            throw new IllegalArgumentException("visitOccurrence branch must match thread branch when both are present");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
