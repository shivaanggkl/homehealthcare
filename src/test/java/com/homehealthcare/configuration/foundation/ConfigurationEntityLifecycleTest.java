package com.homehealthcare.configuration.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ConfigurationEntityLifecycleTest {

    @Test
    void agencyConfigurationSupportsSharedLifecycleTransitionsAndEffectiveWindow() {
        TestAgencyConfigurationEntity entity = new TestAgencyConfigurationEntity();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-12-31T23:59:59Z");

        entity.markDraft();
        entity.updateDisplayOrder(4);
        entity.assignEffectiveWindow(start, end);
        entity.activate();

        assertThat(entity.getStatus()).isEqualTo(ConfigurationStatus.ACTIVE);
        assertThat(entity.getDisplayOrder()).isEqualTo(4);
        assertThat(entity.scopeType()).isEqualTo(ConfigurationScopeType.AGENCY_MASTER_DATA);
        assertThat(entity.isEffectiveAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"))).isTrue();

        entity.deactivate();
        assertThat(entity.getStatus()).isEqualTo(ConfigurationStatus.INACTIVE);
        assertThat(entity.isEffectiveAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"))).isFalse();

        entity.archive();
        assertThat(entity.getStatus()).isEqualTo(ConfigurationStatus.ARCHIVED);
    }

    @Test
    void branchPolicyConfigurationUsesBranchOverrideScope() {
        TestBranchPolicyConfigurationEntity entity = new TestBranchPolicyConfigurationEntity();

        entity.activate();

        assertThat(entity.scopeType()).isEqualTo(ConfigurationScopeType.BRANCH_OVERRIDE_POLICY);
        assertThat(entity.getStatus()).isEqualTo(ConfigurationStatus.ACTIVE);
    }

    @Test
    void invalidEffectiveWindowIsRejected() {
        TestAgencyConfigurationEntity entity = new TestAgencyConfigurationEntity();
        OffsetDateTime start = OffsetDateTime.parse("2026-12-31T23:59:59Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> entity.assignEffectiveWindow(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("effectiveTo must be greater than or equal to effectiveFrom");
    }

    @Test
    void negativeDisplayOrderIsRejected() {
        TestAgencyConfigurationEntity entity = new TestAgencyConfigurationEntity();

        assertThatThrownBy(() -> entity.updateDisplayOrder(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayOrder must be greater than or equal to 0");
    }

    private static final class TestAgencyConfigurationEntity extends AgencyConfigurationEntity {
    }

    private static final class TestBranchPolicyConfigurationEntity extends BranchPolicyConfigurationEntity {
    }
}
