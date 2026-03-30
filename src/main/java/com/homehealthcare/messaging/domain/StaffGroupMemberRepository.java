package com.homehealthcare.messaging.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffGroupMemberRepository extends JpaRepository<StaffGroupMember, UUID> {

    boolean existsByStaffGroup_IdAndMembership_IdAndRemovedAtIsNull(UUID staffGroupId, UUID membershipId);

    List<StaffGroupMember> findAllByStaffGroup_IdAndRemovedAtIsNullOrderByAddedAtAsc(UUID staffGroupId);
}
