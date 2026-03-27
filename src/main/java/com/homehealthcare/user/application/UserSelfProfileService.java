package com.homehealthcare.user.application;

import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserSelfProfileService {

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTION_USER_SELF_PROFILE_UPDATED = "USER_SELF_PROFILE_UPDATED";
    private static final String TARGET_TYPE_USER = "USER";

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public UpdatedSelfProfileResult updateOwnProfile(@NotNull User actor, @Valid UpdateSelfProfileCommand command) {
        actor.updateSelfProfile(
                command.firstName(),
                command.lastName(),
                command.phone(),
                command.preferredLanguage(),
                command.timeZone());

        User savedUser = userRepository.save(actor);

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_USER,
                savedUser.getId(),
                savedUser.getEmail(),
                ACTION_USER_SELF_PROFILE_UPDATED,
                TARGET_TYPE_USER,
                savedUser.getId(),
                null,
                "{\"preferredLanguage\":\"" + nullSafe(savedUser.getPreferredLanguage())
                        + "\",\"timeZone\":\"" + nullSafe(savedUser.getTimeZone()) + "\"}"));

        return new UpdatedSelfProfileResult(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhone(),
                savedUser.getPreferredLanguage(),
                savedUser.getTimeZone());
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record UpdateSelfProfileCommand(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            String preferredLanguage,
            String timeZone) {
    }

    public record UpdatedSelfProfileResult(
            UUID userId,
            String firstName,
            String lastName,
            String phone,
            String preferredLanguage,
            String timeZone) {
    }
}
