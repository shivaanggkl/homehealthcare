package com.homehealthcare.mileagepay.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mileage_pay_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MileagePaySetting extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "reimbursement_strategy", nullable = false, length = 64)
    private MileageReimbursementStrategy reimbursementStrategy;

    @Column(name = "mileage_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal mileageRate;

    @Column(name = "travel_pay_enabled", nullable = false)
    private boolean travelPayEnabled;

    @Column(name = "visit_type_pay_adjustments_json", columnDefinition = "clob")
    private String visitTypePayAdjustmentsJson;

    @Builder
    private MileagePaySetting(
            UUID id,
            Agency agency,
            Branch branch,
            MileageReimbursementStrategy reimbursementStrategy,
            BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson) {
        this.id = id;
        assignAgency(agency);
        this.branch = branch;
        this.reimbursementStrategy = reimbursementStrategy;
        this.mileageRate = mileageRate;
        this.travelPayEnabled = travelPayEnabled;
        this.visitTypePayAdjustmentsJson = visitTypePayAdjustmentsJson;
    }

    public static MileagePaySetting create(
            Agency agency,
            Branch branch,
            MileageReimbursementStrategy reimbursementStrategy,
            BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson,
            int displayOrder) {
        validateBranch(agency, branch);
        MileagePaySetting setting = MileagePaySetting.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .branch(branch)
                .reimbursementStrategy(reimbursementStrategy)
                .mileageRate(mileageRate)
                .travelPayEnabled(travelPayEnabled)
                .visitTypePayAdjustmentsJson(visitTypePayAdjustmentsJson)
                .build();
        setting.updateDisplayOrder(displayOrder);
        setting.activate();
        return setting;
    }

    public void updateSetting(
            Branch branch,
            MileageReimbursementStrategy reimbursementStrategy,
            BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson,
            int displayOrder) {
        validateBranch(getAgency(), branch);
        this.branch = branch;
        this.reimbursementStrategy = reimbursementStrategy;
        this.mileageRate = mileageRate;
        this.travelPayEnabled = travelPayEnabled;
        this.visitTypePayAdjustmentsJson = visitTypePayAdjustmentsJson;
        updateDisplayOrder(displayOrder);
    }

    public boolean isBranchOverride() {
        return branch != null;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Objects.requireNonNull(reimbursementStrategy, "reimbursementStrategy must not be null");
        mileageRate = Objects.requireNonNull(mileageRate, "mileageRate must not be null");
        if (mileageRate.signum() < 0) {
            throw new IllegalArgumentException("mileageRate must be greater than or equal to 0");
        }
        visitTypePayAdjustmentsJson = visitTypePayAdjustmentsJson == null || visitTypePayAdjustmentsJson.trim().isBlank()
                ? null
                : visitTypePayAdjustmentsJson.trim();
        validateBranch(getAgency(), branch);
    }

    private static void validateBranch(Agency agency, Branch branch) {
        if (branch != null && !Objects.equals(agency == null ? null : agency.getId(), branch.getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the mileage pay setting");
        }
    }
}
