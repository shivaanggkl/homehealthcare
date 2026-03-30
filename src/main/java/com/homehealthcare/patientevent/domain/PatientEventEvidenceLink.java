package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLink;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.PatientEventEvidenceSourceType;
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
@Table(name = "patient_event_evidence_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientEventEvidenceLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 48)
    private Epic12PatientEventTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 48)
    private PatientEventEvidenceSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_attachment_id")
    private PatientAttachment patientAttachment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobile_artifact_id")
    private MobileFieldArtifact mobileArtifact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentation_attachment_link_id")
    private DocumentationAttachmentLink documentationAttachmentLink;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_by_membership_id", nullable = false)
    private AgencyMembership linkedByMembership;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    @Builder
    private PatientEventEvidenceLink(
            UUID id,
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientEventEvidenceSourceType sourceType,
            PatientAttachment patientAttachment,
            MobileFieldArtifact mobileArtifact,
            DocumentationAttachmentLink documentationAttachmentLink,
            AgencyMembership linkedByMembership,
            OffsetDateTime linkedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.patientAttachment = patientAttachment;
        this.mobileArtifact = mobileArtifact;
        this.documentationAttachmentLink = documentationAttachmentLink;
        assignLinkedByMembership(linkedByMembership);
        this.linkedAt = Objects.requireNonNull(linkedAt, "linkedAt must not be null");
        validateState();
    }

    public static PatientEventEvidenceLink linkPatientAttachment(
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            PatientAttachment patientAttachment,
            AgencyMembership linkedByMembership,
            OffsetDateTime linkedAt) {
        return PatientEventEvidenceLink.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .sourceType(PatientEventEvidenceSourceType.PATIENT_ATTACHMENT)
                .patientAttachment(patientAttachment)
                .linkedByMembership(linkedByMembership)
                .linkedAt(linkedAt)
                .build();
    }

    public static PatientEventEvidenceLink linkMobileArtifact(
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            MobileFieldArtifact mobileArtifact,
            AgencyMembership linkedByMembership,
            OffsetDateTime linkedAt) {
        return PatientEventEvidenceLink.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .sourceType(PatientEventEvidenceSourceType.MOBILE_FIELD_ARTIFACT)
                .mobileArtifact(mobileArtifact)
                .linkedByMembership(linkedByMembership)
                .linkedAt(linkedAt)
                .build();
    }

    public static PatientEventEvidenceLink linkDocumentationAttachment(
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            DocumentationAttachmentLink documentationAttachmentLink,
            AgencyMembership linkedByMembership,
            OffsetDateTime linkedAt) {
        return PatientEventEvidenceLink.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .sourceType(PatientEventEvidenceSourceType.DOCUMENTATION_ATTACHMENT_LINK)
                .documentationAttachmentLink(documentationAttachmentLink)
                .linkedByMembership(linkedByMembership)
                .linkedAt(linkedAt)
                .build();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the evidence link");
        }
        this.branch = branch;
    }

    private void assignLinkedByMembership(AgencyMembership linkedByMembership) {
        this.linkedByMembership = Objects.requireNonNull(linkedByMembership, "linkedByMembership must not be null");
        if (!Objects.equals(linkedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("linkedByMembership must belong to the same agency as the evidence link");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        assignBranch(branch);
        assignLinkedByMembership(linkedByMembership);
        validateState();
    }

    private void validateState() {
        int sourceCount = 0;
        if (patientAttachment != null) {
            sourceCount++;
            if (!Objects.equals(patientAttachment.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("patientAttachment must belong to the same agency as the evidence link");
            }
            if (!Objects.equals(patientAttachment.getPatient().getId(), patient.getId())) {
                throw new IllegalArgumentException("patientAttachment must belong to the same patient as the evidence link");
            }
        }
        if (mobileArtifact != null) {
            sourceCount++;
            if (!Objects.equals(mobileArtifact.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("mobileArtifact must belong to the same agency as the evidence link");
            }
            if (!Objects.equals(mobileArtifact.getPatient().getId(), patient.getId())) {
                throw new IllegalArgumentException("mobileArtifact must belong to the same patient as the evidence link");
            }
        }
        if (documentationAttachmentLink != null) {
            sourceCount++;
            if (!Objects.equals(documentationAttachmentLink.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("documentationAttachmentLink must belong to the same agency as the evidence link");
            }
            if (!Objects.equals(documentationAttachmentLink.getDocumentationRecord().getPatientId(), patient.getId())) {
                throw new IllegalArgumentException("documentationAttachmentLink must belong to the same patient as the evidence link");
            }
        }
        if (sourceCount != 1) {
            throw new IllegalArgumentException("Exactly one evidence source must be linked");
        }
        switch (sourceType) {
            case PATIENT_ATTACHMENT -> {
                if (patientAttachment == null) {
                    throw new IllegalArgumentException("sourceType PATIENT_ATTACHMENT requires patientAttachment");
                }
            }
            case MOBILE_FIELD_ARTIFACT -> {
                if (mobileArtifact == null) {
                    throw new IllegalArgumentException("sourceType MOBILE_FIELD_ARTIFACT requires mobileArtifact");
                }
            }
            case DOCUMENTATION_ATTACHMENT_LINK -> {
                if (documentationAttachmentLink == null) {
                    throw new IllegalArgumentException("sourceType DOCUMENTATION_ATTACHMENT_LINK requires documentationAttachmentLink");
                }
            }
        }
    }
}
