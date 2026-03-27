package com.homehealthcare.shared.persistence;

import com.homehealthcare.security.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

public class TenantAwareJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;
    private final Class<T> domainClass;

    public TenantAwareJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    @Override
    public Optional<T> findById(ID id) {
        Optional<UUID> tenantAgencyId = currentTenantAgencyId();
        if (tenantAgencyId.isEmpty()) {
            return super.findById(id);
        }

        return entityManager.createQuery(
                        "select entity from " + entityInformation.getEntityName()
                                + " entity where entity.id = :id and entity.agency.id = :agencyId",
                        domainClass)
                .setParameter("id", id)
                .setParameter("agencyId", tenantAgencyId.get())
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsById(ID id) {
        Optional<UUID> tenantAgencyId = currentTenantAgencyId();
        if (tenantAgencyId.isEmpty()) {
            return super.existsById(id);
        }

        Long count = entityManager.createQuery(
                        "select count(entity) from " + entityInformation.getEntityName()
                                + " entity where entity.id = :id and entity.agency.id = :agencyId",
                        Long.class)
                .setParameter("id", id)
                .setParameter("agencyId", tenantAgencyId.get())
                .getSingleResult();

        return count != null && count > 0;
    }

    private Optional<UUID> currentTenantAgencyId() {
        if (!AgencyScopedEntity.class.isAssignableFrom(domainClass)) {
            return Optional.empty();
        }

        return TenantContextHolder.get().map(context -> context.agencyId());
    }
}
