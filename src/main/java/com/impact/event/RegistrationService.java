package com.impact.event;

import com.impact.config.AppClock;
import com.impact.event.dto.ParticipantDto;
import com.impact.event.dto.RegistrationDetailsDto;
import com.impact.scheduler.LifecycleService;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AppClock appClock;
    private final LifecycleService lifecycleService;

    // Defines active registrants for capacity and notifications
    private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED);

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository,
                               UserRepository userRepository, AppClock appClock, @Lazy LifecycleService lifecycleService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.appClock = appClock;
        this.lifecycleService = lifecycleService;
    }

    @Transactional
    public void register(Long eventId, Long userId, Role role) {
        if (role == Role.ROLE_ADMIN) {
            throw new IllegalStateException("Admins cannot register for events.");
        }

        Event event = lifecycleService.lockAndTick(eventId);

        if (event.getCreatorId().equals(userId)) {
            throw new IllegalStateException("You cannot register for your own event.");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Registration is only open for Published events.");
        }
        if (!event.getStartAt().isAfter(appClock.now())) {
            throw new IllegalStateException("Registration is closed. The event has already started.");
        }

        long activeCount = registrationRepository.countByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
        
        Optional<Registration> existingOpt = registrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingOpt.isPresent()) {
            Registration existing = existingOpt.get();
            if (existing.getStatus() == RegistrationStatus.CONFIRMED || existing.getStatus() == RegistrationStatus.ATTENDED) {
                throw new IllegalStateException("You are already registered for this event.");
            }
            if (activeCount >= event.getCapacity()) {
                throw new IllegalStateException("This event is full.");
            }
            existing.setStatus(RegistrationStatus.CONFIRMED);
            existing.setRegisteredAt(appClock.now());
            registrationRepository.save(existing);
            return;
        }

        if (activeCount >= event.getCapacity()) {
            throw new IllegalStateException("This event is full.");
        }

        Registration reg = new Registration();
        reg.setEventId(eventId);
        reg.setUserId(userId);
        reg.setStatus(RegistrationStatus.CONFIRMED);
        reg.setRegisteredAt(appClock.now());

        try {
            registrationRepository.saveAndFlush(reg);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("You are already registered for this event.");
        }
    }

    @Transactional
    public void withdraw(Long eventId, Long userId) {
        Event event = lifecycleService.lockAndTick(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("You can only withdraw while the event is Published.");
        }
        if (!event.getStartAt().isAfter(appClock.now())) {
            throw new IllegalStateException("You cannot withdraw after the event has started.");
        }

        Registration reg = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("Registration not found."));

        if (reg.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed registrations can be withdrawn.");
        }

        reg.setStatus(RegistrationStatus.WITHDRAWN);
        registrationRepository.save(reg);
    }

    @Transactional
    public void saveAttendance(Long eventId, Long creatorId, List<Long> attendedUserIds) {
        Event event = lifecycleService.lockAndTick(eventId);
        
        if (!event.getCreatorId().equals(creatorId)) {
            throw new IllegalStateException("Only the creator can record attendance.");
        }
        if (event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.AWAITING_PROOF) {
            throw new IllegalStateException("Attendance can only be recorded when the event is In Progress or Awaiting Proof.");
        }
        
        // Stage 5 note: Check if proof package is submitted here to lock attendance.

        List<Registration> activeRegs = registrationRepository.findByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
        for (Registration reg : activeRegs) {
            if (attendedUserIds != null && attendedUserIds.contains(reg.getUserId())) {
                reg.setStatus(RegistrationStatus.ATTENDED);
                reg.setHoursCredited(event.getExpectedHours());
            } else {
                reg.setStatus(RegistrationStatus.CONFIRMED);
                reg.setHoursCredited(0);
            }
            registrationRepository.save(reg);
        }
    }

    public boolean isUserActive(Long eventId, Long userId) {
        if (userId == null) return false;
        return registrationRepository.findByEventIdAndUserId(eventId, userId)
                .map(r -> ACTIVE_STATUSES.contains(r.getStatus()))
                .orElse(false);
    }

    public int getSeatsLeft(Long eventId, int capacity) {
        long active = registrationRepository.countByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
        return (int) Math.max(0, capacity - active);
    }

    public Map<Long, Integer> getSeatsLeftMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Object[]> counts = registrationRepository.countActiveByEventIds(eventIds);
        
        Map<Long, Integer> seatsMap = new HashMap<>();
        for (Event e : events) {
            seatsMap.put(e.getId(), e.getCapacity());
        }
        
        for (Object[] row : counts) {
            Long eId = (Long) row[0];
            Long active = (Long) row[1];
            seatsMap.put(eId, (int) Math.max(0, seatsMap.get(eId) - active));
        }
        return seatsMap;
    }

    public List<RegistrationDetailsDto> getMyRegistrations(Long userId) {
        List<Registration> registrations = registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId);
        List<RegistrationDetailsDto> dtoList = new ArrayList<>();
        for (Registration reg : registrations) {
            eventRepository.findById(reg.getEventId()).ifPresent(event -> {
                dtoList.add(new RegistrationDetailsDto(reg, event));
            });
        }
        return dtoList;
    }

    public List<ParticipantDto> getParticipantsForEvent(Long eventId, Long requestingUserId, Role role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        
        if (role != Role.ROLE_ADMIN && !event.getCreatorId().equals(requestingUserId)) {
            throw new IllegalStateException("You are not authorized to view the participant list for this event.");
        }

        List<Registration> active = registrationRepository.findByEventIdAndStatusInOrderByRegisteredAtAsc(eventId, ACTIVE_STATUSES);
        List<ParticipantDto> participants = new ArrayList<>();
        
        for (Registration reg : active) {
            userRepository.findById(reg.getUserId()).ifPresent(user -> {
                participants.add(new ParticipantDto(reg, user));
            });
        }
        return participants;
    }
}