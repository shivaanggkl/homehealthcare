package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionSeverity;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionTargetType;
import com.homehealthcare.revenuereadiness.foundation.RevenueExceptionType;
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
@Table(name = "revenue_exception_flags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevenueExceptionFlag extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 48)
    private RevenueExceptionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 64)
    private RevenueExceptionType exceptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private RevenueExceptionSeverity severity;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "cleared_at")
    private OffsetDateTime clearedAt;

    @Builder
    private RevenueExceptionFlag(
            UUID id,
            Branch branch,
            RevenueExceptionTargetType targetType,
            UUID targetId,
            RevenueExceptionType exceptionType,
            RevenueExceptionSeverity severity,
            String reasonCode,
            String summary,
            OffsetDateTime detectedAt,
            OffsetDateTime clearedAt) {
        this.id = id;
        assignBranch(branch);
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.exceptionType = Objects.requireNonNull(exceptionType, "exceptionType must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        this.clearedAt = clearedAt;
    }

    public static RevenueExceptionFlag detect(
            com.homehealthcare.agency.domain.Agency agency,
            Branch branch,
            RevenueExceptionTargetType targetType,
            UUID targetId,
            RevenueExceptionType exceptionType,
            RevenueExceptionSeverity severity,
            String reasonCode,
            String summary,
            OffsetDateTime detectedAt) {
        RevenueExceptionFlag flag = RevenueExceptionFlag.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .exceptionType(exceptionType)
                .severity(severity)
                .reasonCode(reasonCode)
                .summary(summary)
                .detectedAt(detectedAt)
                .build();
        flag.assignAgency(agency);
        return flag;
    }

    public void refresh(RevenueExceptionSeverity severity, String reasonCode, String summary, OffsetDateTime detectedAt) {
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        this.clearedAt = null;
    }

    public void clear(OffsetDateTime clearedAt) {
        this.clearedAt = Objects.requireNonNull(clearedAt, "clearedAt must not be null");
    }

    public boolean isActive() {
        return clearedAt == null;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            if (getAgencyId() == null) {
                assignAgency(branch.getAgency());
            }
            if (!Objects.equals(branch.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("branch must belong to the same agency as the revenue exception flag");
            }
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reasonCode = normalizeRequired(reasonCode);
        summary = normalizeRequired(summary);
        assignBranch(branch);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }
}
