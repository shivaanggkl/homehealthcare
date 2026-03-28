package com.homehealthcare.mileagepay.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.ConfigurationPayloadValidator;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.mileagepay.application.MileagePaySettingService;
import com.homehealthcare.mileagepay.domain.MileagePaySetting;
import com.homehealthcare.mileagepay.domain.MileagePaySettingRepository;
import com.homehealthcare.mileagepay.domain.MileageReimbursementStrategy;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mileage-pay-settings")
class MileagePaySettingController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationPayloadValidator configurationPayloadValidator;
    private final MileagePaySettingRepository mileagePaySettingRepository;
    private final MileagePaySettingService mileagePaySettingService;

    MileagePaySettingController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            ConfigurationPayloadValidator configurationPayloadValidator,
            MileagePaySettingRepository mileagePaySettingRepository,
            MileagePaySettingService mileagePaySettingService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.configurationPayloadValidator = configurationPayloadValidator;
        this.mileagePaySettingRepository = mileagePaySettingRepository;
        this.mileagePaySettingService = mileagePaySettingService;
    }

    @GetMapping
    MileagePaySettingsResponse settings(@RequestParam(name = "effectiveAt", required = false) OffsetDateTime effectiveAt) {
        var actor = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actor,
                AgencyPermission.VIEW_COMPENSATION_SETTINGS,
                UnauthorizedConfigurationActorException::new);
        OffsetDateTime probe = effectiveAt == null ? OffsetDateTime.now() : effectiveAt;
        MileagePaySettingScopeResponse agencyDefault = mileagePaySettingRepository.findByAgency_IdAndBranchIsNull(actor.getAgencyId())
                .map(item -> toScopeResponse(item, probe))
                .orElse(null);
        List<MileagePaySettingScopeResponse> branchOverrides = mileagePaySettingRepository.findAllByAgency_Id(actor.getAgencyId())
                .stream()
                .filter(MileagePaySetting::isBranchOverride)
                .sorted(Comparator.comparing(item -> item.getBranch().getName()))
                .map(item -> toScopeResponse(item, probe))
                .toList();
        return new MileagePaySettingsResponse(actor.getAgencyId(), probe, agencyDefault, branchOverrides);
    }

    @PutMapping("/default")
    MileagePaySettingScopeResponse updateAgencyDefault(@Valid @RequestBody ManageMileagePaySettingRequest request) {
        if (request.branchId() != null) {
            throw new IllegalArgumentException("branchId must be omitted for the agency default mileage/pay setting");
        }
        configurationPayloadValidator.validateOptionalJson("visitTypePayAdjustmentsJson", request.visitTypePayAdjustmentsJson());
        var actor = configurationActorResolver.requireActorMembership();
        MileagePaySetting saved = mileagePaySettingRepository.findByAgency_IdAndBranchIsNull(actor.getAgencyId())
                .map(existing -> mileagePaySettingService.update(actor, existing.getId(), toCommand(request)))
                .orElseGet(() -> mileagePaySettingService.create(actor, toCommand(request)));
        return toScopeResponse(saved, OffsetDateTime.now());
    }

    @PutMapping("/branches/{branchId}")
    MileagePaySettingScopeResponse updateBranchOverride(
            @PathVariable UUID branchId,
            @Valid @RequestBody ManageMileagePaySettingRequest request) {
        if (request.branchId() != null && !branchId.equals(request.branchId())) {
            throw new IllegalArgumentException("branchId in path and body must match");
        }
        configurationPayloadValidator.validateOptionalJson("visitTypePayAdjustmentsJson", request.visitTypePayAdjustmentsJson());
        var actor = configurationActorResolver.requireActorMembership();
        ManageMileagePaySettingRequest normalizedRequest = new ManageMileagePaySettingRequest(
                branchId,
                request.reimbursementStrategy(),
                request.mileageRate(),
                request.travelPayEnabled(),
                request.visitTypePayAdjustmentsJson(),
                request.displayOrder(),
                request.effectiveFrom(),
                request.effectiveTo());
        MileagePaySetting saved = mileagePaySettingRepository.findByAgency_IdAndBranch_Id(actor.getAgencyId(), branchId)
                .map(existing -> mileagePaySettingService.update(actor, existing.getId(), toCommand(normalizedRequest)))
                .orElseGet(() -> mileagePaySettingService.create(actor, toCommand(normalizedRequest)));
        return toScopeResponse(saved, OffsetDateTime.now());
    }

    private static MileagePaySettingService.ManageMileagePaySettingCommand toCommand(ManageMileagePaySettingRequest request) {
        return new MileagePaySettingService.ManageMileagePaySettingCommand(
                request.branchId(),
                request.reimbursementStrategy(),
                request.mileageRate(),
                request.travelPayEnabled(),
                request.visitTypePayAdjustmentsJson(),
                request.displayOrder(),
                request.effectiveFrom(),
                request.effectiveTo());
    }

    private static MileagePaySettingScopeResponse toScopeResponse(MileagePaySetting setting, OffsetDateTime effectiveAt) {
        return new MileagePaySettingScopeResponse(
                setting.getId(),
                setting.getAgencyId(),
                setting.getBranch() == null ? null : setting.getBranch().getId(),
                setting.getReimbursementStrategy(),
                setting.getMileageRate(),
                setting.isTravelPayEnabled(),
                setting.getVisitTypePayAdjustmentsJson(),
                setting.getStatus(),
                setting.getEffectiveFrom(),
                setting.getEffectiveTo(),
                setting.isEffectiveAt(effectiveAt),
                setting.isBranchOverride());
    }

    record ManageMileagePaySettingRequest(
            UUID branchId,
            @NotNull MileageReimbursementStrategy reimbursementStrategy,
            @NotNull BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson,
            int displayOrder,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo) {
    }

    record MileagePaySettingsResponse(
            UUID agencyId,
            OffsetDateTime effectiveAt,
            MileagePaySettingScopeResponse agencyDefault,
            List<MileagePaySettingScopeResponse> branchOverrides) {
    }

    record MileagePaySettingScopeResponse(
            UUID id,
            UUID agencyId,
            UUID branchId,
            MileageReimbursementStrategy reimbursementStrategy,
            BigDecimal mileageRate,
            boolean travelPayEnabled,
            String visitTypePayAdjustmentsJson,
            com.homehealthcare.configuration.foundation.ConfigurationStatus status,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo,
            boolean effectiveAtRequestedTime,
            boolean branchOverride) {
    }
}
