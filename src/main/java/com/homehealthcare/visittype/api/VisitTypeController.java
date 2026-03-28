package com.homehealthcare.visittype.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.visittype.application.VisitTypeCatalogService;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
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
@RequestMapping("/api/visit-types")
class VisitTypeController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final VisitTypeCatalogService visitTypeCatalogService;
    private final VisitTypeRepository visitTypeRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    VisitTypeController(
            ConfigurationActorResolver configurationActorResolver,
            VisitTypeCatalogService visitTypeCatalogService,
            VisitTypeRepository visitTypeRepository,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.configurationActorResolver = configurationActorResolver;
        this.visitTypeCatalogService = visitTypeCatalogService;
        this.visitTypeRepository = visitTypeRepository;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    PagedResponse<VisitTypeResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        UUID agencyId = configurationActorResolver.requireActorMembership().getAgencyId();
        agencyAuthorizationGuard.requirePermission(
                configurationActorResolver.requireActorMembership(),
                AgencyPermission.VIEW_AGENCY_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);
        List<VisitTypeResponse> filtered = visitTypeRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agencyId).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> serviceLineId == null
                        || (item.getServiceLine() != null && serviceLineId.equals(item.getServiceLine().getId())))
                .filter(item -> matchesSearch(item.getName(), item.getCode(), search))
                .map(VisitTypeController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    VisitTypeResponse create(@Valid @RequestBody ManageVisitTypeRequest request) {
        return toResponse(visitTypeCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new VisitTypeCatalogService.ManageVisitTypeCommand(
                        request.serviceLineId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.defaultDurationMinutes(),
                        request.billable(),
                        request.displayOrder())));
    }

    @PutMapping("/{visitTypeId}")
    VisitTypeResponse update(@PathVariable UUID visitTypeId, @Valid @RequestBody ManageVisitTypeRequest request) {
        return toResponse(visitTypeCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                visitTypeId,
                new VisitTypeCatalogService.ManageVisitTypeCommand(
                        request.serviceLineId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.defaultDurationMinutes(),
                        request.billable(),
                        request.displayOrder())));
    }

    @DeleteMapping("/{visitTypeId}")
    VisitTypeResponse deactivate(@PathVariable UUID visitTypeId) {
        return toResponse(visitTypeCatalogService.deactivate(configurationActorResolver.requireActorMembership(), visitTypeId));
    }

    private static boolean matchesSearch(String name, String code, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalized)
                || code.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static PagedResponse<VisitTypeResponse> page(List<VisitTypeResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static VisitTypeResponse toResponse(VisitType visitType) {
        return new VisitTypeResponse(
                visitType.getId(),
                visitType.getAgencyId(),
                visitType.getServiceLine() == null ? null : visitType.getServiceLine().getId(),
                visitType.getName(),
                visitType.getCode(),
                visitType.getDescription(),
                visitType.getDefaultDurationMinutes(),
                visitType.isBillable(),
                visitType.getStatus(),
                visitType.getDisplayOrder());
    }

    record ManageVisitTypeRequest(
            UUID serviceLineId,
            @NotBlank String name,
            @NotBlank String code,
            String description,
            @Min(1) int defaultDurationMinutes,
            boolean billable,
            int displayOrder) {
    }

    record VisitTypeResponse(
            UUID id,
            UUID agencyId,
            UUID serviceLineId,
            String name,
            String code,
            String description,
            int defaultDurationMinutes,
            boolean billable,
            ConfigurationStatus status,
            int displayOrder) {
    }
}
