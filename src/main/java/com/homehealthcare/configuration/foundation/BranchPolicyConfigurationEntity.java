package com.homehealthcare.configuration.foundation;

import com.homehealthcare.shared.persistence.BranchScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BranchPolicyConfigurationEntity extends BranchScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConfigurationStatus status = ConfigurationStatus.DRAFT;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    public ConfigurationScopeType scopeType() {
        return ConfigurationScopeType.BRANCH_OVERRIDE_POLICY;
    }

    public void markDraft() {
        this.status = ConfigurationStatus.DRAFT;
    }

    public void activate() {
        this.status = ConfigurationStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ConfigurationStatus.INACTIVE;
    }

    public void archive() {
        this.status = ConfigurationStatus.ARCHIVED;
    }

    public void updateDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be greater than or equal to 0");
        }
        this.displayOrder = displayOrder;
    }

    public void assignEffectiveWindow(OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be greater than or equal to effectiveFrom");
        }
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public boolean isEffectiveAt(OffsetDateTime timestamp) {
        OffsetDateTime probe = timestamp == null ? OffsetDateTime.now() : timestamp;
        boolean startsBefore = effectiveFrom == null || !probe.isBefore(effectiveFrom);
        boolean endsAfter = effectiveTo == null || !probe.isAfter(effectiveTo);
        return status == ConfigurationStatus.ACTIVE && startsBefore && endsAfter;
    }
}
