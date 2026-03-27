package com.homehealthcare.agency.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    boolean existsBySlug(String slug);

    Optional<Agency> findBySlug(String slug);
}
