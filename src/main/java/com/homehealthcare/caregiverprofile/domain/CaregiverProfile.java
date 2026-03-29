package com.homehealthcare.caregiverprofile.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
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
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverProfile extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_membership_id", nullable = false)
    private AgencyMembership agencyMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_branch_id")
    private Branch primaryBranch;

    @Column(name = "caregiver_code", length = 100)
    private String caregiverCode;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "employment_type", length = 60)
    private String employmentType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverProfile(
            UUID id,
            AgencyMembership agencyMembership,
            Branch primaryBranch,
            String caregiverCode,
            String displayName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignMembership(agencyMembership);
        assignPrimaryBranch(primaryBranch);
        this.caregiverCode = caregiverCode;
        this.displayName = displayName;
        this.employmentType = employmentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateDateWindow();
    }

    public static CaregiverProfile create(
            AgencyMembership agencyMembership,
            Branch primaryBranch,
            String caregiverCode,
            String displayName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        return CaregiverProfile.builder()
                .id(UUID.randomUUID())
                .agencyMembership(agencyMembership)
                .primaryBranch(primaryBranch)
                .caregiverCode(caregiverCode)
                .displayName(displayName)
                .employmentType(employmentType)
                .startDate(startDate)
                .endDate(endDate)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            Branch primaryBranch,
            String caregiverCode,
            String displayName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        assignPrimaryBranch(primaryBranch);
        this.caregiverCode = caregiverCode;
        this.displayName = displayName;
        this.employmentType = employmentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        validateDateWindow();
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
    }

    public void suspend() {
        this.status = WorkforceLifecycleStatus.SUSPENDED;
    }

    public void markUnschedulable() {
        this.status = WorkforceLifecycleStatus.UNSCHEDULABLE;
    }

    public UUID getAgencyMembershipId() {
        return agencyMembership == null ? null : agencyMembership.getId();
    }

    public UUID getUserId() {
        return agencyMembership == null ? null : agencyMembership.getUserId();
    }

    public UUID getPrimaryBranchId() {
        return primaryBranch == null ? null : primaryBranch.getId();
    }

    private void assignMembership(AgencyMembership agencyMembership) {
        this.agencyMembership = Objects.requireNonNull(agencyMembership, "agencyMembership must not be null");
        assignAgency(agencyMembership.getAgency());
    }

    private void assignPrimaryBranch(Branch primaryBranch) {
        if (primaryBranch != null && !Objects.equals(primaryBranch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("primaryBranch must belong to the same agency as the caregiver profile");
        }
        this.primaryBranch = primaryBranch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (agencyMembership == null) {
            throw new IllegalArgumentException("agencyMembership must not be null");
        }
        caregiverCode = normalizeOptional(caregiverCode);
        if (caregiverCode != null) {
            caregiverCode = caregiverCode.toUpperCase(Locale.ROOT);
        }
        displayName = normalizeOptional(displayName);
        employmentType = normalizeOptional(employmentType);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        assignPrimaryBranch(primaryBranch);
        validateDateWindow();
    }

    private void validateDateWindow() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
