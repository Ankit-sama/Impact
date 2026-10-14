package com.impact.event;

import com.impact.config.AppClock;
import com.impact.config.ImpactProperties;
import com.impact.event.dto.EventFormDto;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationRepository;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AppClock appClock;
    private final ImpactProperties properties;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public EventService(EventRepository eventRepository, OrganizationRepository organizationRepository,
                        UserRepository userRepository, AppClock appClock, ImpactProperties properties) {
        this.eventRepository = eventRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.appClock = appClock;
        this.properties = properties;
    }

    public List<Event> getMyEvents(Long creatorId) {
        return eventRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId);
    }

    public Optional<Event> getEventById(Long eventId) {
        return eventRepository.findById(eventId);
    }

    public Event getEventForCreator(Long eventId, Long creatorId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (!event.getCreatorId().equals(creatorId)) {
            throw new IllegalStateException("You are not authorized to access this event.");
        }
        return event;
    }

    public List<Event> searchDiscoverEvents(String keyword, EventCategory category, String city, String dateRange) {
        Instant startFrom = null;
        Instant startTo = null;
        
        // Very basic date range handling for Discover filters if provided later
        if (dateRange != null && dateRange.equals("upcoming7")) {
            startFrom = appClock.now();
            startTo = appClock.now().plus(7, ChronoUnit.DAYS);
        } else if (dateRange != null && dateRange.equals("upcoming30")) {
            startFrom = appClock.now();
            startTo = appClock.now().plus(30, ChronoUnit.DAYS);
        }

        List<EventStatus> visibleStatuses = List.of(EventStatus.PUBLISHED, EventStatus.IN_PROGRESS);
        return eventRepository.findDiscoverEvents(visibleStatuses, keyword, category, city, startFrom, startTo);
    }

    @Transactional
    public void createDraft(Long creatorId, Role role, EventFormDto dto) {
        validateEventTiming(dto);

        Event event = new Event();
        event.setCreatorId(creatorId);
        
        if (role == Role.ROLE_ORG_MANAGER) {
            Organization org = organizationRepository.findByManagerId(creatorId)
                    .orElseThrow(() -> new IllegalStateException("Manager has no organization"));
            event.setOrganizationId(org.getId());
        }

        mapDtoToEvent(dto, event);
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedAt(appClock.now());
        
        eventRepository.save(event);
    }

    @Transactional
    public void updateDraft(Long eventId, Long creatorId, EventFormDto dto) {
        Event event = getEventForCreator(eventId, creatorId);
        
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only Draft events can be edited completely.");
        }
        
        validateEventTiming(dto);
        mapDtoToEvent(dto, event);
        eventRepository.save(event);
    }

    @Transactional
    public void publishEvent(Long eventId, Long creatorId) {
        Event event = getEventForCreator(eventId, creatorId);
        
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only Draft events can be published.");
        }

        Instant readinessCutoff = appClock.now().plus(properties.getEvent().getReadinessWindowMinutes(), ChronoUnit.MINUTES);
        if (event.getStartAt().isBefore(readinessCutoff)) {
            throw new IllegalStateException("Event start time must be at least " + 
                    properties.getEvent().getReadinessWindowMinutes() + " minutes from now to allow for readiness checks.");
        }

        if (event.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(event.getOrganizationId()).orElseThrow();
            if (org.getStatus() != OrgStatus.VERIFIED) {
                throw new IllegalStateException("Your organization must be verified before you can publish events.");
            }
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(appClock.now());
        eventRepository.save(event);
    }

    public List<Event> findSimilarEvents(Long eventId, Long creatorId) {
        Event event = getEventForCreator(eventId, creatorId);
        Instant windowStart = event.getStartAt().minus(properties.getSimilarity().getWindowDays(), ChronoUnit.DAYS);
        Instant windowEnd = event.getStartAt().plus(properties.getSimilarity().getWindowDays(), ChronoUnit.DAYS);
        
        return eventRepository.findSimilarEvents(creatorId, event.getCategory(), event.getCity(), windowStart, windowEnd);
    }

    public EventFormDto convertToFormDto(Event event) {
        EventFormDto dto = new EventFormDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setCategory(event.getCategory());
        dto.setPurpose(event.getPurpose());
        dto.setStartAtLocal(LocalDateTime.ofInstant(event.getStartAt(), IST_ZONE));
        dto.setEndAtLocal(LocalDateTime.ofInstant(event.getEndAt(), IST_ZONE));
        dto.setExpectedHours(event.getExpectedHours());
        dto.setState(event.getState());
        dto.setCity(event.getCity());
        dto.setAddress(event.getAddress());
        dto.setCapacity(event.getCapacity());
        return dto;
    }

    private void validateEventTiming(EventFormDto dto) {
        Instant start = dto.getStartAtLocal().atZone(IST_ZONE).toInstant();
        Instant end = dto.getEndAtLocal().atZone(IST_ZONE).toInstant();

        if (!start.isAfter(appClock.now())) {
            throw new IllegalArgumentException("Start time must be in the future.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        long durationMinutes = ChronoUnit.MINUTES.between(start, end);
        long minMinutes = properties.getEvent().getMinDurationMinutes();
        long maxMinutes = properties.getEvent().getMaxDurationDays() * 24L * 60L;

        if (durationMinutes < minMinutes) {
            throw new IllegalArgumentException("Event duration must be at least " + minMinutes + " minutes.");
        }
        if (durationMinutes > maxMinutes) {
            throw new IllegalArgumentException("Event duration must not exceed " + properties.getEvent().getMaxDurationDays() + " days.");
        }

        long maxExpectedHours = durationMinutes / 60;
        if (dto.getExpectedHours() > maxExpectedHours) {
            throw new IllegalArgumentException("Expected hours cannot exceed the total duration in hours.");
        }
    }

    private void mapDtoToEvent(EventFormDto dto, Event event) {
        event.setTitle(dto.getTitle());
        event.setCategory(dto.getCategory());
        event.setPurpose(dto.getPurpose());
        event.setStartAt(dto.getStartAtLocal().atZone(IST_ZONE).toInstant());
        event.setEndAt(dto.getEndAtLocal().atZone(IST_ZONE).toInstant());
        event.setExpectedHours(dto.getExpectedHours());
        event.setState(dto.getState());
        event.setCity(dto.getCity());
        event.setAddress(dto.getAddress());
        event.setCapacity(dto.getCapacity());
    }

    // Helper for Details page
    public User getEventCreator(Long creatorId) {
        return userRepository.findById(creatorId).orElse(null);
    }
}