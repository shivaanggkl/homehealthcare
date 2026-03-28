package com.homehealthcare.mileagepay.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.configuration.foundation.ConfigurationAuditService;
import com.homehealthcare.configuration.foundation.ConfigurationEntityNotFoundException;
import com.homehealthcare.configuration.foundation.DuplicateConfigurationException;
import com.homehealthcare.configuration.foundation.Epic2ConfigurationTargetType;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mileagepay.domain.MileagePaySetting;
import com.homehealthcare.mileagepay.domain.MileagePaySettingRepository;
import com.homehealthcare.mileagepay.domain.MileageReimbursementStrategy;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MileagePaySettingService {

    private final MileagePaySettingRepository mileagePaySettingRepository;
    private final BranchRepository branchRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public MileagePaySetting create(@NotNull AgencyMembership actorMembership, @Valid ManageMileagePaySettingCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_COMPENSATION_SETTINGS,
                UnauthorizedConfigurationActorException::new);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertUnique(actorMembership.getAgencyId(), branch, null);
        MileagePaySetting saved = mileagePaySettingRepository.saveAndFlush(MileagePaySetting.create(
                actorMembership.getAgency(),
                branch,
                command.reimbursementStrategy(),
                command.mileageRate(),
                command.travelPayEnabled(),
                command.visitTypePayAdjustmentsJson(),
                command.displayOrder()));
        saved.assignEffectiveWindow(command.effectiveFrom(), command.effectiveTo());
        saved = mileagePaySettingRepository.saveAndFlush(saved);
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.MILEAGE_PAY_SETTING,
                saved.getId(),
                saved.getBranch() == null ? null : saved.getBranch().getId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public MileagePaySetting update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID settingId,
            @Valid ManageMileagePaySettingCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_COMPENSATION_SETTINGS,
                UnauthorizedConfigurationActorException::new);
        MileagePaySetting setting = mileagePaySettingRepository.findById(settingId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("MileagePaySetting", settingId));
        assertSameAgency(actorMembership.getAgencyId(), setting.getAgencyId(), "MileagePaySetting", settingId);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertUnique(actorMembership.getAgencyId(), branch, settingId);
        setting.updateSetting(
                branch,
                command.reimbursementStrategy(),
                command.mileageRate(),
                command.travelPayEnabled(),
                command.visitTypePayAdjustmentsJson(),
                command.displayOrder());
        setting.assignEffectiveWindow(command.effectiveFrom(), command.effectiveTo());
        setting.activate();
        MileagePaySetting saved = mileagePaySettingRepository.saveAndFlush(setting);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.MILEAGE_PAY_SETTING,
                saved.getId(),
                saved.getBranch() == null ? null : saved.getBranch().getId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public MileagePaySetting deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID settingId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_COMPENSATION_SETTINGS,
                UnauthorizedConfigurationActorException::new);
        MileagePaySetting setting = mileagePaySettingRepository.findById(settingId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("MileagePaySetting", settingId));
        assertSameAgency(actorMembership.getAgencyId(), setting.getAgencyId(), "MileagePaySetting", settingId);
        setting.deactivate();
        MileagePaySetting saved = mileagePaySettingRepository.saveAndFlush(setting);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.MILEAGE_PAY_SETTING,
                saved.getId(),
                saved.getBranch() == null ? null : saved.getBranch().getId(),
                metadata(saved));
        return saved;
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("Branch", branchId));
    }

    private void assertUnique(UUID agencyId, Branch branch, UUID existingId) {
        boolean duplicate;
        if (branch == null) {
            duplicate = existingId == null
                    ? mileagePaySettingRepository.existsByAgency_IdAndBranchIsNull(agencyId)
                    : mileagePaySettingRepository.existsByAgency_IdAndBranchIsNullAndIdNot(agencyId, existingId);
        } else {
            duplicate = existingId == null
                    ? mileagePaySettingRepository.existsByAgency_IdAndBranch_Id(agencyId, branch.getId())
                    : mileagePaySettingRepository.existsByAgency_IdAndBranch_IdAndIdNot(agencyId, branch.getId(), existingId);
        }
        if (duplicate) {
            throw new DuplicateConfigurationException("MileagePaySetting", agencyId, "scope", branch == null ? "agency-default" : branch.getId().toString());
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(MileagePaySetting setting) {
        return "{\"strategy\":\"" + setting.getReimbursementStrategy().name()
                + "\",\"branchOverride\":" + setting.isBranchOverride()
                + ",\"travelPayEnabled\":" + setting.isTravelPayEnabled() + "}";
    }

    public record ManageMileagePaySettingCommand(
            UUID branchId,
            @NotNull MileageReimbursementStrategy reimbursementStrategy,
            @NotNull BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson,
            int displayOrder,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo) {
    }
}
