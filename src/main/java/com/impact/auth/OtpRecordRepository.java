package com.impact.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}