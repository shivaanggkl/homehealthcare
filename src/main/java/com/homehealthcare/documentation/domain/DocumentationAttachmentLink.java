package com.homehealthcare.documentation.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "documentation_attachment_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationAttachmentLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_record_id", nullable = false)
    private VisitDocumentationRecord documentationRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_attachment_id")
    private PatientAttachment patientAttachment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobile_artifact_id")
    private MobileFieldArtifact mobileArtifact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_by_membership_id", nullable = false)
    private AgencyMembership linkedByMembership;

    @Column(name = "caption", length = 255)
    private String caption;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    @Builder
    private DocumentationAttachmentLink(
            UUID id,
            VisitDocumentationRecord documentationRecord,
            PatientAttachment patientAttachment,
            MobileFieldArtifact mobileArtifact,
            AgencyMembership linkedByMembership,
            String caption,
            String description,
            OffsetDateTime linkedAt) {
        this.id = id;
        assignDocumentationRecord(documentationRecord);
        assignPatientAttachment(patientAttachment);
        assignMobileArtifact(mobileArtifact);
        assignLinkedByMembership(linkedByMembership);
        this.caption = caption;
        this.description = description;
        this.linkedAt = Objects.requireNonNull(linkedAt, "linkedAt must not be null");
    }

    public static DocumentationAttachmentLink linkPatientAttachment(
            VisitDocumentationRecord documentationRecord,
            PatientAttachment patientAttachment,
            AgencyMembership linkedByMembership,
            String caption,
            String description,
            OffsetDateTime linkedAt) {
        return DocumentationAttachmentLink.builder()
                .id(UUID.randomUUID())
                .documentationRecord(documentationRecord)
                .patientAttachment(patientAttachment)
                .linkedByMembership(linkedByMembership)
                .caption(caption)
                .description(description)
                .linkedAt(linkedAt)
                .build();
    }

    public static DocumentationAttachmentLink linkMobileArtifact(
            VisitDocumentationRecord documentationRecord,
            MobileFieldArtifact mobileArtifact,
            AgencyMembership linkedByMembership,
            String caption,
            String description,
            OffsetDateTime linkedAt) {
        return DocumentationAttachmentLink.builder()
                .id(UUID.randomUUID())
                .documentationRecord(documentationRecord)
                .mobileArtifact(mobileArtifact)
                .linkedByMembership(linkedByMembership)
                .caption(caption)
                .description(description)
                .linkedAt(linkedAt)
                .build();
    }

    private void assignDocumentationRecord(VisitDocumentationRecord documentationRecord) {
        this.documentationRecord = Objects.requireNonNull(documentationRecord, "documentationRecord must not be null");
        assignAgency(documentationRecord.getAgency());
    }

    private void assignPatientAttachment(PatientAttachment patientAttachment) {
        if (patientAttachment != null) {
            if (!Objects.equals(patientAttachment.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("patientAttachment must belong to the same agency as the documentation record");
            }
            if (!Objects.equals(patientAttachment.getPatient().getId(), documentationRecord.getPatientId())) {
                throw new IllegalArgumentException("patientAttachment must belong to the same patient as the documentation record");
            }
        }
        this.patientAttachment = patientAttachment;
    }

    private void assignMobileArtifact(MobileFieldArtifact mobileArtifact) {
        if (mobileArtifact != null) {
            if (!Objects.equals(mobileArtifact.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("mobileArtifact must belong to the same agency as the documentation record");
            }
            if (!Objects.equals(mobileArtifact.getPatient().getId(), documentationRecord.getPatientId())) {
                throw new IllegalArgumentException("mobileArtifact must belong to the same patient as the documentation record");
            }
            if (!Objects.equals(mobileArtifact.getVisitOccurrenceId(), documentationRecord.getVisitOccurrenceId())) {
                throw new IllegalArgumentException("mobileArtifact must belong to the same visit as the documentation record");
            }
        }
        this.mobileArtifact = mobileArtifact;
    }

    private void assignLinkedByMembership(AgencyMembership linkedByMembership) {
        this.linkedByMembership = Objects.requireNonNull(linkedByMembership, "linkedByMembership must not be null");
        if (!Objects.equals(linkedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("linkedByMembership must belong to the same agency as the documentation record");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        caption = optional(caption);
        description = optional(description);
        if (patientAttachment == null && mobileArtifact == null) {
            throw new IllegalArgumentException("Either patientAttachment or mobileArtifact must be linked");
        }
        if (patientAttachment != null && mobileArtifact != null) {
            throw new IllegalArgumentException("Only one attachment source may be linked per record");
        }
        assignPatientAttachment(patientAttachment);
        assignMobileArtifact(mobileArtifact);
        assignLinkedByMembership(linkedByMembership);
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
