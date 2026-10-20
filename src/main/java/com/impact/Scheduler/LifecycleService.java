package com.impact.scheduler;

import com.impact.config.AppClock;
import com.impact.config.ImpactProperties;
import com.impact.event.*;
import com.impact.notification.NotificationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LifecycleService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final EventService eventService;
    private final AppClock appClock;
    private final ImpactProperties properties;

    public LifecycleService(EventRepository eventRepository, RegistrationRepository registrationRepository,
                            NotificationService notificationService, @Lazy EventService eventService,
                            AppClock appClock, ImpactProperties properties) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.eventService = eventService;
        this.appClock = appClock;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${impact.scheduler.interval-seconds}000")
    @Transactional // FIX: Start the transaction at the entry point of the scheduled job
    public void runScheduledTick() {
        tick();
    }

    @Transactional
    public int tick() {
        Instant now = appClock.now();
        Instant readinessTime = now.plus(properties.getEvent().getReadinessWindowMinutes(), ChronoUnit.MINUTES);
        
        List<Long> candidateIds = eventRepository.findIdsNeedingTick(now, readinessTime);
        int updatedCount = 0;
        
        for (Long id : candidateIds) {
            Event lockedEvent = lockAndTick(id);
            if (lockedEvent != null) {
                updatedCount++;
            }
        }
        return updatedCount;
    }

    @Transactional
    public Event lockAndTick(Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId).orElse(null);
        if (event == null) return null;

        boolean changed = false;
        Instant now = appClock.now();

        if (event.getStatus() == EventStatus.DRAFT && !now.isBefore(event.getStartAt())) {
            eventService.cancelEvent(event, CancelledBy.SYSTEM, "Start time passed while still a draft");
            return event;
        }

        if (event.getStatus() == EventStatus.PUBLISHED) {
            Instant readinessCutoff = event.getStartAt().minus(properties.getEvent().getReadinessWindowMinutes(), ChronoUnit.MINUTES);
            
            if (!now.isBefore(readinessCutoff)) {
                long confirmed = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
                
                if (confirmed == 0) {
                    eventService.cancelEvent(event, CancelledBy.SYSTEM, "No volunteers registered");
                    return event;
                }
                
                if (!now.isBefore(event.getStartAt())) {
                    event.setStatus(EventStatus.IN_PROGRESS);
                    event.setNeedsVolunteers(false);
                    changed = true;
                    
                    notificationService.createNotification(event.getCreatorId(), "'" + event.getTitle() + "' has started.", "/events/" + event.getId());
                    
                    List<Registration> activeRegs = registrationRepository.findByEventIdAndStatusIn(event.getId(), List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED));
                    for (Registration reg : activeRegs) {
                        notificationService.createNotification(reg.getUserId(), "'" + event.getTitle() + "' has started.", "/events/" + event.getId());
                    }
                } else {
                    long required = (event.getCapacity() * properties.getWorkforce().getThresholdPercent()) / 100;
                    boolean needsVols = confirmed < required;
                    
                    if (needsVols && !event.getNeedsVolunteers()) {
                        event.setNeedsVolunteers(true);
                        notificationService.createNotification(event.getCreatorId(),
                            "Only " + confirmed + " of " + event.getCapacity() + " seats are filled for '" + event.getTitle() + "'",
                            "/events/" + event.getId());
                        changed = true;
                    } else if (!needsVols && event.getNeedsVolunteers()) {
                        event.setNeedsVolunteers(false);
                        changed = true;
                    }
                }
            }
        }

        if (event.getStatus() == EventStatus.IN_PROGRESS && !now.isBefore(event.getEndAt())) {
            event.setStatus(EventStatus.AWAITING_PROOF);
            notificationService.createNotification(event.getCreatorId(),
                "Your event '" + event.getTitle() + "' has ended. Proof submission opens soon.",
                "/events/" + event.getId());
            changed = true;

            List<Registration> activeRegs = registrationRepository.findByEventIdAndStatusIn(event.getId(), List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED));
            for (Registration reg : activeRegs) {
                notificationService.createNotification(reg.getUserId(), "'" + event.getTitle() + "' has ended.", "/events/" + event.getId());
            }
        }
        
        if (changed) {
            eventRepository.save(event);
            return event;
        }
        
        return event; 
    }
}