package com.homehealthcare.notification.api;

import com.homehealthcare.notification.application.AdminNotificationPreferencesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/admin-notifications")
class AdminNotificationPreferencesController {

    private final AdminNotificationPreferencesService adminNotificationPreferencesService;

    AdminNotificationPreferencesController(AdminNotificationPreferencesService adminNotificationPreferencesService) {
        this.adminNotificationPreferencesService = adminNotificationPreferencesService;
    }

    @GetMapping
    AdminNotificationPreferencesService.PreferenceView currentPreferences() {
        return adminNotificationPreferencesService.currentPreferences();
    }

    @PutMapping
    AdminNotificationPreferencesService.PreferenceView updatePreferences(@Valid @RequestBody UpdatePreferenceRequest request) {
        return adminNotificationPreferencesService.updatePreferences(new AdminNotificationPreferencesService.UpdatePreferenceCommand(
                request.emailEnabled(),
                request.failedLoginAlertsEnabled(),
                request.lockedAccountAlertsEnabled(),
                request.newAdminAlertsEnabled()));
    }

    record UpdatePreferenceRequest(
            Boolean emailEnabled,
            Boolean failedLoginAlertsEnabled,
            Boolean lockedAccountAlertsEnabled,
            Boolean newAdminAlertsEnabled) {
    }
}
