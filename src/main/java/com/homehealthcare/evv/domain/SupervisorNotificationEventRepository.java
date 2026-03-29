package com.homehealthcare.evv.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupervisorNotificationEventRepository extends JpaRepository<SupervisorNotificationEvent, UUID> {
}
