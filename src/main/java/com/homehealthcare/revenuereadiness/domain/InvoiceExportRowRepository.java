package com.homehealthcare.revenuereadiness.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceExportRowRepository extends JpaRepository<InvoiceExportRow, UUID> {

    List<InvoiceExportRow> findAllByAgency_IdOrderByGeneratedAtDesc(UUID agencyId);
}
