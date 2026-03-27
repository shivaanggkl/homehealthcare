package com.homehealthcare.platform.admin.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalSuperAdminRepository extends JpaRepository<InternalSuperAdmin, UUID> {

    Optional<InternalSuperAdmin> findByEmail(String email);
}
