package com.homehealthcare.platform.email;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.email")
public class SystemEmailProperties {

    @NotBlank
    private String baseUrl = "https://app.homehealthcare.local";

    @NotBlank
    private String invitePath = "/accept-invite";

    @NotBlank
    private String passwordResetPath = "/reset-password";

    @NotBlank
    private String securityAlertPath = "/security-alert";

    @NotBlank
    private String platformName = "HomeHealthCare";

    @NotBlank
    private String supportEmail = "support@homehealthcare.local";

    @NotBlank
    private String primaryColor = "#0F4C81";

    private String logoUrl;

    @NotBlank
    private String linkSigningSecret = "change-me-before-production";

    private boolean securityAlertEnabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInvitePath() {
        return invitePath;
    }

    public void setInvitePath(String invitePath) {
        this.invitePath = invitePath;
    }

    public String getPasswordResetPath() {
        return passwordResetPath;
    }

    public void setPasswordResetPath(String passwordResetPath) {
        this.passwordResetPath = passwordResetPath;
    }

    public String getSecurityAlertPath() {
        return securityAlertPath;
    }

    public void setSecurityAlertPath(String securityAlertPath) {
        this.securityAlertPath = securityAlertPath;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLinkSigningSecret() {
        return linkSigningSecret;
    }

    public void setLinkSigningSecret(String linkSigningSecret) {
        this.linkSigningSecret = linkSigningSecret;
    }

    public boolean isSecurityAlertEnabled() {
        return securityAlertEnabled;
    }

    public void setSecurityAlertEnabled(boolean securityAlertEnabled) {
        this.securityAlertEnabled = securityAlertEnabled;
    }
}
