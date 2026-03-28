package com.homehealthcare.security.api;

import com.homehealthcare.agency.application.AgencyMfaPolicyService;
import com.homehealthcare.auth.application.PasswordPolicy;
import com.homehealthcare.auth.application.SessionSecurityProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/settings")
class SecuritySettingsController {

    private final AgencyMfaPolicyService agencyMfaPolicyService;
    private final PasswordPolicy passwordPolicy;
    private final SessionSecurityProperties sessionSecurityProperties;

    SecuritySettingsController(
            AgencyMfaPolicyService agencyMfaPolicyService,
            PasswordPolicy passwordPolicy,
            SessionSecurityProperties sessionSecurityProperties) {
        this.agencyMfaPolicyService = agencyMfaPolicyService;
        this.passwordPolicy = passwordPolicy;
        this.sessionSecurityProperties = sessionSecurityProperties;
    }

    @GetMapping
    SecuritySettingsResponse settings() {
        AgencyMfaPolicyService.AgencyMfaPolicyView mfaPolicy = agencyMfaPolicyService.currentPolicy();
        PasswordPolicy.PasswordPolicyView passwordPolicyView = passwordPolicy.describe();
        return new SecuritySettingsResponse(
                new MfaPolicyResponse(
                        mfaPolicy.agencyId(),
                        mfaPolicy.mode(),
                        List.copyOf(mfaPolicy.requiredRoles())),
                new PasswordPolicyResponse(
                        passwordPolicyView.minimumLength(),
                        passwordPolicyView.requireUppercase(),
                        passwordPolicyView.requireLowercase(),
                        passwordPolicyView.requireDigit(),
                        passwordPolicyView.requireSymbol(),
                        passwordPolicyView.commonPasswordCheckEnabled(),
                        passwordPolicyView.preventReuseCount(),
                        passwordPolicyView.summary()),
                new SessionPolicyResponse(
                        seconds(sessionSecurityProperties.getIdleTimeout()),
                        seconds(sessionSecurityProperties.getAbsoluteSessionDuration()),
                        seconds(sessionSecurityProperties.getWarningWindow()),
                        false,
                        "Epic 1 exposes session policy as runtime configuration only; owner edits are not supported by public API."));
    }

    private static long seconds(Duration duration) {
        return duration.getSeconds();
    }

    record SecuritySettingsResponse(
            MfaPolicyResponse mfaPolicy,
            PasswordPolicyResponse passwordPolicy,
            SessionPolicyResponse sessionPolicy) {
    }

    record MfaPolicyResponse(
            java.util.UUID agencyId,
            com.homehealthcare.agency.domain.AgencyMfaPolicyMode mode,
            List<com.homehealthcare.security.branch.AgencyRole> requiredRoles) {
    }

    record PasswordPolicyResponse(
            int minimumLength,
            boolean requireUppercase,
            boolean requireLowercase,
            boolean requireDigit,
            boolean requireSymbol,
            boolean commonPasswordCheckEnabled,
            int preventReuseCount,
            String summary) {
    }

    record SessionPolicyResponse(
            long idleTimeoutSeconds,
            long absoluteSessionDurationSeconds,
            long warningWindowSeconds,
            boolean editable,
            String scopeBoundary) {
    }
}
