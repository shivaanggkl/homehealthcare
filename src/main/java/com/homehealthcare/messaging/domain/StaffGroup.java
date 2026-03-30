package com.homehealthcare.messaging.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
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
@Table(name = "staff_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffGroup extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private StaffGroup(
            UUID id,
            String name,
            String description,
            Branch branch,
            boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        assignBranch(branch);
        this.active = active;
    }

    public static StaffGroup create(Agency agency, String name, String description, Branch branch) {
        StaffGroup group = StaffGroup.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(description)
                .branch(branch)
                .active(true)
                .build();
        group.assignAgency(branch != null ? branch.getAgency() : Objects.requireNonNull(agency, "agency must not be null"));
        return group;
    }

    public void update(String name, String description, Branch branch) {
        this.name = name;
        this.description = description;
        assignBranch(branch);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            if (getAgencyId() == null) {
                assignAgency(branch.getAgency());
            } else if (!Objects.equals(branch.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("branch must belong to the same agency as the staff group");
            }
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = Objects.requireNonNull(name, "name must not be null").trim();
        description = normalizeOptional(description);
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        assignBranch(branch);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
