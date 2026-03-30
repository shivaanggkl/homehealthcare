package com.homehealthcare.revenuereadiness.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollExportRowRepository extends JpaRepository<PayrollExportRow, UUID> {

    List<PayrollExportRow> findAllByAgency_IdOrderByGeneratedAtDesc(UUID agencyId);
}
