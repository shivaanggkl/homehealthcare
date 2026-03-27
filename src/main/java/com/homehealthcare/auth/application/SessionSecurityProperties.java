package com.homehealthcare.auth.application;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "security.session")
public class SessionSecurityProperties {

    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(30);

    @NotNull
    private Duration idleTimeout = Duration.ofMinutes(30);

    @NotNull
    private Duration absoluteSessionDuration = Duration.ofHours(12);

    @NotNull
    private Duration warningWindow = Duration.ofMinutes(2);

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Duration getAbsoluteSessionDuration() {
        return absoluteSessionDuration;
    }

    public void setAbsoluteSessionDuration(Duration absoluteSessionDuration) {
        this.absoluteSessionDuration = absoluteSessionDuration;
    }

    public Duration getWarningWindow() {
        return warningWindow;
    }

    public void setWarningWindow(Duration warningWindow) {
        this.warningWindow = warningWindow;
    }
}
