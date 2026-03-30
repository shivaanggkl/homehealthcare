package com.homehealthcare.revenuereadiness.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payer_service_summary_projections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayerServiceSummaryProjection extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_payer_link_id")
    private PatientPayerLink patientPayerLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id")
    private PatientEpisodeAuthorization authorization;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "payer_external_id", length = 120)
    private String payerExternalId;

    @Column(name = "member_policy_number", length = 120)
    private String memberPolicyNumber;

    @Column(name = "authorization_number", length = 120)
    private String authorizationNumber;

    @Column(name = "service_line_code", length = 50)
    private String serviceLineCode;

    @Column(name = "service_line_name", length = 200)
    private String serviceLineName;

    @Column(name = "primary_payer", nullable = false)
    private boolean primaryPayer;

    @Column(name = "summarized_at", nullable = false)
    private OffsetDateTime summarizedAt;

    @Builder
    private PayerServiceSummaryProjection(
            UUID id,
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            PatientPayerLink patientPayerLink,
            PatientEpisodeAuthorization authorization,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String authorizationNumber,
            String serviceLineCode,
            String serviceLineName,
            boolean primaryPayer,
            OffsetDateTime summarizedAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignPatientPayerLink(patientPayerLink);
        assignAuthorization(authorization);
        this.payerName = payerName;
        this.payerExternalId = payerExternalId;
        this.memberPolicyNumber = memberPolicyNumber;
        this.authorizationNumber = authorizationNumber;
        this.serviceLineCode = serviceLineCode;
        this.serviceLineName = serviceLineName;
        this.primaryPayer = primaryPayer;
        this.summarizedAt = Objects.requireNonNull(summarizedAt, "summarizedAt must not be null");
    }

    public static PayerServiceSummaryProjection create(
            VisitOccurrence visitOccurrence,
            Patient patient,
            Branch branch,
            ServiceLine serviceLine,
            PatientPayerLink patientPayerLink,
            PatientEpisodeAuthorization authorization,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String authorizationNumber,
            String serviceLineCode,
            String serviceLineName,
            boolean primaryPayer,
            OffsetDateTime summarizedAt) {
        return PayerServiceSummaryProjection.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .patient(patient)
                .branch(branch)
                .serviceLine(serviceLine)
                .patientPayerLink(patientPayerLink)
                .authorization(authorization)
                .payerName(payerName)
                .payerExternalId(payerExternalId)
                .memberPolicyNumber(memberPolicyNumber)
                .authorizationNumber(authorizationNumber)
                .serviceLineCode(serviceLineCode)
                .serviceLineName(serviceLineName)
                .primaryPayer(primaryPayer)
                .summarizedAt(summarizedAt)
                .build();
    }

    public void refresh(
            Branch branch,
            ServiceLine serviceLine,
            PatientPayerLink patientPayerLink,
            PatientEpisodeAuthorization authorization,
            String payerName,
            String payerExternalId,
            String memberPolicyNumber,
            String authorizationNumber,
            String serviceLineCode,
            String serviceLineName,
            boolean primaryPayer,
            OffsetDateTime summarizedAt) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignPatientPayerLink(patientPayerLink);
        assignAuthorization(authorization);
        this.payerName = payerName;
        this.payerExternalId = payerExternalId;
        this.memberPolicyNumber = memberPolicyNumber;
        this.authorizationNumber = authorizationNumber;
        this.serviceLineCode = serviceLineCode;
        this.serviceLineName = serviceLineName;
        this.primaryPayer = primaryPayer;
        this.summarizedAt = Objects.requireNonNull(summarizedAt, "summarizedAt must not be null");
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getAuthorizationId() {
        return authorization == null ? null : authorization.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        if (!Objects.equals(patient.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patient must belong to the same agency as the payer/service summary");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the payer/service summary");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the payer/service summary");
        }
        this.serviceLine = serviceLine;
    }

    private void assignPatientPayerLink(PatientPayerLink patientPayerLink) {
        if (patientPayerLink != null && !Objects.equals(patientPayerLink.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("patientPayerLink must belong to the same agency as the payer/service summary");
        }
        this.patientPayerLink = patientPayerLink;
    }

    private void assignAuthorization(PatientEpisodeAuthorization authorization) {
        if (authorization != null && !Objects.equals(authorization.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("authorization must belong to the same agency as the payer/service summary");
        }
        this.authorization = authorization;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        payerName = normalizeOptional(payerName);
        payerExternalId = normalizeOptional(payerExternalId);
        memberPolicyNumber = normalizeOptional(memberPolicyNumber);
        authorizationNumber = normalizeOptional(authorizationNumber);
        serviceLineCode = normalizeOptional(serviceLineCode);
        serviceLineName = normalizeOptional(serviceLineName);
        assignPatient(patient);
        assignBranch(branch);
        assignServiceLine(serviceLine);
        assignPatientPayerLink(patientPayerLink);
        assignAuthorization(authorization);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
