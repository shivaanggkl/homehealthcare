package com.homehealthcare.notification.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNotificationPreferenceRepository extends JpaRepository<AdminNotificationPreference, UUID> {

    Optional<AdminNotificationPreference> findByAgencyMembership_Id(UUID agencyMembershipId);
}
