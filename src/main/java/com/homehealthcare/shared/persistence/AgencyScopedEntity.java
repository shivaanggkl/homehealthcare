package com.homehealthcare.shared.persistence;

import com.homehealthcare.agency.domain.Agency;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class AgencyScopedEntity extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    protected void assignAgency(Agency agency) {
        this.agency = agency;
    }

    public UUID getAgencyId() {
        return agency == null ? null : agency.getId();
    }
}
