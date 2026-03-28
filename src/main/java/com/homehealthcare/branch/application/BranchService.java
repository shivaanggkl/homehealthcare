package com.homehealthcare.branch.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.BranchAccessContext;
import com.homehealthcare.security.branch.CurrentBranchAccess;
import com.homehealthcare.security.tenant.CurrentTenant;
import com.homehealthcare.security.tenant.TenantContextException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class BranchService {

    private static final String ACTOR_TYPE_AGENCY_MEMBERSHIP = "AGENCY_MEMBERSHIP";
    private static final String ACTION_BRANCH_CREATED = "BRANCH_CREATED";
    private static final String ACTION_BRANCH_UPDATED = "BRANCH_UPDATED";
    private static final String ACTION_BRANCH_DEACTIVATED = "BRANCH_DEACTIVATED";

    private final AgencyRepository agencyRepository;
    private final BranchRepository branchRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final CurrentTenant currentTenant;
    private final CurrentBranchAccess currentBranchAccess;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional
    public Branch createBranch(@Valid CreateBranchCommand command) {
        UUID agencyId = requireAllowedAgency(command.agencyId());
        requireCreateBranchPermission();

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new AgencyNotFoundException(agencyId));

        String normalizedName = command.name().trim();
        String normalizedCode = command.code().trim().toUpperCase(Locale.ROOT);

        if (branchRepository.existsByAgency_IdAndName(agency.getId(), normalizedName)) {
            throw new DuplicateBranchNameException(normalizedName);
        }
        if (branchRepository.existsByAgency_IdAndCode(agency.getId(), normalizedCode)) {
            throw new DuplicateBranchCodeException(normalizedCode);
        }

        Branch branch = Branch.create(
                agency,
                normalizedName,
                normalizedCode,
                command.address(),
                command.timezone());
        Branch savedBranch = branchRepository.saveAndFlush(branch);
        currentTenant.get()
                .flatMap(context -> agencyMembershipRepository.findById(context.membershipId()))
                .ifPresent(actorMembership -> auditEventRepository.save(AuditEvent.createSuccess(
                        ACTOR_TYPE_AGENCY_MEMBERSHIP,
                        actorMembership.getId(),
                        actorMembership.getUser().getEmail(),
                        ACTION_BRANCH_CREATED,
                        "BRANCH",
                        savedBranch.getId(),
                        agency.getId(),
                        savedBranch.getId(),
                        "{\"name\":\"" + savedBranch.getName()
                                + "\",\"code\":\"" + savedBranch.getCode() + "\"}")));

        return savedBranch;
    }

    @Transactional(readOnly = true)
    public Branch getBranchForCurrentAgency(@NotNull UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (currentBranchAccess.get().map(access -> access.canAccessBranch(AgencyPermission.VIEW_BRANCH, branch.getId())).orElse(true)) {
            return branch;
        }

        throw new BranchNotFoundException(branchId);
    }

    @Transactional(readOnly = true)
    public List<Branch> listAccessibleBranchesForCurrentAgency() {
        UUID agencyId = currentTenant.requireAgencyId();
        return currentBranchAccess.get()
                .map(access -> listBranchesForScopedUser(agencyId, access))
                .orElseGet(() -> branchRepository.findAllByAgency_IdOrderByNameAsc(agencyId));
    }

    @Transactional(readOnly = true)
    public List<Branch> searchAccessibleBranchesForCurrentAgency(String search) {
        List<Branch> branches = listAccessibleBranchesForCurrentAgency();
        if (search == null || search.isBlank()) {
            return branches;
        }
        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        return branches.stream()
                .filter(branch -> branch.getName().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || branch.getCode().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .toList();
    }

    @Transactional
    public Branch updateBranchForCurrentAgency(@NotNull UUID branchId, @Valid UpdateBranchCommand command) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
        requireBranchEditAccess(branch.getId());

        String normalizedName = command.name().trim();
        String normalizedCode = command.code().trim().toUpperCase(Locale.ROOT);
        if (!branch.getName().equalsIgnoreCase(normalizedName)
                && branchRepository.existsByAgency_IdAndName(branch.getAgencyId(), normalizedName)) {
            throw new DuplicateBranchNameException(normalizedName);
        }
        if (!branch.getCode().equalsIgnoreCase(normalizedCode)
                && branchRepository.existsByAgency_IdAndCode(branch.getAgencyId(), normalizedCode)) {
            throw new DuplicateBranchCodeException(normalizedCode);
        }

        branch.rename(normalizedName);
        branch.updateCode(normalizedCode);
        branch.updateAddress(command.address());
        branch.updateTimezone(command.timezone());
        Branch savedBranch = branchRepository.saveAndFlush(branch);
        currentTenant.get()
                .flatMap(context -> agencyMembershipRepository.findById(context.membershipId()))
                .ifPresent(actorMembership -> auditEventRepository.save(AuditEvent.createSuccess(
                        ACTOR_TYPE_AGENCY_MEMBERSHIP,
                        actorMembership.getId(),
                        actorMembership.getUser().getEmail(),
                        ACTION_BRANCH_UPDATED,
                        "BRANCH",
                        savedBranch.getId(),
                        savedBranch.getAgencyId(),
                        savedBranch.getId(),
                        "{\"name\":\"" + savedBranch.getName()
                                + "\",\"code\":\"" + savedBranch.getCode() + "\"}")));
        return savedBranch;
    }

    @Transactional
    public Branch deactivateBranchForCurrentAgency(@NotNull UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
        requireBranchDeactivatePermission();
        requireBranchEditAccess(branch.getId());
        branch.deactivate();
        Branch savedBranch = branchRepository.saveAndFlush(branch);
        currentTenant.get()
                .flatMap(context -> agencyMembershipRepository.findById(context.membershipId()))
                .ifPresent(actorMembership -> auditEventRepository.save(AuditEvent.createSuccess(
                        ACTOR_TYPE_AGENCY_MEMBERSHIP,
                        actorMembership.getId(),
                        actorMembership.getUser().getEmail(),
                        ACTION_BRANCH_DEACTIVATED,
                        "BRANCH",
                        savedBranch.getId(),
                        savedBranch.getAgencyId(),
                        savedBranch.getId(),
                        "{\"status\":\"" + savedBranch.getStatus().name() + "\"}")));
        return savedBranch;
    }

    private UUID requireAllowedAgency(UUID requestedAgencyId) {
        return currentTenant.get()
                .map(tenantContext -> {
                    if (!tenantContext.agencyId().equals(requestedAgencyId)) {
                        throw new TenantContextException("Authenticated request tried to write outside its agency scope");
                    }
                    return tenantContext.agencyId();
                })
                .orElse(requestedAgencyId);
    }

    private List<Branch> listBranchesForScopedUser(UUID agencyId, BranchAccessContext access) {
        if (access.canAccessAllBranches(AgencyPermission.VIEW_BRANCH)) {
            return branchRepository.findAllByAgency_IdOrderByNameAsc(agencyId);
        }
        if (access.assignedBranchIds().isEmpty()) {
            return List.of();
        }
        return branchRepository.findAllByAgency_IdAndIdInOrderByNameAsc(agencyId, access.assignedBranchIds());
    }

    private void requireBranchEditAccess(UUID branchId) {
        boolean allowed = currentBranchAccess.get()
                .map(access -> access.canAccessBranch(AgencyPermission.EDIT_BRANCH, branchId))
                .orElse(true);
        if (!allowed) {
            throw new UnauthorizedBranchOperationException(branchId);
        }
    }

    private void requireCreateBranchPermission() {
        currentTenant.get()
                .flatMap(context -> agencyMembershipRepository.findById(context.membershipId()))
                .ifPresent(actorMembership -> agencyAuthorizationGuard.requirePermission(
                        actorMembership,
                        AgencyPermission.EDIT_BRANCH,
                        UnauthorizedBranchOperationException::new));
    }

    private void requireBranchDeactivatePermission() {
        currentTenant.get()
                .flatMap(context -> agencyMembershipRepository.findById(context.membershipId()))
                .ifPresent(actorMembership -> {
                    if (actorMembership.getRole() != com.homehealthcare.security.branch.AgencyRole.AGENCY_OWNER) {
                        throw new UnauthorizedBranchOperationException(actorMembership.getId());
                    }
                });
    }

    public record CreateBranchCommand(
            @NotNull UUID agencyId,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }

    public record UpdateBranchCommand(
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }
}
