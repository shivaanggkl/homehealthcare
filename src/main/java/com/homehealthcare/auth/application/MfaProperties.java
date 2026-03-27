package com.homehealthcare.auth.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "security.mfa")
public class MfaProperties {

    @NotBlank
    private String issuer = "HomeHealthCare";

    @NotNull
    private Duration enrollmentChallengeTtl = Duration.ofMinutes(5);

    @Min(1)
    private int recoveryCodeCount = 8;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getEnrollmentChallengeTtl() {
        return enrollmentChallengeTtl;
    }

    public void setEnrollmentChallengeTtl(Duration enrollmentChallengeTtl) {
        this.enrollmentChallengeTtl = enrollmentChallengeTtl;
    }

    public int getRecoveryCodeCount() {
        return recoveryCodeCount;
    }

    public void setRecoveryCodeCount(int recoveryCodeCount) {
        this.recoveryCodeCount = recoveryCodeCount;
    }
}
