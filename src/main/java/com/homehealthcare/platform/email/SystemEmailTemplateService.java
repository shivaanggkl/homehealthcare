package com.homehealthcare.platform.email;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemEmailTemplateService {

    private final SystemEmailProperties properties;
    private final SignedEmailLinkService signedEmailLinkService;
    private final SystemEmailTemplateRenderer renderer;

    public TemplatedEmail composeInvitationEmail(
            String agencyName,
            String recipientName,
            String roleName,
            String token,
            OffsetDateTime expiresAt) {
        EmailBranding branding = EmailBranding.defaultBranding(properties, agencyName);
        String actionUrl = signedEmailLinkService.createInvitationLink(token, expiresAt);
        String subject = branding.agencyName() + " invited you to " + branding.platformName();
        Map<String, String> variables = commonVariables(branding, recipientName, actionUrl, expiresAt);
        variables.put("roleName", roleName);
        return new TemplatedEmail(
                subject,
                renderer.render("emails/invitation.txt", variables),
                renderer.render("emails/invitation.html", variables),
                actionUrl,
                expiresAt,
                branding);
    }

    public TemplatedEmail composePasswordResetEmail(
            String recipientName,
            String token,
            OffsetDateTime expiresAt) {
        EmailBranding branding = EmailBranding.defaultBranding(properties, null);
        String actionUrl = signedEmailLinkService.createPasswordResetLink(token, expiresAt);
        String subject = "Reset your " + branding.platformName() + " password";
        Map<String, String> variables = commonVariables(branding, recipientName, actionUrl, expiresAt);
        return new TemplatedEmail(
                subject,
                renderer.render("emails/password-reset.txt", variables),
                renderer.render("emails/password-reset.html", variables),
                actionUrl,
                expiresAt,
                branding);
    }

    public TemplatedEmail composeSecurityAlertEmail(
            String recipientName,
            String alertTitle,
            String alertBody,
            String reference,
            OffsetDateTime expiresAt) {
        EmailBranding branding = EmailBranding.defaultBranding(properties, null);
        String actionUrl = signedEmailLinkService.createSecurityAlertLink(reference, expiresAt);
        Map<String, String> variables = commonVariables(branding, recipientName, actionUrl, expiresAt);
        variables.put("alertTitle", alertTitle);
        variables.put("alertBody", alertBody);
        return new TemplatedEmail(
                alertTitle + " | " + branding.platformName(),
                renderer.render("emails/security-alert.txt", variables),
                renderer.render("emails/security-alert.html", variables),
                actionUrl,
                expiresAt,
                branding);
    }

    private Map<String, String> commonVariables(
            EmailBranding branding,
            String recipientName,
            String actionUrl,
            OffsetDateTime expiresAt) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("platformName", branding.platformName());
        variables.put("agencyName", branding.agencyName() == null ? branding.platformName() : branding.agencyName());
        variables.put("supportEmail", branding.supportEmail());
        variables.put("primaryColor", branding.primaryColor());
        variables.put("logoUrl", branding.logoUrl() == null ? "" : branding.logoUrl());
        variables.put("recipientName", recipientName == null || recipientName.isBlank() ? "there" : recipientName);
        variables.put("actionUrl", actionUrl);
        variables.put("expiresAt", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiresAt));
        return variables;
    }
}
