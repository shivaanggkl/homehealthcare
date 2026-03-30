package com.homehealthcare.compliance.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.serviceline.domain.ServiceLine;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "required_documentation_requirements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredDocumentationRequirement extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "source_category", nullable = false, length = 80)
    private String sourceCategory;

    @Column(name = "requirement_code", nullable = false, length = 100)
    private String requirementCode;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "patient_applicable", nullable = false)
    private boolean patientApplicable;

    @Column(name = "episode_applicable", nullable = false)
    private boolean episodeApplicable;

    @Column(name = "due_days")
    private Integer dueDays;

    @Column(name = "recency_days")
    private Integer recencyDays;

    @Column(name = "requires_signature_verification", nullable = false)
    private boolean requiresSignatureVerification;

    @Column(name = "requires_attachment_evidence", nullable = false)
    private boolean requiresAttachmentEvidence;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private RequiredDocumentationRequirement(
            UUID id,
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String sourceCategory,
            String requirementCode,
            String description,
            boolean patientApplicable,
            boolean episodeApplicable,
            Integer dueDays,
            Integer recencyDays,
            boolean requiresSignatureVerification,
            boolean requiresAttachmentEvidence,
            boolean active) {
        this.id = id;
        assignAgency(agency);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.sourceCategory = sourceCategory;
        this.requirementCode = requirementCode;
        this.description = description;
        this.patientApplicable = patientApplicable;
        this.episodeApplicable = episodeApplicable;
        this.dueDays = dueDays;
        this.recencyDays = recencyDays;
        this.requiresSignatureVerification = requiresSignatureVerification;
        this.requiresAttachmentEvidence = requiresAttachmentEvidence;
        this.active = active;
    }

    public static RequiredDocumentationRequirement create(
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String sourceCategory,
            String requirementCode,
            String description,
            boolean patientApplicable,
            boolean episodeApplicable,
            Integer dueDays,
            Integer recencyDays,
            boolean requiresSignatureVerification,
            boolean requiresAttachmentEvidence,
            boolean active) {
        return RequiredDocumentationRequirement.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .branch(branch)
                .serviceLine(serviceLine)
                .sourceCategory(sourceCategory)
                .requirementCode(requirementCode)
                .description(description)
                .patientApplicable(patientApplicable)
                .episodeApplicable(episodeApplicable)
                .dueDays(dueDays)
                .recencyDays(recencyDays)
                .requiresSignatureVerification(requiresSignatureVerification)
                .requiresAttachmentEvidence(requiresAttachmentEvidence)
                .active(active)
                .build();
    }

    public void updateDetails(
            Branch branch,
            ServiceLine serviceLine,
            String sourceCategory,
            String description,
            boolean patientApplicable,
            boolean episodeApplicable,
            Integer dueDays,
            Integer recencyDays,
            boolean requiresSignatureVerification,
            boolean requiresAttachmentEvidence,
            boolean active) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.sourceCategory = sourceCategory;
        this.description = description;
        this.patientApplicable = patientApplicable;
        this.episodeApplicable = episodeApplicable;
        this.dueDays = dueDays;
        this.recencyDays = recencyDays;
        this.requiresSignatureVerification = requiresSignatureVerification;
        this.requiresAttachmentEvidence = requiresAttachmentEvidence;
        this.active = active;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the documentation requirement");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the documentation requirement");
        }
        this.serviceLine = serviceLine;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        sourceCategory = required(sourceCategory).toUpperCase(Locale.ROOT);
        requirementCode = required(requirementCode).toUpperCase(Locale.ROOT);
        description = required(description);
        if (dueDays != null && dueDays < 0) {
            throw new IllegalArgumentException("dueDays must be zero or greater");
        }
        if (recencyDays != null && recencyDays < 0) {
            throw new IllegalArgumentException("recencyDays must be zero or greater");
        }
        assignBranch(branch);
        assignServiceLine(serviceLine);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
