package com.homehealthcare.auth.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "security.login-protection")
public class LoginProtectionProperties {

    private boolean enabled = true;

    @Min(1)
    private int failureThreshold = 5;

    @NotNull
    private Duration failureWindow = Duration.ofMinutes(15);

    @NotNull
    private Duration temporaryLockoutDuration = Duration.ofMinutes(15);

    @Min(1)
    private int maxAttemptsPerIp = 20;

    @Min(1)
    private int maxAttemptsPerEmail = 10;

    @NotNull
    private Duration rateLimitWindow = Duration.ofMinutes(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Duration getFailureWindow() {
        return failureWindow;
    }

    public void setFailureWindow(Duration failureWindow) {
        this.failureWindow = failureWindow;
    }

    public Duration getTemporaryLockoutDuration() {
        return temporaryLockoutDuration;
    }

    public void setTemporaryLockoutDuration(Duration temporaryLockoutDuration) {
        this.temporaryLockoutDuration = temporaryLockoutDuration;
    }

    public int getMaxAttemptsPerIp() {
        return maxAttemptsPerIp;
    }

    public void setMaxAttemptsPerIp(int maxAttemptsPerIp) {
        this.maxAttemptsPerIp = maxAttemptsPerIp;
    }

    public int getMaxAttemptsPerEmail() {
        return maxAttemptsPerEmail;
    }

    public void setMaxAttemptsPerEmail(int maxAttemptsPerEmail) {
        this.maxAttemptsPerEmail = maxAttemptsPerEmail;
    }

    public Duration getRateLimitWindow() {
        return rateLimitWindow;
    }

    public void setRateLimitWindow(Duration rateLimitWindow) {
        this.rateLimitWindow = rateLimitWindow;
    }
}
