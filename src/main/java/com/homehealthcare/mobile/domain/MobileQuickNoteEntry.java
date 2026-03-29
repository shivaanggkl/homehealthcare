package com.homehealthcare.mobile.domain;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
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
@Table(name = "mobile_quick_note_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileQuickNoteEntry extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_session_id", nullable = false)
    private MobileVisitExecutionSession executionSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @Column(name = "authored_at", nullable = false)
    private OffsetDateTime authoredAt;

    @Column(name = "note_text", nullable = false, length = 2000)
    private String noteText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MobileQuickNoteStatus status;

    @Builder
    private MobileQuickNoteEntry(
            UUID id,
            MobileVisitExecutionSession executionSession,
            CaregiverProfile caregiverProfile,
            OffsetDateTime authoredAt,
            String noteText,
            MobileQuickNoteStatus status) {
        this.id = id;
        assignExecutionSession(executionSession);
        assignCaregiverProfile(caregiverProfile);
        this.authoredAt = Objects.requireNonNull(authoredAt, "authoredAt must not be null");
        this.noteText = noteText;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static MobileQuickNoteEntry create(
            MobileVisitExecutionSession executionSession,
            CaregiverProfile caregiverProfile,
            OffsetDateTime authoredAt,
            String noteText,
            MobileQuickNoteStatus status) {
        return MobileQuickNoteEntry.builder()
                .id(UUID.randomUUID())
                .executionSession(executionSession)
                .caregiverProfile(caregiverProfile)
                .authoredAt(authoredAt)
                .noteText(noteText)
                .status(status)
                .build();
    }

    public void update(String noteText, MobileQuickNoteStatus status) {
        this.noteText = noteText;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    private void assignExecutionSession(MobileVisitExecutionSession executionSession) {
        this.executionSession = Objects.requireNonNull(executionSession, "executionSession must not be null");
        assignAgency(executionSession.getAgency());
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the quick note");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        noteText = Objects.requireNonNull(noteText, "noteText must not be null").trim();
        status = Objects.requireNonNull(status, "status must not be null");
        assignCaregiverProfile(caregiverProfile);
    }
}
