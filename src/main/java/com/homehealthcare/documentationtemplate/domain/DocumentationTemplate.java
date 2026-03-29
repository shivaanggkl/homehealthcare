package com.homehealthcare.documentationtemplate.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.visittype.domain.VisitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documentation_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTemplate extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 64)
    private DocumentationTemplateType templateType;

    @Column(name = "structured_definition_json", nullable = false, columnDefinition = "clob")
    private String structuredDefinitionJson;

    @Column(name = "version", nullable = false)
    private int version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_type_id")
    private VisitType visitType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "allowed_actor_roles", length = 500)
    private String allowedActorRoles;

    @Column(name = "requires_signature_verification", nullable = false)
    private boolean requiresSignatureVerification;

    @Builder
    private DocumentationTemplate(
            UUID id,
            Agency agency,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int version,
            ServiceLine serviceLine,
            VisitType visitType,
            Branch branch,
            String helpText,
            String allowedActorRoles,
            boolean requiresSignatureVerification) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.templateType = templateType;
        this.structuredDefinitionJson = structuredDefinitionJson;
        this.version = version;
        this.serviceLine = serviceLine;
        this.visitType = visitType;
        this.branch = branch;
        this.helpText = helpText;
        this.allowedActorRoles = allowedActorRoles;
        this.requiresSignatureVerification = requiresSignatureVerification;
    }

    public static DocumentationTemplate createDraft(
            Agency agency,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder) {
        return createDraft(agency, name, code, templateType, structuredDefinitionJson, displayOrder, null, null, null, null, Set.of(), false);
    }

    public static DocumentationTemplate createDraft(
            Agency agency,
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder,
            ServiceLine serviceLine,
            VisitType visitType,
            Branch branch,
            String helpText,
            Set<AgencyRole> allowedActorRoles,
            boolean requiresSignatureVerification) {
        DocumentationTemplate template = DocumentationTemplate.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .templateType(templateType)
                .structuredDefinitionJson(structuredDefinitionJson)
                .version(1)
                .serviceLine(serviceLine)
                .visitType(visitType)
                .branch(branch)
                .helpText(helpText)
                .allowedActorRoles(joinRoles(allowedActorRoles))
                .requiresSignatureVerification(requiresSignatureVerification)
                .build();
        template.updateDisplayOrder(displayOrder);
        template.markDraft();
        return template;
    }

    public void updateDraft(
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder) {
        updateDraft(name, code, templateType, structuredDefinitionJson, displayOrder, null, null, null, null, Set.of(), requiresSignatureVerification);
    }

    public void updateDraft(
            String name,
            String code,
            DocumentationTemplateType templateType,
            String structuredDefinitionJson,
            int displayOrder,
            ServiceLine serviceLine,
            VisitType visitType,
            Branch branch,
            String helpText,
            Set<AgencyRole> allowedActorRoles,
            boolean requiresSignatureVerification) {
        this.name = name;
        this.code = code;
        this.templateType = templateType;
        this.structuredDefinitionJson = structuredDefinitionJson;
        this.version = this.version + 1;
        this.serviceLine = serviceLine;
        this.visitType = visitType;
        this.branch = branch;
        this.helpText = helpText;
        this.allowedActorRoles = joinRoles(allowedActorRoles);
        this.requiresSignatureVerification = requiresSignatureVerification;
        updateDisplayOrder(displayOrder);
        markDraft();
    }

    public void publish() {
        activate();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    public UUID getVisitTypeId() {
        return visitType == null ? null : visitType.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public Set<AgencyRole> allowedActorRoleSet() {
        if (allowedActorRoles == null || allowedActorRoles.isBlank()) {
            return Set.of();
        }
        EnumSet<AgencyRole> roles = EnumSet.noneOf(AgencyRole.class);
        Arrays.stream(allowedActorRoles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AgencyRole::valueOf)
                .forEach(roles::add);
        return roles;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = required(name);
        code = required(code).toUpperCase(Locale.ROOT);
        structuredDefinitionJson = required(structuredDefinitionJson);
        Objects.requireNonNull(templateType, "templateType must not be null");
        helpText = optional(helpText);
        allowedActorRoles = normalizeRoles(allowedActorRoles);
        validateLinks(getAgency(), serviceLine, visitType, branch);
        if (version <= 0) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static void validateLinks(Agency agency, ServiceLine serviceLine, VisitType visitType, Branch branch) {
        UUID agencyId = agency == null ? null : agency.getId();
        if (serviceLine != null && !Objects.equals(agencyId, serviceLine.getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the documentation template");
        }
        if (visitType != null && !Objects.equals(agencyId, visitType.getAgencyId())) {
            throw new IllegalArgumentException("visitType must belong to the same agency as the documentation template");
        }
        if (branch != null && !Objects.equals(agencyId, branch.getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the documentation template");
        }
    }

    private static String joinRoles(Set<AgencyRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream().map(AgencyRole::name).sorted().reduce((left, right) -> left + "," + right).orElse(null);
    }

    private static String normalizeRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return null;
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AgencyRole::valueOf)
                .map(AgencyRole::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
