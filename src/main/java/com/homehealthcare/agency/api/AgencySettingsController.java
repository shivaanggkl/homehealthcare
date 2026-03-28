package com.homehealthcare.agency.api;

import com.homehealthcare.agency.application.AgencySettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agency/settings")
class AgencySettingsController {

    private final AgencySettingsService agencySettingsService;

    AgencySettingsController(AgencySettingsService agencySettingsService) {
        this.agencySettingsService = agencySettingsService;
    }

    @GetMapping
    AgencySettingsService.AgencySettingsView settings() {
        return agencySettingsService.getSettings();
    }

    @PutMapping
    AgencySettingsService.AgencySettingsView update(@Valid @RequestBody UpdateAgencySettingsRequest request) {
        return agencySettingsService.updateSettings(new AgencySettingsService.UpdateAgencySettingsCommand(
                request.name(),
                request.timezone(),
                request.contactEmail()));
    }

    record UpdateAgencySettingsRequest(
            @NotBlank String name,
            @NotBlank String timezone,
            @NotBlank @Email String contactEmail) {
    }
}
