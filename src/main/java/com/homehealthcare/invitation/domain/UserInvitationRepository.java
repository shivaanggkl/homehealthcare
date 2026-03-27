package com.homehealthcare.invitation.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByToken(String token);

    Optional<UserInvitation> findFirstByAgency_IdAndEmailAndStatusOrderByCreatedAtDesc(
            UUID agencyId,
            String email,
            UserInvitationStatus status);
}
