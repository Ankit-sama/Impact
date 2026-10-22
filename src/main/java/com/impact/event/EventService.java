package com.impact.event;

import com.impact.admin.ActivityLogService;
import com.impact.config.AppClock;
import com.impact.config.ImpactProperties;
import com.impact.event.dto.EventFormDto;
import com.impact.event.dto.PublishedEventEditDto;
import com.impact.notification.NotificationService;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationRepository;
import com.impact.scheduler.LifecycleService;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AppClock appClock;
    private final ImpactProperties properties;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final RegistrationRepository registrationRepository;
    private final LifecycleService lifecycleService;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public EventService(EventRepository eventRepository, OrganizationRepository organizationRepository,
                        UserRepository userRepository, AppClock appClock, ImpactProperties properties,
                        ActivityLogService activityLogService, NotificationService notificationService,
                        RegistrationRepository registrationRepository, @Lazy LifecycleService lifecycleService) {
        this.eventRepository = eventRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.appClock = appClock;
        this.properties = properties;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.registrationRepository = registrationRepository;
        this.lifecycleService = lifecycleService;
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

    public Event getPublicEventDetails(Long eventId, Long requestingUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        
        if (event.getStatus() == EventStatus.DRAFT) {
            if (requestingUserId == null || !event.getCreatorId().equals(requestingUserId)) {
                throw new IllegalStateException("This event has not been published yet.");
            }
        }
        return event;
    }

    public List<Event> searchDiscoverEvents(String keyword, EventCategory category, String city, String startFrom, String startTo) {
        Instant fromInstant = null;
        Instant toInstant = null;
        
        try {
            if (startFrom != null && !startFrom.isBlank()) {
                fromInstant = LocalDate.parse(startFrom).atStartOfDay(IST_ZONE).toInstant();
            }
            if (startTo != null && !startTo.isBlank()) {
                toInstant = LocalDate.parse(startTo).atTime(LocalTime.MAX).atZone(IST_ZONE).toInstant();
            }
        } catch (Exception e) { }

        List<EventStatus> visibleStatuses = List.of(EventStatus.PUBLISHED, EventStatus.IN_PROGRESS);

        Specification<Event> spec = Specification.where(EventSpecifications.isStatusIn(visibleStatuses))
                .and(EventSpecifications.hasKeyword(keyword))
                .and(EventSpecifications.hasCategory(category))
                .and(EventSpecifications.hasCity(city))
                .and(EventSpecifications.startsAfter(fromInstant))
                .and(EventSpecifications.startsBefore(toInstant));

        return eventRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startAt"));
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
        Event event = lifecycleService.lockAndTick(eventId);
        if (!event.getCreatorId().equals(creatorId)) {
            throw new IllegalStateException("You are not authorized to access this event.");
        }
        
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only Draft events can be edited completely.");
        }
        
        validateEventTiming(dto);
        mapDtoToEvent(dto, event);
        eventRepository.save(event);
    }

    @Transactional
    public void deleteDraftEvent(Long eventId, Long creatorId) {
        Event event = lifecycleService.lockAndTick(eventId);
        if (!event.getCreatorId().equals(creatorId)) {
            throw new IllegalStateException("You are not authorized to access this event.");
        }
        
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only Draft events can be deleted.");
        }
        
        eventRepository.delete(event);
        logger.info("Draft Event {} deleted successfully by user {}", eventId, creatorId);
    }

    public PublishedEventEditDto getPublishedEventForEdit(Long eventId, Long creatorId) {
        Event event = getEventForCreator(eventId, creatorId);
        if (event.getStatus() != EventStatus.PUBLISHED || !event.getStartAt().isAfter(appClock.now())) {
            throw new IllegalStateException("Only published events that haven't started can be edited.");
        }
        
        PublishedEventEditDto dto = new PublishedEventEditDto();
        dto.setPurpose(event.getPurpose());
        dto.setAddress(event.getAddress());
        dto.setCapacity(event.getCapacity());
        return dto;
    }

    @Transactional
    public void updatePublishedEvent(Long eventId, Long creatorId, PublishedEventEditDto dto) {
        try {
            Event event = lifecycleService.lockAndTick(eventId);
            if (!event.getCreatorId().equals(creatorId)) {
                throw new IllegalStateException("You are not authorized to access this event.");
            }
            if (event.getStatus() != EventStatus.PUBLISHED) {
                throw new IllegalStateException("Only published events can be edited with this form.");
            }
            if (!event.getStartAt().isAfter(appClock.now())) {
                throw new IllegalStateException("You cannot edit an event after it has started.");
            }
            if (dto.getCapacity() < event.getCapacity()) {
                throw new IllegalArgumentException("Capacity can only be increased, not lowered.");
            }

            event.setPurpose(dto.getPurpose());
            event.setAddress(dto.getAddress());
            event.setCapacity(dto.getCapacity());
            eventRepository.save(event);
            
            logger.info("Published Event {} edited successfully by user {}", eventId, creatorId);
        } catch (Exception e) {
            logger.error("Failed to update published event: " + eventId, e);
            throw e;
        }
    }

    @Transactional
    public void publishEvent(Long eventId, Long creatorId) {
        Event event = lifecycleService.lockAndTick(eventId);
        if (!event.getCreatorId().equals(creatorId)) {
            throw new IllegalStateException("You are not authorized to access this event.");
        }
        
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

    @Transactional
    public void processUserCancellation(Long eventId, Long userId, Role role, String reason) {
        try {
            Event event = lifecycleService.lockAndTick(eventId);
            if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.VERIFIED || 
                event.getStatus() == EventStatus.NOT_VERIFIED || event.getStatus() == EventStatus.COMPLETED) {
                throw new IllegalStateException("Event cannot be cancelled in its current state.");
            }

            boolean isCreator = event.getCreatorId().equals(userId);
            boolean isAdmin = role == Role.ROLE_ADMIN;

            if (!isCreator && !isAdmin) {
                throw new IllegalStateException("You do not have permission to cancel this event.");
            }

            CancelledBy cancelledBy;
            if (isAdmin) {
                // FIXED: Admins can no longer cancel IN_PROGRESS events
                if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.AWAITING_PROOF) {
                    throw new IllegalStateException("Administrators can only cancel Published or Awaiting Proof events.");
                }
                if (reason == null || reason.trim().isEmpty()) {
                    throw new IllegalArgumentException("Reason is required for administrative cancellation.");
                }
                cancelledBy = CancelledBy.ADMIN;
            } else {
                // FIXED: Creators can only cancel PUBLISHED events (before they start)
                if (event.getStatus() != EventStatus.PUBLISHED) {
                    throw new IllegalStateException("Creators can only cancel Published events.");
                }
                if (!event.getStartAt().isAfter(appClock.now())) {
                    throw new IllegalStateException("Creators cannot cancel an event after its start time.");
                }
                cancelledBy = CancelledBy.CREATOR;
            }

            cancelEvent(event, cancelledBy, reason);
            logger.info("Event {} cancelled successfully by user {} (Role: {})", eventId, userId, role);
            
        } catch (Exception e) {
            logger.error("Failed to process cancellation for event: " + eventId, e);
            throw e;
        }
    }

    @Transactional
    public void cancelEvent(Event event, CancelledBy cancelledBy, String reason) {
        if (event.getStatus() == EventStatus.CANCELLED) return;

        event.setStatus(EventStatus.CANCELLED);
        event.setCancelledBy(cancelledBy);
        event.setCancelReason(reason);
        event.setNeedsVolunteers(false);
        eventRepository.save(event);

        if (cancelledBy != CancelledBy.CREATOR) {
            notificationService.createNotification(event.getCreatorId(),
                    "Your event '" + event.getTitle() + "' was cancelled by " + cancelledBy.name() + ". Reason: " + (reason != null && !reason.isBlank() ? reason : "Not provided"),
                    "/events/" + event.getId());
        }

        String actor = "the system";
        if (cancelledBy == CancelledBy.CREATOR) actor = "the organizer";
        if (cancelledBy == CancelledBy.ADMIN) actor = "an administrator";

        List<Registration> activeRegs = registrationRepository.findByEventIdAndStatusIn(event.getId(), List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED));
        for (Registration reg : activeRegs) {
            reg.setHoursCredited(0); 
            registrationRepository.save(reg);
            
            notificationService.createNotification(reg.getUserId(),
                    "'" + event.getTitle() + "' was cancelled by " + actor + ". Reason: " + (reason != null && !reason.isBlank() ? reason : "Not provided"),
                    "/events/" + event.getId());
        }

        activityLogService.logAction(
                event.getCreatorId(),
                "EVENT_CANCELLED", "EVENT", event.getId(), "Cancelled by " + cancelledBy.name() + ". Reason: " + (reason != null ? reason : "None")
        );

        // TODO (Stage 6): Refund simulated contributions here
    }

    public long getCreatorCancelledEventsCount(Long organizationId) {
        return eventRepository.countByOrganizationIdAndStatusAndCancelledByAndPublishedAtIsNotNull(organizationId, EventStatus.CANCELLED, CancelledBy.CREATOR);
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

    public User getEventCreator(Long creatorId) {
        return userRepository.findById(creatorId).orElse(null);
    }

    private void validateEventTiming(EventFormDto dto) {
        Instant start = dto.getStartAtLocal().atZone(IST_ZONE).toInstant();
        Instant end = dto.getEndAtLocal().atZone(IST_ZONE).toInstant();

        if (!start.isAfter(appClock.now())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(IST_ZONE);
            throw new IllegalArgumentException("Start time must be after the current time " + formatter.format(appClock.now()) + ".");
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
        event.setCity(event.getCity());
        event.setAddress(dto.getAddress());
        event.setCapacity(dto.getCapacity());
    }
}