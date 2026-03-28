package com.homehealthcare.branchpolicy.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchpolicy.domain.BranchPolicy;
import com.homehealthcare.branchpolicy.domain.BranchPolicyRepository;
import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.ConfigurationEntityNotFoundException;
import com.homehealthcare.configuration.foundation.DuplicateConfigurationException;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class BranchPolicyCatalogService {

    private final BranchPolicyRepository branchPolicyRepository;
    private final BranchRepository branchRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public BranchPolicy create(@NotNull AgencyMembership actorMembership, @Valid ManageBranchPolicyCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_BRANCH_POLICY,
                UnauthorizedConfigurationActorException::new);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertUnique(branch.getId(), command.policyKey(), null);
        BranchPolicy saved = branchPolicyRepository.saveAndFlush(BranchPolicy.create(
                branch,
                command.policyKey(),
                command.settingsPayloadJson(),
                command.fallbackToAgencyDefault(),
                command.displayOrder()));
        saved.assignEffectiveWindow(command.effectiveFrom(), command.effectiveTo());
        saved = branchPolicyRepository.saveAndFlush(saved);
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.BRANCH_POLICY,
                saved.getId(),
                saved.getBranchId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public BranchPolicy update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID branchPolicyId,
            @Valid ManageBranchPolicyCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_BRANCH_POLICY,
                UnauthorizedConfigurationActorException::new);
        BranchPolicy policy = branchPolicyRepository.findById(branchPolicyId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("BranchPolicy", branchPolicyId));
        assertSameAgency(actorMembership.getAgencyId(), policy.getAgencyId(), "BranchPolicy", branchPolicyId);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        if (!policy.getBranchId().equals(branch.getId())) {
            throw new IllegalArgumentException("branchId cannot be changed for an existing branch policy");
        }
        assertUnique(branch.getId(), command.policyKey(), branchPolicyId);
        policy.updatePolicy(
                command.policyKey(),
                command.settingsPayloadJson(),
                command.fallbackToAgencyDefault(),
                command.displayOrder());
        policy.assignEffectiveWindow(command.effectiveFrom(), command.effectiveTo());
        policy.activate();
        BranchPolicy saved = branchPolicyRepository.saveAndFlush(policy);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.BRANCH_POLICY,
                saved.getId(),
                saved.getBranchId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public BranchPolicy deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID branchPolicyId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_BRANCH_POLICY,
                UnauthorizedConfigurationActorException::new);
        BranchPolicy policy = branchPolicyRepository.findById(branchPolicyId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("BranchPolicy", branchPolicyId));
        assertSameAgency(actorMembership.getAgencyId(), policy.getAgencyId(), "BranchPolicy", branchPolicyId);
        policy.deactivate();
        BranchPolicy saved = branchPolicyRepository.saveAndFlush(policy);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.BRANCH_POLICY,
                saved.getId(),
                saved.getBranchId(),
                metadata(saved));
        return saved;
    }

    public Optional<String> resolveEffectiveSettings(UUID branchId, String policyKey, OffsetDateTime timestamp) {
        return branchPolicyRepository.findEffectivePolicy(branchId, policyKey, timestamp)
                .map(BranchPolicy::effectiveSettingsPayloadJson);
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        return branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("Branch", branchId));
    }

    private void assertUnique(UUID branchId, String policyKey, UUID existingId) {
        boolean duplicate = existingId == null
                ? branchPolicyRepository.existsByBranch_IdAndPolicyKey(branchId, policyKey.trim())
                : branchPolicyRepository.existsByBranch_IdAndPolicyKeyAndIdNot(branchId, policyKey.trim(), existingId);
        if (duplicate) {
            throw new DuplicateConfigurationException("BranchPolicy", branchId, "policyKey", policyKey);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(BranchPolicy policy) {
        return "{\"policyKey\":\"" + policy.getPolicyKey()
                + "\",\"fallbackToAgencyDefault\":" + policy.isFallbackToAgencyDefault() + "}";
    }

    public record ManageBranchPolicyCommand(
            @NotNull UUID branchId,
            @NotBlank String policyKey,
            String settingsPayloadJson,
            boolean fallbackToAgencyDefault,
            int displayOrder,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo) {
    }
}
