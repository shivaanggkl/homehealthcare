package com.homehealthcare.documentation.domain;

import com.homehealthcare.documentation.foundation.DocumentationFieldType;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "documentation_template_fields")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentationTemplateField extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documentation_template_id", nullable = false)
    private com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private DocumentationTemplateSection section;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 32)
    private DocumentationFieldType fieldType;

    @Column(name = "required_field", nullable = false)
    private boolean requiredField;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "options_json", columnDefinition = "clob")
    private String optionsJson;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @Column(name = "visible_actor_roles", length = 500)
    private String visibleActorRoles;

    @Column(name = "editable_actor_roles", length = 500)
    private String editableActorRoles;

    @Builder
    private DocumentationTemplateField(
            UUID id,
            com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate,
            DocumentationTemplateSection section,
            String fieldKey,
            String label,
            DocumentationFieldType fieldType,
            boolean requiredField,
            int sortOrder,
            String optionsJson,
            String helpText,
            String visibleActorRoles,
            String editableActorRoles) {
        this.id = id;
        assignDocumentationTemplate(documentationTemplate);
        assignSection(section);
        this.fieldKey = fieldKey;
        this.label = label;
        this.fieldType = Objects.requireNonNull(fieldType, "fieldType must not be null");
        this.requiredField = requiredField;
        this.sortOrder = sortOrder;
        this.optionsJson = optionsJson;
        this.helpText = helpText;
        this.visibleActorRoles = visibleActorRoles;
        this.editableActorRoles = editableActorRoles;
    }

    public static DocumentationTemplateField create(
            com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate,
            DocumentationTemplateSection section,
            String fieldKey,
            String label,
            DocumentationFieldType fieldType,
            boolean requiredField,
            int sortOrder,
            String optionsJson,
            String helpText,
            Set<AgencyRole> visibleActorRoles,
            Set<AgencyRole> editableActorRoles) {
        return DocumentationTemplateField.builder()
                .id(UUID.randomUUID())
                .documentationTemplate(documentationTemplate)
                .section(section)
                .fieldKey(fieldKey)
                .label(label)
                .fieldType(fieldType)
                .requiredField(requiredField)
                .sortOrder(sortOrder)
                .optionsJson(optionsJson)
                .helpText(helpText)
                .visibleActorRoles(joinRoles(visibleActorRoles))
                .editableActorRoles(joinRoles(editableActorRoles))
                .build();
    }

    public Set<AgencyRole> visibleActorRoleSet() {
        return parseRoles(visibleActorRoles);
    }

    public Set<AgencyRole> editableActorRoleSet() {
        return parseRoles(editableActorRoles);
    }

    public boolean isVisibleTo(AgencyRole role) {
        Set<AgencyRole> roles = visibleActorRoleSet();
        return roles.isEmpty() || roles.contains(role);
    }

    public boolean isEditableBy(AgencyRole role) {
        Set<AgencyRole> roles = editableActorRoleSet();
        return roles.isEmpty() || roles.contains(role);
    }

    public UUID getDocumentationTemplateId() {
        return documentationTemplate == null ? null : documentationTemplate.getId();
    }

    private void assignDocumentationTemplate(com.homehealthcare.documentationtemplate.domain.DocumentationTemplate documentationTemplate) {
        this.documentationTemplate = Objects.requireNonNull(documentationTemplate, "documentationTemplate must not be null");
        assignAgency(documentationTemplate.getAgency());
    }

    private void assignSection(DocumentationTemplateSection section) {
        if (section != null && !Objects.equals(section.getDocumentationTemplateId(), getDocumentationTemplateId())) {
            throw new IllegalArgumentException("section must belong to the same documentation template as the field");
        }
        this.section = section;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        fieldKey = required(fieldKey);
        label = required(label);
        optionsJson = optional(optionsJson);
        helpText = optional(helpText);
        visibleActorRoles = normalizeRoles(visibleActorRoles);
        editableActorRoles = normalizeRoles(editableActorRoles);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be greater than or equal to 0");
        }
        fieldType = Objects.requireNonNull(fieldType, "fieldType must not be null");
        assignDocumentationTemplate(documentationTemplate);
        assignSection(section);
    }

    private static Set<AgencyRole> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        EnumSet<AgencyRole> parsed = EnumSet.noneOf(AgencyRole.class);
        Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AgencyRole::valueOf)
                .forEach(parsed::add);
        return parsed;
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

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
