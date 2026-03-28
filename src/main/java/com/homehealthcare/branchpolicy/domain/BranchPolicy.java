package com.homehealthcare.branchpolicy.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.configuration.foundation.BranchPolicyConfigurationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "branch_policies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchPolicy extends BranchPolicyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "settings_payload_json", columnDefinition = "clob")
    private String settingsPayloadJson;

    @Column(name = "fallback_to_agency_default", nullable = false)
    private boolean fallbackToAgencyDefault;

    @Builder
    private BranchPolicy(
            UUID id,
            Branch branch,
            String policyKey,
            String settingsPayloadJson,
            boolean fallbackToAgencyDefault) {
        this.id = id;
        assignBranch(branch);
        this.policyKey = policyKey;
        this.settingsPayloadJson = settingsPayloadJson;
        this.fallbackToAgencyDefault = fallbackToAgencyDefault;
    }

    public static BranchPolicy create(
            Branch branch,
            String policyKey,
            String settingsPayloadJson,
            boolean fallbackToAgencyDefault,
            int displayOrder) {
        BranchPolicy policy = BranchPolicy.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .policyKey(policyKey)
                .settingsPayloadJson(settingsPayloadJson)
                .fallbackToAgencyDefault(fallbackToAgencyDefault)
                .build();
        policy.updateDisplayOrder(displayOrder);
        policy.activate();
        return policy;
    }

    public void updatePolicy(
            String policyKey,
            String settingsPayloadJson,
            boolean fallbackToAgencyDefault,
            int displayOrder) {
        this.policyKey = policyKey;
        this.settingsPayloadJson = settingsPayloadJson;
        this.fallbackToAgencyDefault = fallbackToAgencyDefault;
        updateDisplayOrder(displayOrder);
    }

    public boolean usesAgencyDefault() {
        return fallbackToAgencyDefault;
    }

    public String effectiveSettingsPayloadJson() {
        return fallbackToAgencyDefault ? null : settingsPayloadJson;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        policyKey = Objects.requireNonNull(policyKey, "policyKey must not be null").trim();
        settingsPayloadJson = settingsPayloadJson == null || settingsPayloadJson.trim().isBlank()
                ? null
                : settingsPayloadJson.trim();
        if (!fallbackToAgencyDefault && settingsPayloadJson == null) {
            throw new IllegalArgumentException("settingsPayloadJson must be present when fallbackToAgencyDefault is false");
        }
    }
}
