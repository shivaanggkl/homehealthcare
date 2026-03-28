package com.homehealthcare.branchpolicy.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.homehealthcare.branchpolicy.application.BranchPolicyCatalogService;
import com.homehealthcare.branchpolicy.domain.BranchPolicy;
import com.homehealthcare.branchpolicy.domain.BranchPolicyRepository;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.ConfigurationPayloadValidator;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branch-policies")
class BranchPolicyController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationPayloadValidator configurationPayloadValidator;
    private final BranchPolicyRepository branchPolicyRepository;
    private final BranchPolicyCatalogService branchPolicyCatalogService;

    BranchPolicyController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            ConfigurationPayloadValidator configurationPayloadValidator,
            BranchPolicyRepository branchPolicyRepository,
            BranchPolicyCatalogService branchPolicyCatalogService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.configurationPayloadValidator = configurationPayloadValidator;
        this.branchPolicyRepository = branchPolicyRepository;
        this.branchPolicyCatalogService = branchPolicyCatalogService;
    }

    @GetMapping
    PagedResponse<BranchPolicyResponse> list(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "policyKey", required = false) String policyKey,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actor = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actor,
                AgencyPermission.VIEW_BRANCH_POLICY,
                UnauthorizedConfigurationActorException::new);
        List<BranchPolicyResponse> filtered = branchPolicyRepository.findAllByAgency_IdOrderByPolicyKeyAsc(actor.getAgencyId())
                .stream()
                .filter(item -> branchId == null || branchId.equals(item.getBranchId()))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> policyKey == null || policyKey.isBlank() || item.getPolicyKey().equalsIgnoreCase(policyKey.trim()))
                .sorted(Comparator.comparing(BranchPolicy::getBranchId).thenComparing(BranchPolicy::getPolicyKey))
                .map(BranchPolicyController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    BranchPolicyResponse create(@Valid @RequestBody ManageBranchPolicyRequest request) {
        validatePayload(request.settingsPayloadJson(), request.fallbackToAgencyDefault());
        return toResponse(branchPolicyCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new BranchPolicyCatalogService.ManageBranchPolicyCommand(
                        request.branchId(),
                        request.policyKey(),
                        request.settingsPayloadJson(),
                        request.fallbackToAgencyDefault(),
                        request.displayOrder(),
                        request.effectiveFrom(),
                        request.effectiveTo())));
    }

    @PutMapping("/{branchPolicyId}")
    BranchPolicyResponse update(@PathVariable UUID branchPolicyId, @Valid @RequestBody ManageBranchPolicyRequest request) {
        validatePayload(request.settingsPayloadJson(), request.fallbackToAgencyDefault());
        return toResponse(branchPolicyCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                branchPolicyId,
                new BranchPolicyCatalogService.ManageBranchPolicyCommand(
                        request.branchId(),
                        request.policyKey(),
                        request.settingsPayloadJson(),
                        request.fallbackToAgencyDefault(),
                        request.displayOrder(),
                        request.effectiveFrom(),
                        request.effectiveTo())));
    }

    @DeleteMapping("/{branchPolicyId}")
    BranchPolicyResponse deactivate(@PathVariable UUID branchPolicyId) {
        return toResponse(branchPolicyCatalogService.deactivate(
                configurationActorResolver.requireActorMembership(),
                branchPolicyId));
    }

    private void validatePayload(String payload, boolean fallbackToAgencyDefault) {
        if (fallbackToAgencyDefault) {
            configurationPayloadValidator.validateOptionalJson("settingsPayloadJson", payload);
        } else {
            configurationPayloadValidator.requireJsonObjectOrArray("settingsPayloadJson", payload);
        }
    }

    private static PagedResponse<BranchPolicyResponse> page(List<BranchPolicyResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static BranchPolicyResponse toResponse(BranchPolicy policy) {
        return new BranchPolicyResponse(
                policy.getId(),
                policy.getAgencyId(),
                policy.getBranchId(),
                policy.getPolicyKey(),
                policy.getSettingsPayloadJson(),
                policy.effectiveSettingsPayloadJson(),
                policy.isFallbackToAgencyDefault(),
                policy.usesAgencyDefault(),
                policy.getStatus(),
                policy.getDisplayOrder(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo());
    }

    record ManageBranchPolicyRequest(
            UUID branchId,
            @NotBlank String policyKey,
            String settingsPayloadJson,
            boolean fallbackToAgencyDefault,
            int displayOrder,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BranchPolicyResponse(
            UUID id,
            UUID agencyId,
            UUID branchId,
            String policyKey,
            String settingsPayloadJson,
            String effectiveSettingsPayloadJson,
            boolean fallbackToAgencyDefault,
            boolean usesAgencyDefault,
            ConfigurationStatus status,
            int displayOrder,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo) {
    }
}
