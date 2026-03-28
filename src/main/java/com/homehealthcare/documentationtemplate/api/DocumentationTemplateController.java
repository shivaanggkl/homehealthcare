package com.homehealthcare.documentationtemplate.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.ConfigurationPayloadValidator;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.configuration.foundation.ConfigurationStatus;
import com.homehealthcare.configuration.foundation.UnauthorizedConfigurationActorException;
import com.homehealthcare.documentationtemplate.application.DocumentationTemplateCatalogService;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplate;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateRepository;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documentation-templates")
class DocumentationTemplateController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final ConfigurationPayloadValidator configurationPayloadValidator;
    private final DocumentationTemplateRepository documentationTemplateRepository;
    private final DocumentationTemplateCatalogService documentationTemplateCatalogService;

    DocumentationTemplateController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            ConfigurationPayloadValidator configurationPayloadValidator,
            DocumentationTemplateRepository documentationTemplateRepository,
            DocumentationTemplateCatalogService documentationTemplateCatalogService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.configurationPayloadValidator = configurationPayloadValidator;
        this.documentationTemplateRepository = documentationTemplateRepository;
        this.documentationTemplateCatalogService = documentationTemplateCatalogService;
    }

    @GetMapping
    PagedResponse<DocumentationTemplateResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) ConfigurationStatus status,
            @RequestParam(name = "templateType", required = false) DocumentationTemplateType templateType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        var actor = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(
                actor,
                AgencyPermission.VIEW_TEMPLATE_CONFIGURATION,
                UnauthorizedConfigurationActorException::new);

        List<DocumentationTemplateResponse> filtered = documentationTemplateRepository
                .findAllByAgency_IdOrderByDisplayOrderAscNameAsc(actor.getAgencyId())
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> templateType == null || item.getTemplateType() == templateType)
                .filter(item -> matchesSearch(search, item.getName(), item.getCode()))
                .map(DocumentationTemplateController::toResponse)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    DocumentationTemplateResponse create(@Valid @RequestBody ManageDocumentationTemplateRequest request) {
        configurationPayloadValidator.requireJsonObjectOrArray("structuredDefinitionJson", request.structuredDefinitionJson());
        return toResponse(documentationTemplateCatalogService.createDraft(
                configurationActorResolver.requireActorMembership(),
                new DocumentationTemplateCatalogService.ManageDocumentationTemplateCommand(
                        request.name(),
                        request.code(),
                        request.templateType(),
                        request.structuredDefinitionJson(),
                        request.displayOrder())));
    }

    @PutMapping("/{templateId}")
    DocumentationTemplateResponse update(@PathVariable UUID templateId, @Valid @RequestBody ManageDocumentationTemplateRequest request) {
        configurationPayloadValidator.requireJsonObjectOrArray("structuredDefinitionJson", request.structuredDefinitionJson());
        return toResponse(documentationTemplateCatalogService.updateDraft(
                configurationActorResolver.requireActorMembership(),
                templateId,
                new DocumentationTemplateCatalogService.ManageDocumentationTemplateCommand(
                        request.name(),
                        request.code(),
                        request.templateType(),
                        request.structuredDefinitionJson(),
                        request.displayOrder())));
    }

    @PostMapping("/{templateId}/version")
    DocumentationTemplateResponse createNextVersion(
            @PathVariable UUID templateId,
            @Valid @RequestBody ManageDocumentationTemplateRequest request) {
        configurationPayloadValidator.requireJsonObjectOrArray("structuredDefinitionJson", request.structuredDefinitionJson());
        return toResponse(documentationTemplateCatalogService.updateDraft(
                configurationActorResolver.requireActorMembership(),
                templateId,
                new DocumentationTemplateCatalogService.ManageDocumentationTemplateCommand(
                        request.name(),
                        request.code(),
                        request.templateType(),
                        request.structuredDefinitionJson(),
                        request.displayOrder())));
    }

    @PostMapping("/{templateId}/publish")
    DocumentationTemplateResponse publish(@PathVariable UUID templateId) {
        return toResponse(documentationTemplateCatalogService.publish(
                configurationActorResolver.requireActorMembership(),
                templateId));
    }

    private static boolean matchesSearch(String search, String name, String code) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(normalized)
                || code.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static PagedResponse<DocumentationTemplateResponse> page(List<DocumentationTemplateResponse> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static DocumentationTemplateResponse toResponse(DocumentationTemplate template) {
        return new DocumentationTemplateResponse(
                template.getId(),
                template.getAgencyId(),
                template.getName(),
                template.getCode(),
                template.getTemplateType(),
                template.getStructuredDefinitionJson(),
                template.getVersion(),
                template.getStatus(),
                template.getDisplayOrder());
    }

    record ManageDocumentationTemplateRequest(
            @NotBlank String name,
            @NotBlank String code,
            DocumentationTemplateType templateType,
            @NotBlank String structuredDefinitionJson,
            int displayOrder) {
    }

    record DocumentationTemplateResponse(
            UUID id,
            UUID agencyId,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int version,
            ConfigurationStatus status,
            int displayOrder) {
    }
}
