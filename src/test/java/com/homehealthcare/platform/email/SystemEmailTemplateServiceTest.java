package com.homehealthcare.platform.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SystemEmailTemplateServiceTest {

    @Autowired
    private SystemEmailTemplateService systemEmailTemplateService;

    @Autowired
    private SignedEmailLinkService signedEmailLinkService;

    @Test
    void invitationAndPasswordResetTemplatesRenderSignedExpiringLinks() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(2);

        TemplatedEmail inviteEmail = systemEmailTemplateService.composeInvitationEmail(
                "North Star Home Care",
                "Casey",
                "SCHEDULER_COORDINATOR",
                "invite-token-123",
                expiresAt);
        TemplatedEmail resetEmail = systemEmailTemplateService.composePasswordResetEmail(
                "Alicia",
                "reset-token-456",
                expiresAt);

        assertThat(inviteEmail.subject()).contains("North Star Home Care");
        assertThat(inviteEmail.textBody()).contains("Casey");
        assertThat(inviteEmail.htmlBody()).contains("Accept invitation");
        assertThat(resetEmail.subject()).contains("Reset your HomeHealthCare password");
        assertThat(resetEmail.textBody()).contains("Reset your password");
        assertThat(resetEmail.htmlBody()).contains("Reset password");

        assertSignedLink(inviteEmail.actionUrl(), "INVITATION", "invite-token-123");
        assertSignedLink(resetEmail.actionUrl(), "PASSWORD_RESET", "reset-token-456");
    }

    @Test
    void securityAlertTemplateExistsForConfiguredAlertUseCases() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(30);
        TemplatedEmail alertEmail = systemEmailTemplateService.composeSecurityAlertEmail(
                "Jordan",
                "New sign-in detected",
                "We noticed a new sign-in from a device we have not seen before.",
                "alert-reference-1",
                expiresAt);

        assertThat(alertEmail.subject()).contains("New sign-in detected");
        assertThat(alertEmail.textBody()).contains("Review activity");
        assertThat(alertEmail.htmlBody()).contains("Review activity");
        assertSignedLink(alertEmail.actionUrl(), "SECURITY_ALERT", "alert-reference-1");
    }

    private void assertSignedLink(String actionUrl, String purpose, String tokenOrReference) {
        URI uri = URI.create(actionUrl);
        Map<String, String> params = Arrays.stream(uri.getQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));

        OffsetDateTime expiresAt = OffsetDateTime.parse(params.get("expiresAt"));
        assertThat(params.get("token")).isEqualTo(tokenOrReference);
        assertThat(params.get("signature")).isNotBlank();
        assertThat(signedEmailLinkService.verify(purpose, tokenOrReference, expiresAt, params.get("signature"))).isTrue();
    }
}
