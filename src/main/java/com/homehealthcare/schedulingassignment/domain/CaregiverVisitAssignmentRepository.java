package com.homehealthcare.schedulingassignment.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaregiverVisitAssignmentRepository extends JpaRepository<CaregiverVisitAssignment, UUID> {

    Optional<CaregiverVisitAssignment> findByIdAndAgency_Id(UUID id, UUID agencyId);

    List<CaregiverVisitAssignment> findAllByVisitOccurrence_IdOrderByAssignedAtAsc(UUID visitOccurrenceId);

    List<CaregiverVisitAssignment> findAllByCaregiverProfile_IdOrderByAssignedAtAsc(UUID caregiverProfileId);

    Optional<CaregiverVisitAssignment> findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(
            UUID visitOccurrenceId,
            CaregiverAssignmentStatus assignmentStatus);

    @Query("""
            select case when count(assignment) > 0 then true else false end
            from CaregiverVisitAssignment assignment
            where assignment.caregiverProfile.id = :caregiverProfileId
              and assignment.assignmentStatus = :status
              and assignment.visitOccurrence.status <> com.homehealthcare.scheduling.foundation.SchedulingVisitStatus.CANCELLED
              and (:excludeVisitOccurrenceId is null or assignment.visitOccurrence.id <> :excludeVisitOccurrenceId)
              and assignment.visitOccurrence.plannedStartAt < :plannedEndAt
              and assignment.visitOccurrence.plannedEndAt > :plannedStartAt
            """)
    boolean existsActiveOverlap(
            @Param("caregiverProfileId") UUID caregiverProfileId,
            @Param("plannedStartAt") OffsetDateTime plannedStartAt,
            @Param("plannedEndAt") OffsetDateTime plannedEndAt,
            @Param("excludeVisitOccurrenceId") UUID excludeVisitOccurrenceId,
            @Param("status") CaregiverAssignmentStatus status);
}
