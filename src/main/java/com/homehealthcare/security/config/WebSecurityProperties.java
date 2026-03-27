package com.homehealthcare.security.config;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "security.web")
public class WebSecurityProperties {

    @NotBlank
    private String contentSecurityPolicy = "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    @NotBlank
    private String referrerPolicy = "no-referrer";

    private List<String> allowedOrigins = new ArrayList<>();

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public void setContentSecurityPolicy(String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public String getReferrerPolicy() {
        return referrerPolicy;
    }

    public void setReferrerPolicy(String referrerPolicy) {
        this.referrerPolicy = referrerPolicy;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }
}
