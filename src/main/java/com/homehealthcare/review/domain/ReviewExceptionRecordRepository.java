package com.homehealthcare.review.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewExceptionRecordRepository extends JpaRepository<ReviewExceptionRecord, UUID> {

    List<ReviewExceptionRecord> findAllByWorkItem_IdOrderByDetectedAtAsc(UUID workItemId);
}
