package com.homehealthcare.patientpayer.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientPayerLinkRepository extends JpaRepository<PatientPayerLink, UUID> {

    List<PatientPayerLink> findAllByPatient_IdOrderByEffectiveFromDesc(UUID patientId);

    @Query("""
            select case when count(link) > 0 then true else false end
            from PatientPayerLink link
            where link.patient.id = :patientId
              and (:excludeId is null or link.id <> :excludeId)
              and (
                    coalesce(link.payerExternalId, '') = coalesce(:payerExternalId, '')
                and coalesce(link.payerName, '') = coalesce(:payerName, '')
                and coalesce(link.memberPolicyNumber, '') = coalesce(:memberPolicyNumber, '')
              )
              and link.effectiveFrom <= :effectiveToBoundary
              and coalesce(link.effectiveTo, :openEndedDate) >= :effectiveFrom
            """)
    boolean existsOverlappingCoverage(
            @Param("patientId") UUID patientId,
            @Param("payerName") String payerName,
            @Param("payerExternalId") String payerExternalId,
            @Param("memberPolicyNumber") String memberPolicyNumber,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveToBoundary") LocalDate effectiveToBoundary,
            @Param("openEndedDate") LocalDate openEndedDate,
            @Param("excludeId") UUID excludeId);

    long countByPatient_IdAndPrimaryPayerTrueAndStatusIn(UUID patientId, List<PatientPayerLinkStatus> statuses);
}
