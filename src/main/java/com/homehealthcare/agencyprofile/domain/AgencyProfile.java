package com.homehealthcare.agencyprofile.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.ZoneId;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agency_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyProfile extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "primary_phone", length = 30)
    private String primaryPhone;

    @Column(name = "primary_address", length = 500)
    private String primaryAddress;

    @Column(name = "operations_contact_name", length = 200)
    private String operationsContactName;

    @Column(name = "operations_contact_email", length = 320)
    private String operationsContactEmail;

    @Column(name = "support_contact_name", length = 200)
    private String supportContactName;

    @Column(name = "support_contact_email", length = 320)
    private String supportContactEmail;

    @Column(name = "default_timezone", nullable = false, length = 64)
    private String defaultTimezone;

    @Column(name = "default_locale", nullable = false, length = 35)
    private String defaultLocale;

    @Builder
    private AgencyProfile(
            UUID id,
            Agency agency,
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            String operationsContactEmail,
            String supportContactName,
            String supportContactEmail,
            String defaultTimezone,
            String defaultLocale) {
        this.id = id;
        assignAgency(agency);
        this.displayName = displayName;
        this.legalName = legalName;
        this.primaryPhone = primaryPhone;
        this.primaryAddress = primaryAddress;
        this.operationsContactName = operationsContactName;
        this.operationsContactEmail = operationsContactEmail;
        this.supportContactName = supportContactName;
        this.supportContactEmail = supportContactEmail;
        this.defaultTimezone = defaultTimezone;
        this.defaultLocale = defaultLocale;
    }

    public static AgencyProfile create(
            Agency agency,
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            String operationsContactEmail,
            String supportContactName,
            String supportContactEmail,
            String defaultTimezone,
            String defaultLocale) {
        AgencyProfile profile = AgencyProfile.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .displayName(displayName)
                .legalName(legalName)
                .primaryPhone(primaryPhone)
                .primaryAddress(primaryAddress)
                .operationsContactName(operationsContactName)
                .operationsContactEmail(operationsContactEmail)
                .supportContactName(supportContactName)
                .supportContactEmail(supportContactEmail)
                .defaultTimezone(defaultTimezone)
                .defaultLocale(defaultLocale)
                .build();
        profile.activate();
        return profile;
    }

    public void updateProfile(
            String displayName,
            String legalName,
            String primaryPhone,
            String primaryAddress,
            String operationsContactName,
            String operationsContactEmail,
            String supportContactName,
            String supportContactEmail,
            String defaultTimezone,
            String defaultLocale) {
        this.displayName = displayName;
        this.legalName = legalName;
        this.primaryPhone = primaryPhone;
        this.primaryAddress = primaryAddress;
        this.operationsContactName = operationsContactName;
        this.operationsContactEmail = operationsContactEmail;
        this.supportContactName = supportContactName;
        this.supportContactEmail = supportContactEmail;
        this.defaultTimezone = defaultTimezone;
        this.defaultLocale = defaultLocale;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        displayName = normalizeOptional(displayName);
        legalName = normalizeOptional(legalName);
        primaryPhone = normalizeOptional(primaryPhone);
        primaryAddress = normalizeOptional(primaryAddress);
        operationsContactName = normalizeOptional(operationsContactName);
        operationsContactEmail = normalizeEmail(operationsContactEmail);
        supportContactName = normalizeOptional(supportContactName);
        supportContactEmail = normalizeEmail(supportContactEmail);
        defaultTimezone = normalizeTimezone(defaultTimezone);
        defaultLocale = normalizeLocale(defaultLocale);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeTimezone(String value) {
        String normalized = Objects.requireNonNull(value, "defaultTimezone must not be null").trim();
        ZoneId.of(normalized);
        return normalized;
    }

    private static String normalizeLocale(String value) {
        String normalized = Objects.requireNonNull(value, "defaultLocale must not be null").trim();
        try {
            return new Locale.Builder().setLanguageTag(normalized).build().toLanguageTag();
        } catch (IllformedLocaleException ex) {
            throw new IllegalArgumentException("Invalid locale tag: " + normalized, ex);
        }
    }
}
