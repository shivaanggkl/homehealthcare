package com.homehealthcare.alertrule.application;

import com.homehealthcare.alertrule.domain.AlertRule;
import com.homehealthcare.alertrule.domain.AlertRuleRepository;
import com.homehealthcare.alertrule.domain.AlertRuleType;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AlertRuleCatalogService {

    private final AlertRuleRepository alertRuleRepository;
    private final BranchRepository branchRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationAuditService configurationAuditService;

    @Transactional
    public AlertRule create(@NotNull AgencyMembership actorMembership, @Valid ManageAlertRuleCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_ALERT_RULE,
                UnauthorizedConfigurationActorException::new);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertUnique(actorMembership.getAgencyId(), branch, command.name(), null);

        AlertRule saved = alertRuleRepository.saveAndFlush(AlertRule.create(
                actorMembership.getAgency(),
                branch,
                command.name(),
                command.ruleType(),
                command.configPayloadJson(),
                command.notifyEmail(),
                command.notifySms(),
                command.notifyInApp(),
                command.displayOrder()));
        configurationAuditService.recordCreated(
                actorMembership,
                Epic2ConfigurationTargetType.ALERT_RULE,
                saved.getId(),
                saved.getBranch() == null ? null : saved.getBranch().getId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public AlertRule update(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID alertRuleId,
            @Valid ManageAlertRuleCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_ALERT_RULE,
                UnauthorizedConfigurationActorException::new);
        AlertRule rule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("AlertRule", alertRuleId));
        assertSameAgency(actorMembership.getAgencyId(), rule.getAgencyId(), "AlertRule", alertRuleId);
        Branch branch = resolveBranch(actorMembership.getAgencyId(), command.branchId());
        assertUnique(actorMembership.getAgencyId(), branch, command.name(), alertRuleId);

        rule.updateRule(
                branch,
                command.name(),
                command.ruleType(),
                command.configPayloadJson(),
                command.notifyEmail(),
                command.notifySms(),
                command.notifyInApp(),
                command.displayOrder());
        rule.activate();
        AlertRule saved = alertRuleRepository.saveAndFlush(rule);
        configurationAuditService.recordUpdated(
                actorMembership,
                Epic2ConfigurationTargetType.ALERT_RULE,
                saved.getId(),
                saved.getBranch() == null ? null : saved.getBranch().getId(),
                metadata(saved));
        return saved;
    }

    @Transactional
    public AlertRule deactivate(@NotNull AgencyMembership actorMembership, @NotNull UUID alertRuleId) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_ALERT_RULE,
                UnauthorizedConfigurationActorException::new);
        AlertRule rule = alertRuleRepository.findById(alertRuleId)
                .orElseThrow(() -> new ConfigurationEntityNotFoundException("AlertRule", alertRuleId));
        assertSameAgency(actorMembership.getAgencyId(), rule.getAgencyId(), "AlertRule", alertRuleId);
        rule.deactivate();
        AlertRule saved = alertRuleRepository.saveAndFlush(rule);
        configurationAuditService.recordDeactivated(
                actorMembership,
                Epic2ConfigurationTargetType.ALERT_RULE,
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

    private void assertUnique(UUID agencyId, Branch branch, String name, UUID existingId) {
        boolean duplicate;
        if (branch == null) {
            duplicate = existingId == null
                    ? alertRuleRepository.existsByAgency_IdAndBranchIsNullAndName(agencyId, name.trim())
                    : alertRuleRepository.existsByAgency_IdAndBranchIsNullAndNameAndIdNot(agencyId, name.trim(), existingId);
        } else {
            duplicate = existingId == null
                    ? alertRuleRepository.existsByAgency_IdAndBranch_IdAndName(agencyId, branch.getId(), name.trim())
                    : alertRuleRepository.existsByAgency_IdAndBranch_IdAndNameAndIdNot(agencyId, branch.getId(), name.trim(), existingId);
        }
        if (duplicate) {
            throw new DuplicateConfigurationException("AlertRule", agencyId, "name", name);
        }
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new ConfigurationEntityNotFoundException(entityType, entityId);
        }
    }

    private static String metadata(AlertRule rule) {
        return "{\"name\":\"" + rule.getName() + "\",\"ruleType\":\"" + rule.getRuleType().name()
                + "\",\"branchSpecific\":" + rule.isBranchSpecific() + "}";
    }

    public record ManageAlertRuleCommand(
            UUID branchId,
            @NotBlank String name,
            @NotNull AlertRuleType ruleType,
            @NotBlank String configPayloadJson,
            boolean notifyEmail,
            boolean notifySms,
            boolean notifyInApp,
            int displayOrder) {
    }
}
