package com.homehealthcare.branch.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
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

    private final AgencyRepository agencyRepository;
    private final BranchRepository branchRepository;
    private final CurrentTenant currentTenant;
    private final CurrentBranchAccess currentBranchAccess;

    @Transactional
    public Branch createBranch(@Valid CreateBranchCommand command) {
        UUID agencyId = requireAllowedAgency(command.agencyId());

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

        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public Branch getBranchForCurrentAgency(@NotNull UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (currentBranchAccess.get().map(access -> access.canAccessBranch(branch.getId())).orElse(true)) {
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

    @Transactional
    public Branch deactivateBranch(@NotNull Branch branch) {
        branch.deactivate();
        return branchRepository.save(branch);
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
        if (access.canAccessAllBranches()) {
            return branchRepository.findAllByAgency_IdOrderByNameAsc(agencyId);
        }
        if (access.assignedBranchIds().isEmpty()) {
            return List.of();
        }
        return branchRepository.findAllByAgency_IdAndIdInOrderByNameAsc(agencyId, access.assignedBranchIds());
    }

    public record CreateBranchCommand(
            @NotNull UUID agencyId,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }
}
