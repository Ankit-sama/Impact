package com.impact.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);
    
    List<Registration> findByUserIdOrderByRegisteredAtDesc(Long userId);
    
    List<Registration> findByEventIdAndStatusOrderByRegisteredAtAsc(Long eventId, RegistrationStatus status);

    List<Registration> findByEventIdAndStatusInOrderByRegisteredAtAsc(Long eventId, List<RegistrationStatus> statuses);
    
    List<Registration> findByEventIdAndStatusIn(Long eventId, List<RegistrationStatus> statuses);

    // RESTORED: LifecycleService needs this to check for zero registrations during readiness window
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.eventId = :eventId AND r.status IN :statuses")
    long countByEventIdAndStatusIn(@Param("eventId") Long eventId, @Param("statuses") List<RegistrationStatus> statuses);

    @Query("SELECT r.eventId, COUNT(r.id) FROM Registration r WHERE r.eventId IN :eventIds AND r.status IN ('CONFIRMED', 'ATTENDED') GROUP BY r.eventId")
    List<Object[]> countActiveByEventIds(@Param("eventIds") List<Long> eventIds);
}