package com.homehealthcare.branch.api;

import com.homehealthcare.branch.application.BranchService;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/branches")
class BranchManagementController {

    private final BranchService branchService;
    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    BranchManagementController(
            BranchService branchService,
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.branchService = branchService;
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    List<BranchResponse> branches(@RequestParam(name = "search", required = false) String search) {
        requireBranchManagementAccess();
        return branchService.searchAccessibleBranchesForCurrentAgency(search).stream()
                .map(BranchManagementController::toResponse)
                .toList();
    }

    @PostMapping
    BranchResponse create(@Valid @RequestBody CreateBranchRequest request) {
        requireBranchManagementAccess();
        return toResponse(branchService.createBranch(new BranchService.CreateBranchCommand(
                request.agencyId(),
                request.name(),
                request.code(),
                request.address(),
                request.timezone())));
    }

    @PutMapping("/{branchId}")
    BranchResponse update(@PathVariable("branchId") UUID branchId, @Valid @RequestBody UpdateBranchRequest request) {
        requireBranchManagementAccess();
        return toResponse(branchService.updateBranchForCurrentAgency(
                branchId,
                new BranchService.UpdateBranchCommand(
                        request.name(),
                        request.code(),
                        request.address(),
                        request.timezone())));
    }

    @DeleteMapping("/{branchId}")
    BranchResponse deactivate(@PathVariable("branchId") UUID branchId) {
        requireBranchManagementAccess();
        return toResponse(branchService.deactivateBranchForCurrentAgency(branchId));
    }

    private void requireBranchManagementAccess() {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.EDIT_BRANCH,
                com.homehealthcare.branch.application.UnauthorizedBranchOperationException::new);
    }

    private static BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getAgencyId(),
                branch.getName(),
                branch.getCode(),
                branch.getAddress(),
                branch.getTimezone(),
                branch.getStatus());
    }

    record CreateBranchRequest(
            UUID agencyId,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }

    record UpdateBranchRequest(
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }

    record BranchResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            String address,
            String timezone,
            com.homehealthcare.branch.domain.BranchStatus status) {
    }
}
