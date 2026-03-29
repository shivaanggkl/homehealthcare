package com.homehealthcare.tasktemplate.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "task_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskTemplate extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_type_id")
    private VisitType visitType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private TaskTemplateCategory category;

    @Column(name = "default_sort_order", nullable = false)
    private int defaultSortOrder;

    @Column(name = "default_completion_expectation", length = 255)
    private String defaultCompletionExpectation;

    @Column(name = "required_by_default", nullable = false)
    private boolean requiredByDefault;

    @Builder
    private TaskTemplate(
            UUID id,
            Agency agency,
            ServiceLine serviceLine,
            VisitType visitType,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            int defaultSortOrder,
            String defaultCompletionExpectation,
            boolean requiredByDefault) {
        this.id = id;
        assignAgency(agency);
        this.serviceLine = serviceLine;
        this.visitType = visitType;
        this.name = name;
        this.code = code;
        this.description = description;
        this.category = category;
        this.defaultSortOrder = defaultSortOrder;
        this.defaultCompletionExpectation = defaultCompletionExpectation;
        this.requiredByDefault = requiredByDefault;
    }

    public static TaskTemplate create(
            Agency agency,
            ServiceLine serviceLine,
            VisitType visitType,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            int displayOrder) {
        return create(agency, serviceLine, visitType, name, code, description, category, displayOrder, displayOrder, null, false);
    }

    public static TaskTemplate create(
            Agency agency,
            ServiceLine serviceLine,
            VisitType visitType,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            int displayOrder,
            int defaultSortOrder,
            String defaultCompletionExpectation,
            boolean requiredByDefault) {
        validateLinks(agency, serviceLine, visitType);
        TaskTemplate template = TaskTemplate.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .serviceLine(serviceLine)
                .visitType(visitType)
                .name(name)
                .code(code)
                .description(description)
                .category(category)
                .defaultSortOrder(defaultSortOrder)
                .defaultCompletionExpectation(defaultCompletionExpectation)
                .requiredByDefault(requiredByDefault)
                .build();
        template.updateDisplayOrder(displayOrder);
        template.activate();
        return template;
    }

    public void updateDetails(
            ServiceLine serviceLine,
            VisitType visitType,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            int displayOrder) {
        updateDetails(serviceLine, visitType, name, code, description, category, displayOrder, defaultSortOrder, defaultCompletionExpectation, requiredByDefault);
    }

    public void updateDetails(
            ServiceLine serviceLine,
            VisitType visitType,
            String name,
            String code,
            String description,
            TaskTemplateCategory category,
            int displayOrder,
            int defaultSortOrder,
            String defaultCompletionExpectation,
            boolean requiredByDefault) {
        validateLinks(getAgency(), serviceLine, visitType);
        this.serviceLine = serviceLine;
        this.visitType = visitType;
        this.name = name;
        this.code = code;
        this.description = description;
        this.category = category;
        this.defaultSortOrder = defaultSortOrder;
        this.defaultCompletionExpectation = defaultCompletionExpectation;
        this.requiredByDefault = requiredByDefault;
        updateDisplayOrder(displayOrder);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = required(name);
        code = required(code).toUpperCase(Locale.ROOT);
        description = optional(description);
        defaultCompletionExpectation = optional(defaultCompletionExpectation);
        Objects.requireNonNull(category, "category must not be null");
        if (defaultSortOrder < 0) {
            throw new IllegalArgumentException("defaultSortOrder must be greater than or equal to 0");
        }
        validateLinks(getAgency(), serviceLine, visitType);
    }

    private static void validateLinks(Agency agency, ServiceLine serviceLine, VisitType visitType) {
        UUID agencyId = agency == null ? null : agency.getId();
        if (serviceLine != null && !Objects.equals(agencyId, serviceLine.getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the task template");
        }
        if (visitType != null && !Objects.equals(agencyId, visitType.getAgencyId())) {
            throw new IllegalArgumentException("visitType must belong to the same agency as the task template");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
