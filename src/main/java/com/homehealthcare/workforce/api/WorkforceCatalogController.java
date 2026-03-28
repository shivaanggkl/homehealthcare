package com.homehealthcare.workforce.api;

import com.homehealthcare.caregiverskill.application.CaregiverSkillCatalogService;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.certification.application.CaregiverCertificationCatalogService;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
class WorkforceCatalogController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final CaregiverSkillRepository caregiverSkillRepository;
    private final CaregiverCertificationRepository caregiverCertificationRepository;
    private final CaregiverSkillCatalogService caregiverSkillCatalogService;
    private final CaregiverCertificationCatalogService caregiverCertificationCatalogService;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    WorkforceCatalogController(
            ConfigurationActorResolver configurationActorResolver,
            CaregiverSkillRepository caregiverSkillRepository,
            CaregiverCertificationRepository caregiverCertificationRepository,
            CaregiverSkillCatalogService caregiverSkillCatalogService,
            CaregiverCertificationCatalogService caregiverCertificationCatalogService,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.configurationActorResolver = configurationActorResolver;
        this.caregiverSkillRepository = caregiverSkillRepository;
        this.caregiverCertificationRepository = caregiverCertificationRepository;
        this.caregiverSkillCatalogService = caregiverSkillCatalogService;
        this.caregiverCertificationCatalogService = caregiverCertificationCatalogService;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping("/api/caregiver-skills")
    PagedResponse<CaregiverSkillResponse> skills(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        UUID agencyId = configurationActorResolver.requireActorMembership().getAgencyId();
        agencyAuthorizationGuard.requirePermission(
                configurationActorResolver.requireActorMembership(),
                AgencyPermission.VIEW_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        List<CaregiverSkillResponse> filtered = caregiverSkillRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agencyId)
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> matchesSearch(item.getName(), item.getCode(), search))
                .map(WorkforceCatalogController::toSkillResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping("/api/caregiver-skills")
    CaregiverSkillResponse createSkill(@Valid @RequestBody ManageCaregiverSkillRequest request) {
        return toSkillResponse(caregiverSkillCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new CaregiverSkillCatalogService.ManageCaregiverSkillCommand(
                        request.name(),
                        request.code(),
                        request.description())));
    }

    @PutMapping("/api/caregiver-skills/{skillId}")
    CaregiverSkillResponse updateSkill(@PathVariable UUID skillId, @Valid @RequestBody ManageCaregiverSkillRequest request) {
        return toSkillResponse(caregiverSkillCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                skillId,
                new CaregiverSkillCatalogService.ManageCaregiverSkillCommand(
                        request.name(),
                        request.code(),
                        request.description())));
    }

    @DeleteMapping("/api/caregiver-skills/{skillId}")
    CaregiverSkillResponse deactivateSkill(@PathVariable UUID skillId) {
        return toSkillResponse(caregiverSkillCatalogService.deactivate(
                configurationActorResolver.requireActorMembership(),
                skillId));
    }

    @GetMapping("/api/caregiver-certifications")
    PagedResponse<CaregiverCertificationResponse> certifications(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        UUID agencyId = configurationActorResolver.requireActorMembership().getAgencyId();
        agencyAuthorizationGuard.requirePermission(
                configurationActorResolver.requireActorMembership(),
                AgencyPermission.VIEW_WORKFORCE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        List<CaregiverCertificationResponse> filtered = caregiverCertificationRepository
                .findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agencyId)
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> matchesSearch(item.getName(), item.getCode(), search))
                .map(WorkforceCatalogController::toCertificationResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping("/api/caregiver-certifications")
    CaregiverCertificationResponse createCertification(@Valid @RequestBody ManageCaregiverCertificationRequest request) {
        return toCertificationResponse(caregiverCertificationCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new CaregiverCertificationCatalogService.ManageCaregiverCertificationCommand(
                        request.name(),
                        request.code(),
                        request.description(),
                        request.expirationRequired())));
    }

    @PutMapping("/api/caregiver-certifications/{certificationId}")
    CaregiverCertificationResponse updateCertification(
            @PathVariable UUID certificationId,
            @Valid @RequestBody ManageCaregiverCertificationRequest request) {
        return toCertificationResponse(caregiverCertificationCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                certificationId,
                new CaregiverCertificationCatalogService.ManageCaregiverCertificationCommand(
                        request.name(),
                        request.code(),
                        request.description(),
                        request.expirationRequired())));
    }

    @DeleteMapping("/api/caregiver-certifications/{certificationId}")
    CaregiverCertificationResponse deactivateCertification(@PathVariable UUID certificationId) {
        return toCertificationResponse(caregiverCertificationCatalogService.deactivate(
                configurationActorResolver.requireActorMembership(),
                certificationId));
    }

    private static boolean matchesSearch(String name, String code, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalized)
                || code.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static CaregiverSkillResponse toSkillResponse(CaregiverSkill skill) {
        return new CaregiverSkillResponse(
                skill.getId(),
                skill.getAgencyId(),
                skill.getName(),
                skill.getCode(),
                skill.getDescription(),
                skill.getStatus());
    }

    private static CaregiverCertificationResponse toCertificationResponse(CaregiverCertification certification) {
        return new CaregiverCertificationResponse(
                certification.getId(),
                certification.getAgencyId(),
                certification.getName(),
                certification.getCode(),
                certification.getDescription(),
                certification.isExpirationRequired(),
                certification.getStatus());
    }

    record ManageCaregiverSkillRequest(
            @NotBlank String name,
            @NotBlank String code,
            String description) {
    }

    record ManageCaregiverCertificationRequest(
            @NotBlank String name,
            @NotBlank String code,
            String description,
            boolean expirationRequired) {
    }

    record CaregiverSkillResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            String description,
            ConfigurationStatus status) {
    }

    record CaregiverCertificationResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            String description,
            boolean expirationRequired,
            ConfigurationStatus status) {
    }
}
