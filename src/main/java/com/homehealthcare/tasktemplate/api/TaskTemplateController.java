package com.homehealthcare.tasktemplate.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.tasktemplate.application.TaskTemplateCatalogService;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
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
@RequestMapping("/api/task-templates")
class TaskTemplateController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskTemplateCatalogService taskTemplateCatalogService;

    TaskTemplateController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            TaskTemplateRepository taskTemplateRepository,
            TaskTemplateCatalogService taskTemplateCatalogService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskTemplateCatalogService = taskTemplateCatalogService;
    }

    @GetMapping
    PagedResponse<TaskTemplateResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "category", required = false) TaskTemplateCategory category,
            @RequestParam(name = "serviceLineId", required = false) UUID serviceLineId,
            @RequestParam(name = "visitTypeId", required = false) UUID visitTypeId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actor = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actor,
                AgencyPermission.VIEW_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);

        List<TaskTemplateResponse> filtered = taskTemplateRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(actor.getAgencyId())
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> category == null || item.getCategory() == category)
                .filter(item -> serviceLineId == null || (item.getServiceLine() != null && serviceLineId.equals(item.getServiceLine().getId())))
                .filter(item -> visitTypeId == null || (item.getVisitType() != null && visitTypeId.equals(item.getVisitType().getId())))
                .filter(item -> matchesSearch(search, item.getName(), item.getCode()))
                .map(TaskTemplateController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    TaskTemplateResponse create(@Valid @RequestBody ManageTaskTemplateRequest request) {
        return toResponse(taskTemplateCatalogService.create(
                configurationActorResolver.requireActorMembership(),
                new TaskTemplateCatalogService.ManageTaskTemplateCommand(
                        request.serviceLineId(),
                        request.visitTypeId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.category(),
                        request.displayOrder())));
    }

    @PutMapping("/{taskTemplateId}")
    TaskTemplateResponse update(@PathVariable UUID taskTemplateId, @Valid @RequestBody ManageTaskTemplateRequest request) {
        return toResponse(taskTemplateCatalogService.update(
                configurationActorResolver.requireActorMembership(),
                taskTemplateId,
                new TaskTemplateCatalogService.ManageTaskTemplateCommand(
                        request.serviceLineId(),
                        request.visitTypeId(),
                        request.name(),
                        request.code(),
                        request.description(),
                        request.category(),
                        request.displayOrder())));
    }

    @DeleteMapping("/{taskTemplateId}")
    TaskTemplateResponse deactivate(@PathVariable UUID taskTemplateId) {
        return toResponse(taskTemplateCatalogService.deactivate(configurationActorResolver.requireActorMembership(), taskTemplateId));
    }

    private static boolean matchesSearch(String search, String name, String code) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalized)
                || code.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static PagedResponse<TaskTemplateResponse> page(List<TaskTemplateResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static TaskTemplateResponse toResponse(TaskTemplate template) {
        return new TaskTemplateResponse(
                template.getId(),
                template.getAgencyId(),
                template.getServiceLine() == null ? null : template.getServiceLine().getId(),
                template.getVisitType() == null ? null : template.getVisitType().getId(),
                template.getName(),
                template.getCode(),
                template.getDescription(),
                template.getCategory(),
                template.getStatus(),
                template.getDisplayOrder());
    }

    record ManageTaskTemplateRequest(
            UUID serviceLineId,
            UUID visitTypeId,
            @NotBlank String name,
            @NotBlank String code,
            String description,
            TaskTemplateCategory category,
            int displayOrder) {
    }

    record TaskTemplateResponse(
            UUID id,
            UUID agencyId,
            UUID serviceLineId,
            UUID visitTypeId,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            ConfigurationStatus status,
            int displayOrder) {
    }
}
