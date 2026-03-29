package com.homehealthcare.patientaddress.domain;

import com.homehealthcare.patient.domain.Patient;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientAddress extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "address_line_1", nullable = false, length = 250)
    private String addressLine1;

    @Column(name = "address_line_2", length = 250)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "state", nullable = false, length = 80)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "geocode_status", length = 40)
    private String geocodeStatus;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "location_notes", length = 1000)
    private String locationNotes;

    @Builder
    private PatientAddress(
            UUID id,
            Patient patient,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String geocodeStatus,
            String timezone,
            String locationNotes) {
        this.id = id;
        assignPatient(patient);
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geocodeStatus = geocodeStatus;
        this.timezone = timezone;
        this.locationNotes = locationNotes;
    }

    public static PatientAddress create(
            Patient patient,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String geocodeStatus,
            String timezone,
            String locationNotes) {
        return PatientAddress.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .addressLine1(addressLine1)
                .addressLine2(addressLine2)
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .latitude(latitude)
                .longitude(longitude)
                .geocodeStatus(geocodeStatus)
                .timezone(timezone)
                .locationNotes(locationNotes)
                .build();
    }

    public void update(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String geocodeStatus,
            String timezone,
            String locationNotes) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geocodeStatus = geocodeStatus;
        this.timezone = timezone;
        this.locationNotes = locationNotes;
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (patient == null) {
            throw new IllegalArgumentException("patient must not be null");
        }
        addressLine1 = normalizeRequired(addressLine1);
        addressLine2 = normalizeOptional(addressLine2);
        city = normalizeRequired(city);
        state = normalizeRequired(state);
        postalCode = normalizeRequired(postalCode);
        country = normalizeOptional(country);
        latitude = normalizeLatitude(latitude);
        longitude = normalizeLongitude(longitude);
        geocodeStatus = normalizeOptional(geocodeStatus);
        timezone = normalizeTimezone(timezone);
        locationNotes = normalizeOptional(locationNotes);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static BigDecimal normalizeLatitude(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-90)) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeLongitude(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.valueOf(-180)) < 0 || value.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static String normalizeTimezone(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        ZoneId.of(normalized);
        return normalized;
    }
}
