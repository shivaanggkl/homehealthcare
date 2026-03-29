package com.homehealthcare.user.api;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.tenant.CurrentTenant;
import com.homehealthcare.user.application.UserSelfProfileService;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile")
class UserSelfProfileController {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final UserRepository userRepository;
    private final UserSelfProfileService userSelfProfileService;

    UserSelfProfileController(
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            UserRepository userRepository,
            UserSelfProfileService userSelfProfileService) {
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.userRepository = userRepository;
        this.userSelfProfileService = userSelfProfileService;
    }

    @GetMapping
    SelfProfileResponse profile() {
        User user = currentUser();
        return new SelfProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getPreferredLanguage(),
                user.getTimeZone());
    }

    @PutMapping
    SelfProfileResponse update(@Valid @RequestBody UpdateSelfProfileRequest request) {
        User user = currentUser();
        UserSelfProfileService.UpdatedSelfProfileResult updated = userSelfProfileService.updateOwnProfile(
                user,
                new UserSelfProfileService.UpdateSelfProfileCommand(
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.preferredLanguage(),
                        request.timeZone()));
        return new SelfProfileResponse(
                updated.userId(),
                updated.firstName(),
                updated.lastName(),
                user.getEmail(),
                updated.phone(),
                updated.preferredLanguage(),
                updated.timeZone());
    }

    private User currentUser() {
        AgencyMembership membership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
        return userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new IllegalStateException("Current user was not found"));
    }

    record UpdateSelfProfileRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            String preferredLanguage,
            String timeZone) {
    }

    record SelfProfileResponse(
            java.util.UUID userId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String preferredLanguage,
            String timeZone) {
    }
}
