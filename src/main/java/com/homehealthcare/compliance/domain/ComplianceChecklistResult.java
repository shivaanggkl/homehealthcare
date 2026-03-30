package com.homehealthcare.compliance.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.compliance.foundation.ComplianceChecklistResultStatus;
import com.homehealthcare.compliance.foundation.ComplianceEvaluationCategory;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.serviceline.domain.ServiceLine;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "compliance_checklist_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplianceChecklistResult extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_category", nullable = false, length = 40)
    private ComplianceEvaluationCategory evaluationCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_definition_id")
    private ComplianceChecklistDefinition checklistDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentation_requirement_id")
    private RequiredDocumentationRequirement documentationRequirement;

    @Column(name = "result_code", nullable = false, length = 100)
    private String resultCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 32)
    private ComplianceChecklistResultStatus resultStatus;

    @Column(name = "evidence_source_type", length = 80)
    private String evidenceSourceType;

    @Column(name = "evidence_source_id")
    private UUID evidenceSourceId;

    @Column(name = "evidence_summary", length = 1000)
    private String evidenceSummary;

    @Column(name = "evaluation_origin", length = 80)
    private String evaluationOrigin;

    @Column(name = "context_period_start")
    private LocalDate contextPeriodStart;

    @Column(name = "context_period_end")
    private LocalDate contextPeriodEnd;

    @Column(name = "satisfied_by_record_type", length = 80)
    private String satisfiedByRecordType;

    @Column(name = "satisfied_by_record_id")
    private UUID satisfiedByRecordId;

    @Column(name = "missing_reason", length = 255)
    private String missingReason;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private ComplianceChecklistResult(
            UUID id,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            ComplianceEvaluationCategory evaluationCategory,
            ComplianceChecklistDefinition checklistDefinition,
            RequiredDocumentationRequirement documentationRequirement,
            String resultCode,
            ComplianceChecklistResultStatus resultStatus,
            String evidenceSourceType,
            UUID evidenceSourceId,
            String evidenceSummary,
            String evaluationOrigin,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            String satisfiedByRecordType,
            UUID satisfiedByRecordId,
            String missingReason,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.evaluationCategory = Objects.requireNonNull(evaluationCategory, "evaluationCategory must not be null");
        assignChecklistDefinition(checklistDefinition);
        assignDocumentationRequirement(documentationRequirement);
        this.resultCode = resultCode;
        this.resultStatus = Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        this.evidenceSourceType = evidenceSourceType;
        this.evidenceSourceId = evidenceSourceId;
        this.evidenceSummary = evidenceSummary;
        this.evaluationOrigin = evaluationOrigin;
        this.contextPeriodStart = contextPeriodStart;
        this.contextPeriodEnd = contextPeriodEnd;
        this.satisfiedByRecordType = satisfiedByRecordType;
        this.satisfiedByRecordId = satisfiedByRecordId;
        this.missingReason = missingReason;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static ComplianceChecklistResult createChecklistEvaluation(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            ComplianceChecklistDefinition checklistDefinition,
            ComplianceChecklistResultStatus resultStatus,
            String evidenceSourceType,
            UUID evidenceSourceId,
            String evidenceSummary,
            String evaluationOrigin,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            OffsetDateTime evaluatedAt) {
        return ComplianceChecklistResult.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .evaluationCategory(ComplianceEvaluationCategory.CHECKLIST_ITEM)
                .checklistDefinition(checklistDefinition)
                .resultCode(checklistDefinition.getItemCode())
                .resultStatus(resultStatus)
                .evidenceSourceType(evidenceSourceType)
                .evidenceSourceId(evidenceSourceId)
                .evidenceSummary(evidenceSummary)
                .evaluationOrigin(evaluationOrigin)
                .contextPeriodStart(contextPeriodStart)
                .contextPeriodEnd(contextPeriodEnd)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public static ComplianceChecklistResult createDocumentationEvaluation(
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            RequiredDocumentationRequirement documentationRequirement,
            ComplianceChecklistResultStatus resultStatus,
            String evidenceSourceType,
            UUID evidenceSourceId,
            String evidenceSummary,
            String evaluationOrigin,
            LocalDate contextPeriodStart,
            LocalDate contextPeriodEnd,
            String satisfiedByRecordType,
            UUID satisfiedByRecordId,
            String missingReason,
            OffsetDateTime evaluatedAt) {
        return ComplianceChecklistResult.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .evaluationCategory(ComplianceEvaluationCategory.REQUIRED_DOCUMENTATION)
                .documentationRequirement(documentationRequirement)
                .resultCode(documentationRequirement.getRequirementCode())
                .resultStatus(resultStatus)
                .evidenceSourceType(evidenceSourceType)
                .evidenceSourceId(evidenceSourceId)
                .evidenceSummary(evidenceSummary)
                .evaluationOrigin(evaluationOrigin)
                .contextPeriodStart(contextPeriodStart)
                .contextPeriodEnd(contextPeriodEnd)
                .satisfiedByRecordType(satisfiedByRecordType)
                .satisfiedByRecordId(satisfiedByRecordId)
                .missingReason(missingReason)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the compliance result");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the compliance result");
        }
        this.serviceLine = serviceLine;
    }

    private void assignChecklistDefinition(ComplianceChecklistDefinition checklistDefinition) {
        if (checklistDefinition != null && !Objects.equals(checklistDefinition.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("checklistDefinition must belong to the same agency as the compliance result");
        }
        this.checklistDefinition = checklistDefinition;
    }

    private void assignDocumentationRequirement(RequiredDocumentationRequirement documentationRequirement) {
        if (documentationRequirement != null && !Objects.equals(documentationRequirement.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("documentationRequirement must belong to the same agency as the compliance result");
        }
        this.documentationRequirement = documentationRequirement;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        resultCode = required(resultCode).toUpperCase(Locale.ROOT);
        evidenceSourceType = optionalUpper(evidenceSourceType);
        evidenceSummary = optional(evidenceSummary);
        evaluationOrigin = optionalUpper(evaluationOrigin);
        satisfiedByRecordType = optionalUpper(satisfiedByRecordType);
        missingReason = optionalUpper(missingReason);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignChecklistDefinition(checklistDefinition);
        assignDocumentationRequirement(documentationRequirement);
        validateState();
    }

    private void validateState() {
        if (contextPeriodStart != null && contextPeriodEnd != null && contextPeriodEnd.isBefore(contextPeriodStart)) {
            throw new IllegalArgumentException("contextPeriodEnd must not be before contextPeriodStart");
        }
        if (evaluationCategory == ComplianceEvaluationCategory.CHECKLIST_ITEM) {
            if (checklistDefinition == null || documentationRequirement != null) {
                throw new IllegalArgumentException("Checklist evaluations must reference only a checklistDefinition");
            }
        }
        if (evaluationCategory == ComplianceEvaluationCategory.REQUIRED_DOCUMENTATION) {
            if (documentationRequirement == null || checklistDefinition != null) {
                throw new IllegalArgumentException("Documentation evaluations must reference only a documentationRequirement");
            }
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String optionalUpper(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
