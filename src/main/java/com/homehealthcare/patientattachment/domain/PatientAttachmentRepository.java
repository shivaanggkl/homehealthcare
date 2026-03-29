package com.homehealthcare.patientattachment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAttachmentRepository extends JpaRepository<PatientAttachment, UUID> {

    List<PatientAttachment> findAllByPatient_IdOrderByCreatedAtDesc(UUID patientId);

    long countByPatient_Id(UUID patientId);
}
