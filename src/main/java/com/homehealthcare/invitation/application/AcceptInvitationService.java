package com.homehealthcare.invitation.application;

import com.homehealthcare.auth.application.PasswordHistoryService;
import com.homehealthcare.auth.application.PasswordPolicy;
import com.homehealthcare.invitation.domain.UserInvitation;
import com.homehealthcare.invitation.domain.UserInvitationRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AcceptInvitationService {

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTION_INVITATION_ACCEPTED = "INVITATION_ACCEPTED";
    private static final String TARGET_TYPE_USER_INVITATION = "USER_INVITATION";

    private final UserInvitationRepository userInvitationRepository;
    private final AuditEventRepository auditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistoryService passwordHistoryService;

    @Transactional(readOnly = true)
    public InvitationDetails getInvitationDetails(@NotBlank String token) {
        UserInvitation invitation = loadPendingInvitation(token);
        return new InvitationDetails(
                invitation.getId(),
                invitation.getAgencyId(),
                invitation.getAgencyMembershipId(),
                invitation.getEmail(),
                invitation.getUser().getFirstName(),
                invitation.getUser().getLastName(),
                invitation.getUser().getPhone(),
                invitation.getAgencyMembership().getRole(),
                invitation.getExpiresAt());
    }

    @Transactional
    public AcceptedInvitationResult acceptInvitation(@NotBlank String token, @Valid AcceptInvitationCommand command) {
        UserInvitation invitation = loadPendingInvitation(token);
        User user = invitation.getUser();

        user.updateProfile(command.firstName(), command.lastName(), command.phone());
        passwordPolicy.validate(command.password(), passwordHistoryService.recentPasswordHashes(user), passwordEncoder);
        String encodedPassword = passwordEncoder.encode(command.password());
        user.activateWithCredentials(encodedPassword);
        passwordHistoryService.recordPassword(user, encodedPassword);
        invitation.accept();

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_USER,
                user.getId(),
                user.getEmail(),
                ACTION_INVITATION_ACCEPTED,
                TARGET_TYPE_USER_INVITATION,
                invitation.getId(),
                invitation.getAgencyId(),
                "{\"membershipId\":\"" + invitation.getAgencyMembershipId()
                        + "\",\"role\":\"" + invitation.getAgencyMembership().getRole().name() + "\"}"));

        return new AcceptedInvitationResult(
                invitation.getId(),
                user.getId(),
                invitation.getAgencyMembershipId(),
                invitation.getAgencyId());
    }

    private UserInvitation loadPendingInvitation(String token) {
        UserInvitation invitation = userInvitationRepository.findByToken(token.trim())
                .orElseThrow(InvitationNotFoundException::new);

        if (invitation.isExpiredAt(OffsetDateTime.now())) {
            invitation.expire();
            userInvitationRepository.save(invitation);
            throw new InvitationExpiredException();
        }

        if (!invitation.isPending()) {
            throw new InvitationAlreadyUsedException();
        }

        return invitation;
    }

    public record AcceptInvitationCommand(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            @NotBlank String password) {
    }

    public record InvitationDetails(
            java.util.UUID invitationId,
            java.util.UUID agencyId,
            java.util.UUID membershipId,
            String email,
            String firstName,
            String lastName,
            String phone,
            com.homehealthcare.security.branch.AgencyRole role,
            OffsetDateTime expiresAt) {
    }

    public record AcceptedInvitationResult(
            java.util.UUID invitationId,
            java.util.UUID userId,
            java.util.UUID membershipId,
            java.util.UUID agencyId) {
    }
}
