package com.homehealthcare.documentation.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.membership.domain.AgencyMembership;
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
@Table(name = "visit_documentation_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitDocumentationRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_template_id", nullable = false)
    private DocumentationTemplate selectedTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_membership_id", nullable = false)
    private AgencyMembership authorMembership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_editor_membership_id", nullable = false)
    private AgencyMembership lastEditorMembership;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentationRecordStatus status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "last_saved_at", nullable = false)
    private OffsetDateTime lastSavedAt;

    @Column(name = "printable_summary_version", nullable = false)
    private int printableSummaryVersion;

    @Builder
    private VisitDocumentationRecord(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            DocumentationTemplate selectedTemplate,
            AgencyMembership authorMembership,
            AgencyMembership lastEditorMembership,
            DocumentationRecordStatus status,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            OffsetDateTime lastSavedAt,
            int printableSummaryVersion) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignBranch(branch);
        assignSelectedTemplate(selectedTemplate);
        assignAuthorMembership(authorMembership);
        assignLastEditorMembership(lastEditorMembership);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.lastSavedAt = Objects.requireNonNull(lastSavedAt, "lastSavedAt must not be null");
        this.printableSummaryVersion = printableSummaryVersion;
        validateState();
    }

    public static VisitDocumentationRecord create(
            VisitOccurrence visitOccurrence,
            DocumentationTemplate selectedTemplate,
            AgencyMembership authorMembership,
            OffsetDateTime now) {
        return VisitDocumentationRecord.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(visitOccurrence.getPatient())
                .branch(visitOccurrence.getBranch())
                .selectedTemplate(selectedTemplate)
                .authorMembership(authorMembership)
                .lastEditorMembership(authorMembership)
                .status(DocumentationRecordStatus.DRAFT)
                .startedAt(now)
                .lastSavedAt(now)
                .printableSummaryVersion(0)
                .build();
    }

    public void markDraftSaved(AgencyMembership lastEditorMembership, OffsetDateTime now) {
        assignLastEditorMembership(lastEditorMembership);
        this.lastSavedAt = Objects.requireNonNull(now, "now must not be null");
        if (status == DocumentationRecordStatus.DRAFT) {
            this.status = DocumentationRecordStatus.IN_PROGRESS;
        }
    }

    public void submit(AgencyMembership lastEditorMembership, OffsetDateTime now) {
        assignLastEditorMembership(lastEditorMembership);
        this.lastSavedAt = Objects.requireNonNull(now, "now must not be null");
        this.submittedAt = now;
        this.status = DocumentationRecordStatus.SUBMITTED;
    }

    public void amend(AgencyMembership lastEditorMembership, OffsetDateTime now) {
        assignLastEditorMembership(lastEditorMembership);
        this.lastSavedAt = Objects.requireNonNull(now, "now must not be null");
        this.status = DocumentationRecordStatus.AMENDED;
    }

    public void incrementPrintableSummaryVersion() {
        this.printableSummaryVersion = this.printableSummaryVersion + 1;
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getSelectedTemplateId() {
        return selectedTemplate == null ? null : selectedTemplate.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the documentation record");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the documentation record");
        }
        this.branch = branch;
    }

    private void assignSelectedTemplate(DocumentationTemplate selectedTemplate) {
        this.selectedTemplate = Objects.requireNonNull(selectedTemplate, "selectedTemplate must not be null");
        if (!Objects.equals(selectedTemplate.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("selectedTemplate must belong to the same agency as the documentation record");
        }
    }

    private void assignAuthorMembership(AgencyMembership authorMembership) {
        this.authorMembership = Objects.requireNonNull(authorMembership, "authorMembership must not be null");
        if (!Objects.equals(authorMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("authorMembership must belong to the same agency as the documentation record");
        }
    }

    private void assignLastEditorMembership(AgencyMembership lastEditorMembership) {
        this.lastEditorMembership = Objects.requireNonNull(lastEditorMembership, "lastEditorMembership must not be null");
        if (!Objects.equals(lastEditorMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("lastEditorMembership must belong to the same agency as the documentation record");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        status = Objects.requireNonNull(status, "status must not be null");
        if (printableSummaryVersion < 0) {
            throw new IllegalArgumentException("printableSummaryVersion must be greater than or equal to 0");
        }
        assignPatient(patient);
        assignBranch(branch);
        assignSelectedTemplate(selectedTemplate);
        assignAuthorMembership(authorMembership);
        assignLastEditorMembership(lastEditorMembership);
        validateState();
    }

    private void validateState() {
        if (submittedAt != null && startedAt != null && submittedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("submittedAt must not be before startedAt");
        }
        if ((status == DocumentationRecordStatus.SUBMITTED || status == DocumentationRecordStatus.LOCKED) && submittedAt == null) {
            throw new IllegalArgumentException("Submitted or locked documentation must have submittedAt");
        }
    }
}
