package com.homehealthcare.serviceline.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.serviceline.application.ServiceLineCatalogService;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
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
@RequestMapping("/api/service-lines")
class ServiceLineController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final ServiceLineCatalogService serviceLineCatalogService;
    private final ServiceLineRepository serviceLineRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    ServiceLineController(
            ConfigurationActorResolver configurationActorResolver,
            ServiceLineCatalogService serviceLineCatalogService,
            ServiceLineRepository serviceLineRepository,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.configurationActorResolver = configurationActorResolver;
        this.serviceLineCatalogService = serviceLineCatalogService;
        this.serviceLineRepository = serviceLineRepository;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    PagedResponse<ServiceLineResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        UUID agencyId = configurationActorResolver.requireActorMembership().getAgencyId();
        agencyAuthorizationGuard.requirePermission(
                configurationActorResolver.requireActorMembership(),
                AgencyPermission.VIEW_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        List<ServiceLineResponse> filtered = serviceLineRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agencyId).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> matchesSearch(item.getName(), item.getCode(), search))
                .map(ServiceLineController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    ServiceLineResponse create(@Valid @RequestBody ManageServiceLineRequest request) {
        return toResponse(serviceLineCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new ServiceLineCatalogService.ManageServiceLineCommand(
                        request.name(),
                        request.code(),
                        request.description(),
                        request.displayOrder())));
    }

    @PutMapping("/{serviceLineId}")
    ServiceLineResponse update(@PathVariable UUID serviceLineId, @Valid @RequestBody ManageServiceLineRequest request) {
        return toResponse(serviceLineCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                serviceLineId,
                new ServiceLineCatalogService.ManageServiceLineCommand(
                        request.name(),
                        request.code(),
                        request.description(),
                        request.displayOrder())));
    }

    @DeleteMapping("/{serviceLineId}")
    ServiceLineResponse deactivate(@PathVariable UUID serviceLineId) {
        return toResponse(serviceLineCatalogService.deactivate(configurationActorResolver.requireActorMembership(), serviceLineId));
    }

    private static boolean matchesSearch(String name, String code, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalized)
                || code.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static PagedResponse<ServiceLineResponse> page(List<ServiceLineResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static ServiceLineResponse toResponse(ServiceLine serviceLine) {
        return new ServiceLineResponse(
                serviceLine.getId(),
                serviceLine.getAgencyId(),
                serviceLine.getName(),
                serviceLine.getCode(),
                serviceLine.getDescription(),
                serviceLine.getStatus(),
                serviceLine.getDisplayOrder());
    }

    record ManageServiceLineRequest(
            @NotBlank String name,
            @NotBlank String code,
            String description,
            int displayOrder) {
    }

    record ServiceLineResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            String description,
            ConfigurationStatus status,
            int displayOrder) {
    }
}
