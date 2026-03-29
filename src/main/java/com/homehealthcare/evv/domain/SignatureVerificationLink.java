package com.homehealthcare.evv.domain;

import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
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
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_signature_verification_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignatureVerificationLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_session_id", nullable = false)
    private EvvVerificationSession verificationSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id")
    private MobileFieldArtifact artifact;

    @Enumerated(EnumType.STRING)
    @Column(name = "signer_role", nullable = false, length = 24)
    private SignatureSignerRole signerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 24)
    private SignatureVerificationStatus verificationStatus;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Builder
    private SignatureVerificationLink(
            UUID id,
            EvvVerificationSession verificationSession,
            MobileFieldArtifact artifact,
            SignatureSignerRole signerRole,
            SignatureVerificationStatus verificationStatus,
            OffsetDateTime recordedAt) {
        this.id = id;
        assignVerificationSession(verificationSession);
        assignArtifact(artifact);
        this.signerRole = Objects.requireNonNull(signerRole, "signerRole must not be null");
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }

    public static SignatureVerificationLink record(
            EvvVerificationSession verificationSession,
            MobileFieldArtifact artifact,
            SignatureSignerRole signerRole,
            SignatureVerificationStatus verificationStatus,
            OffsetDateTime recordedAt) {
        return SignatureVerificationLink.builder()
                .id(UUID.randomUUID())
                .verificationSession(verificationSession)
                .artifact(artifact)
                .signerRole(signerRole)
                .verificationStatus(verificationStatus)
                .recordedAt(recordedAt)
                .build();
    }

    private void assignVerificationSession(EvvVerificationSession verificationSession) {
        this.verificationSession = Objects.requireNonNull(verificationSession, "verificationSession must not be null");
        assignAgency(verificationSession.getAgency());
    }

    private void assignArtifact(MobileFieldArtifact artifact) {
        if (artifact != null) {
            if (!Objects.equals(artifact.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("artifact must belong to the same agency as the signature verification link");
            }
            if (artifact.getArtifactType() != MobileFieldArtifactType.SIGNATURE) {
                throw new IllegalArgumentException("artifact must be a SIGNATURE artifact");
            }
        }
        this.artifact = artifact;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignArtifact(artifact);
    }
}
