package com.homehealthcare.evv.domain;

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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_device_metadata_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceMetadataSnapshot extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clock_event_id", nullable = false)
    private EvvClockEvent clockEvent;

    @Column(name = "platform_summary", length = 80)
    private String platformSummary;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "device_class", length = 40)
    private String deviceClass;

    @Column(name = "timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;

    @Column(name = "session_fingerprint_hash", length = 128)
    private String sessionFingerprintHash;

    @Builder
    private DeviceMetadataSnapshot(
            UUID id,
            EvvClockEvent clockEvent,
            String platformSummary,
            String appVersion,
            String deviceClass,
            Integer timezoneOffsetMinutes,
            String userAgentHash,
            String sessionFingerprintHash) {
        this.id = id;
        assignClockEvent(clockEvent);
        this.platformSummary = platformSummary;
        this.appVersion = appVersion;
        this.deviceClass = deviceClass;
        this.timezoneOffsetMinutes = timezoneOffsetMinutes;
        this.userAgentHash = userAgentHash;
        this.sessionFingerprintHash = sessionFingerprintHash;
    }

    public static DeviceMetadataSnapshot capture(
            EvvClockEvent clockEvent,
            String platformSummary,
            String appVersion,
            String deviceClass,
            Integer timezoneOffsetMinutes,
            String userAgentHash,
            String sessionFingerprintHash) {
        return DeviceMetadataSnapshot.builder()
                .id(UUID.randomUUID())
                .clockEvent(clockEvent)
                .platformSummary(platformSummary)
                .appVersion(appVersion)
                .deviceClass(deviceClass)
                .timezoneOffsetMinutes(timezoneOffsetMinutes)
                .userAgentHash(userAgentHash)
                .sessionFingerprintHash(sessionFingerprintHash)
                .build();
    }

    private void assignClockEvent(EvvClockEvent clockEvent) {
        this.clockEvent = Objects.requireNonNull(clockEvent, "clockEvent must not be null");
        assignAgency(clockEvent.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        platformSummary = optional(platformSummary);
        appVersion = optional(appVersion);
        deviceClass = optional(deviceClass);
        userAgentHash = optional(userAgentHash);
        sessionFingerprintHash = optional(sessionFingerprintHash);
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
