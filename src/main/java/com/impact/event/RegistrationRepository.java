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

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // Efficiently counts confirmed registrations for a list of events to avoid N+1 queries
    @Query("SELECT r.eventId, COUNT(r.id) FROM Registration r WHERE r.eventId IN :eventIds AND r.status = 'CONFIRMED' GROUP BY r.eventId")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}