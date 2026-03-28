package com.homehealthcare.alertrule.domain;

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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "alert_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertRule extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 64)
    private AlertRuleType ruleType;

    @Column(name = "config_payload_json", nullable = false, columnDefinition = "clob")
    private String configPayloadJson;

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail;

    @Column(name = "notify_sms", nullable = false)
    private boolean notifySms;

    @Column(name = "notify_in_app", nullable = false)
    private boolean notifyInApp;

    @Builder
    private AlertRule(
            UUID id,
            Agency agency,
            Branch branch,
            String name,
            AlertRuleType ruleType,
            String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp) {
        this.id = id;
        assignAgency(agency);
        this.branch = branch;
        this.name = name;
        this.ruleType = ruleType;
        this.configPayloadJson = configPayloadJson;
        this.notifyEmail = notifyEmail;
        this.notifySms = notifySms;
        this.notifyInApp = notifyInApp;
    }

    public static AlertRule create(
            Agency agency,
            Branch branch,
            String name,
            AlertRuleType ruleType,
            String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp,
            int displayOrder) {
        validateBranch(agency, branch);
        AlertRule rule = AlertRule.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .branch(branch)
                .name(name)
                .ruleType(ruleType)
                .configPayloadJson(configPayloadJson)
                .notifyEmail(notifyEmail)
                .notifySms(notifySms)
                .notifyInApp(notifyInApp)
                .build();
        rule.updateDisplayOrder(displayOrder);
        rule.activate();
        return rule;
    }

    public void updateRule(
            Branch branch,
            String name,
            AlertRuleType ruleType,
            String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp,
            int displayOrder) {
        validateBranch(getAgency(), branch);
        this.branch = branch;
        this.name = name;
        this.ruleType = ruleType;
        this.configPayloadJson = configPayloadJson;
        this.notifyEmail = notifyEmail;
        this.notifySms = notifySms;
        this.notifyInApp = notifyInApp;
        updateDisplayOrder(displayOrder);
    }

    public boolean isBranchSpecific() {
        return branch != null;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = Objects.requireNonNull(name, "name must not be null").trim();
        configPayloadJson = Objects.requireNonNull(configPayloadJson, "configPayloadJson must not be null").trim();
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        validateBranch(getAgency(), branch);
        if (!notifyEmail && !notifySms && !notifyInApp) {
            throw new IllegalArgumentException("At least one delivery mode must be enabled");
        }
    }

    private static void validateBranch(Agency agency, Branch branch) {
        if (branch != null && !Objects.equals(agency == null ? null : agency.getId(), branch.getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the alert rule");
        }
    }
}
