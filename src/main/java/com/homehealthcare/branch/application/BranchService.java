package com.homehealthcare.branch.application;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @Transactional
    public Branch createBranch(@Valid CreateBranchCommand command) {
        Agency agency = agencyRepository.findById(command.agencyId())
                .orElseThrow(() -> new AgencyNotFoundException(command.agencyId()));

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

    @Transactional
    public Branch deactivateBranch(@NotNull Branch branch) {
        branch.deactivate();
        return branchRepository.save(branch);
    }

    public record CreateBranchCommand(
            @NotNull UUID agencyId,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String address,
            @NotBlank String timezone) {
    }
}
