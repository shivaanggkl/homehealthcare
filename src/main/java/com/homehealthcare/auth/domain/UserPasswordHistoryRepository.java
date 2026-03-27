package com.homehealthcare.auth.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, UUID> {

    List<UserPasswordHistory> findByUser_IdOrderByRecordedAtDesc(UUID userId, Pageable pageable);
}
