package com.homehealthcare.patientauthorization.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientEpisodeAuthorizationRepository extends JpaRepository<PatientEpisodeAuthorization, UUID> {

    List<PatientEpisodeAuthorization> findAllByPatient_IdOrderByStartDateDesc(UUID patientId);

    @Query("""
            select case when count(auth) > 0 then true else false end
            from PatientEpisodeAuthorization auth
            where auth.patient.id = :patientId
              and (:excludeId is null or auth.id <> :excludeId)
              and (
                    (:authorizationNumber is not null and auth.authorizationNumber = :authorizationNumber)
                 or (:authorizationNumber is null and auth.serviceLine.id = :serviceLineId)
              )
              and auth.startDate <= :endDate
              and auth.endDate >= :startDate
            """)
    boolean existsConflictingWindow(
            @Param("patientId") UUID patientId,
            @Param("authorizationNumber") String authorizationNumber,
            @Param("serviceLineId") UUID serviceLineId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId);
}
