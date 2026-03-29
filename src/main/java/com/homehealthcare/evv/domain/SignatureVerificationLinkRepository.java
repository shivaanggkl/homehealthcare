package com.homehealthcare.evv.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignatureVerificationLinkRepository extends JpaRepository<SignatureVerificationLink, UUID> {

    List<SignatureVerificationLink> findAllByVerificationSession_IdOrderByRecordedAtAsc(UUID verificationSessionId);
}
