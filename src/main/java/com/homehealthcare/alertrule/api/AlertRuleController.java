package com.homehealthcare.alertrule.api;

import com.homehealthcare.alertrule.application.AlertRuleCatalogService;
import com.homehealthcare.alertrule.domain.AlertRule;
import com.homehealthcare.alertrule.domain.AlertRuleRepository;
import com.homehealthcare.alertrule.domain.AlertRuleType;
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
import java.util.List;
import java.util.Locale;
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
@RequestMapping("/api/alert-rules")
class AlertRuleController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationPayloadValidator configurationPayloadValidator;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRuleCatalogService alertRuleCatalogService;

    AlertRuleController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            ConfigurationPayloadValidator configurationPayloadValidator,
            AlertRuleRepository alertRuleRepository,
            AlertRuleCatalogService alertRuleCatalogService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.configurationPayloadValidator = configurationPayloadValidator;
        this.alertRuleRepository = alertRuleRepository;
        this.alertRuleCatalogService = alertRuleCatalogService;
    }

    @GetMapping
    PagedResponse<AlertRuleResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "ruleType", required = false) AlertRuleType ruleType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actor = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actor,
                AgencyPermission.VIEW_ALERT_RULE,
                UnauthorizedConfigurationActorException::new);
        List<AlertRuleResponse> filtered = alertRuleRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(actor.getAgencyId())
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> branchId == null || (item.getBranch() != null && branchId.equals(item.getBranch().getId())))
                .filter(item -> ruleType == null || item.getRuleType() == ruleType)
                .filter(item -> matchesSearch(search, item.getName()))
                .map(AlertRuleController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    AlertRuleResponse create(@Valid @RequestBody ManageAlertRuleRequest request) {
        configurationPayloadValidator.requireJsonObjectOrArray("configPayloadJson", request.configPayloadJson());
        return toResponse(alertRuleCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new AlertRuleCatalogService.ManageAlertRuleCommand(
                        request.branchId(),
                        request.name(),
                        request.ruleType(),
                        request.configPayloadJson(),
                        request.notifyEmail(),
                        request.notifySms(),
                        request.notifyInApp(),
                        request.displayOrder())));
    }

    @PutMapping("/{alertRuleId}")
    AlertRuleResponse update(@PathVariable UUID alertRuleId, @Valid @RequestBody ManageAlertRuleRequest request) {
        configurationPayloadValidator.requireJsonObjectOrArray("configPayloadJson", request.configPayloadJson());
        return toResponse(alertRuleCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                alertRuleId,
                new AlertRuleCatalogService.ManageAlertRuleCommand(
                        request.branchId(),
                        request.name(),
                        request.ruleType(),
                        request.configPayloadJson(),
                        request.notifyEmail(),
                        request.notifySms(),
                        request.notifyInApp(),
                        request.displayOrder())));
    }

    @DeleteMapping("/{alertRuleId}")
    AlertRuleResponse deactivate(@PathVariable UUID alertRuleId) {
        return toResponse(alertRuleCatalogService.deactivate(configurationActorResolver.requireActorMembership(), alertRuleId));
    }

    private static boolean matchesSearch(String search, String name) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return name.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private static PagedResponse<AlertRuleResponse> page(List<AlertRuleResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static AlertRuleResponse toResponse(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getAgencyId(),
                rule.getBranch() == null ? null : rule.getBranch().getId(),
                rule.getName(),
                rule.getRuleType(),
                rule.getConfigPayloadJson(),
                rule.isNotifyEmail(),
                rule.isNotifySms(),
                rule.isNotifyInApp(),
                rule.getStatus(),
                rule.getDisplayOrder(),
                rule.isBranchSpecific());
    }

    record ManageAlertRuleRequest(
            UUID branchId,
            @NotBlank String name,
            AlertRuleType ruleType,
            @NotBlank String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp,
            int displayOrder) {
    }

    record AlertRuleResponse(
            UUID id,
            UUID agencyId,
            UUID branchId,
            String name,
            AlertRuleType ruleType,
            String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp,
            ConfigurationStatus status,
            int displayOrder,
            boolean branchSpecific) {
    }
}
