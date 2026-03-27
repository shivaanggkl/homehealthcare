package com.homehealthcare.membership.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agency_memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyMembership extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role_key", nullable = false, length = 64)
    private AgencyRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AgencyMembershipStatus status;

    @OneToMany(mappedBy = "agencyMembership")
    private Set<BranchAssignment> branchAssignments = new LinkedHashSet<>();

    @Builder
    private AgencyMembership(
            UUID id,
            User user,
            Agency agency,
            AgencyRole role,
            AgencyMembershipStatus status) {
        this.id = id;
        this.user = user;
        assignAgency(agency);
        this.role = role;
        this.status = status;
    }

    public static AgencyMembership grant(User user, Agency agency, AgencyRole role) {
        return AgencyMembership.builder()
                .id(UUID.randomUUID())
                .user(user)
                .agency(agency)
                .role(role)
                .status(AgencyMembershipStatus.ACTIVE)
                .build();
    }

    public void activate() {
        this.status = AgencyMembershipStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = AgencyMembershipStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == AgencyMembershipStatus.ACTIVE;
    }

    public void changeRole(AgencyRole role) {
        this.role = role;
    }

    public UUID getUserId() {
        return user == null ? null : user.getId();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AgencyMembership membership)) {
            return false;
        }
        return id != null && Objects.equals(id, membership.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
