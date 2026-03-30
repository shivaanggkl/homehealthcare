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
@Table(name = "compliance_checklist_definitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplianceChecklistDefinition extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "severity_label", length = 60)
    private String severityLabel;

    @Column(name = "weight_score")
    private Integer weightScore;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private ComplianceChecklistDefinition(
            UUID id,
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String itemCode,
            String description,
            String severityLabel,
            Integer weightScore,
            boolean active) {
        this.id = id;
        assignAgency(agency);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.itemCode = itemCode;
        this.description = description;
        this.severityLabel = severityLabel;
        this.weightScore = weightScore;
        this.active = active;
    }

    public static ComplianceChecklistDefinition create(
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String itemCode,
            String description,
            String severityLabel,
            Integer weightScore,
            boolean active) {
        return ComplianceChecklistDefinition.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .branch(branch)
                .serviceLine(serviceLine)
                .itemCode(itemCode)
                .description(description)
                .severityLabel(severityLabel)
                .weightScore(weightScore)
                .active(active)
                .build();
    }

    public void updateDetails(Branch branch, ServiceLine serviceLine, String description, String severityLabel, Integer weightScore, boolean active) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.description = description;
        this.severityLabel = severityLabel;
        this.weightScore = weightScore;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the checklist definition");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the checklist definition");
        }
        this.serviceLine = serviceLine;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        itemCode = required(itemCode).toUpperCase(Locale.ROOT);
        description = required(description);
        severityLabel = optional(severityLabel);
        if (weightScore != null && weightScore < 0) {
            throw new IllegalArgumentException("weightScore must be zero or greater");
        }
        assignBranch(branch);
        assignServiceLine(serviceLine);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
