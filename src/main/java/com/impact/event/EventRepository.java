package com.impact.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' " +
           "AND e.creatorId != :creatorId " +
           "AND e.category = :category " +
           "AND LOWER(TRIM(e.city)) = LOWER(TRIM(:city)) " +
           "AND e.startAt BETWEEN :windowStart AND :windowEnd")
    List<Event> findSimilarEvents(
            @Param("creatorId") Long creatorId,
            @Param("category") EventCategory category,
            @Param("city") String city,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    // NEW: Pessimistic write lock for capacity checks
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}