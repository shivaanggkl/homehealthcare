package com.homehealthcare.platform.audit.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditEventQueryService {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional(readOnly = true)
    public Page<AuditEvent> viewEvents(AuditEventFilter filter, Pageable pageable) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_AUDIT_LOG,
                UnauthorizedAuditLogActorException::new);

        return auditEventRepository.findAll(specification(actorMembership.getAgencyId(), filter), pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> exportEvents(AuditEventFilter filter) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.VIEW_AUDIT_LOG,
                UnauthorizedAuditLogActorException::new);

        return auditEventRepository.findAll(specification(actorMembership.getAgencyId(), filter));
    }

    private static Specification<AuditEvent> specification(UUID agencyId, AuditEventFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("agencyId"), agencyId));
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            }
            if (filter.actorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), filter.actorId()));
            }
            if (filter.actionType() != null && !filter.actionType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), filter.actionType().trim()));
            }
            if (filter.targetUserId() != null) {
                predicates.add(criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("targetType"), "USER"),
                        criteriaBuilder.equal(root.get("targetId"), filter.targetUserId())));
            }
            query.orderBy(criteriaBuilder.desc(root.get("occurredAt")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public record AuditEventFilter(
            Instant from,
            Instant to,
            UUID actorId,
            String actionType,
            UUID targetUserId) {
    }
}
