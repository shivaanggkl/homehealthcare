package com.homehealthcare.auth.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "security.password-policy")
public class PasswordPolicyProperties {

    @Min(8)
    private int minimumLength = 12;

    @NotNull
    private boolean requireUppercase = true;

    @NotNull
    private boolean requireLowercase = true;

    @NotNull
    private boolean requireDigit = true;

    @NotNull
    private boolean requireSymbol = true;

    @NotNull
    private boolean commonPasswordCheckEnabled = true;

    @Min(0)
    private int preventReuseCount = 5;

    public int getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public boolean isRequireSymbol() {
        return requireSymbol;
    }

    public void setRequireSymbol(boolean requireSymbol) {
        this.requireSymbol = requireSymbol;
    }

    public boolean isCommonPasswordCheckEnabled() {
        return commonPasswordCheckEnabled;
    }

    public void setCommonPasswordCheckEnabled(boolean commonPasswordCheckEnabled) {
        this.commonPasswordCheckEnabled = commonPasswordCheckEnabled;
    }

    public int getPreventReuseCount() {
        return preventReuseCount;
    }

    public void setPreventReuseCount(int preventReuseCount) {
        this.preventReuseCount = preventReuseCount;
    }
}
