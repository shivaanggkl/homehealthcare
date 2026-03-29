package com.homehealthcare.mobile.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
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
@Table(name = "mobile_message_threads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileMessageThread extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_occurrence_id")
    private VisitOccurrence visitOccurrence;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MobileMessageThreadStatus status;

    @Builder
    private MobileMessageThread(
            UUID id,
            CaregiverProfile caregiverProfile,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence,
            String subject,
            OffsetDateTime lastMessageAt,
            MobileMessageThreadStatus status) {
        this.id = id;
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        assignPatient(patient);
        assignVisitOccurrence(visitOccurrence);
        this.subject = subject;
        this.lastMessageAt = lastMessageAt;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static MobileMessageThread create(
            CaregiverProfile caregiverProfile,
            Branch branch,
            Patient patient,
            VisitOccurrence visitOccurrence,
            String subject) {
        return MobileMessageThread.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .branch(branch)
                .patient(patient)
                .visitOccurrence(visitOccurrence)
                .subject(subject)
                .status(MobileMessageThreadStatus.OPEN)
                .build();
    }

    public void touchMessage(OffsetDateTime sentAt) {
        this.lastMessageAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the thread");
        }
        this.branch = branch;
    }

    private void assignPatient(Patient patient) {
        if (patient != null && !Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the thread");
        }
        this.patient = patient;
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        if (visitOccurrence != null && !Objects.equals(visitOccurrence.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("visitOccurrence must belong to the same agency as the thread");
        }
        this.visitOccurrence = visitOccurrence;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        subject = Objects.requireNonNull(subject, "subject must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignPatient(patient);
        assignVisitOccurrence(visitOccurrence);
    }
}
