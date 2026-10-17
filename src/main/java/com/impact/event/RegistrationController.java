package com.impact.event;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.event.dto.ParticipantDto;
import com.impact.event.dto.RegistrationDetailsDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EventService eventService;
    private final AppClock appClock;

    public RegistrationController(RegistrationService registrationService, EventService eventService, AppClock appClock) {
        this.registrationService = registrationService;
        this.eventService = eventService;
        this.appClock = appClock;
    }

    @PostMapping("/events/{id}/register")
    public String registerForEvent(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            registrationService.register(id, userDetails.getUser().getId(), userDetails.getUser().getRole());
            ra.addFlashAttribute("successMsg", "Successfully registered for the event!");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id;
    }

    @PostMapping("/events/{id}/withdraw")
    public String withdrawFromEvent(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            registrationService.withdraw(id, userDetails.getUser().getId());
            ra.addFlashAttribute("successMsg", "Successfully withdrawn from the event.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id;
    }

    @GetMapping("/my-registrations")
    public String viewMyRegistrations(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<RegistrationDetailsDto> allRegistrations = registrationService.getMyRegistrations(userDetails.getUser().getId());
        Instant now = appClock.now();
        
        List<RegistrationDetailsDto> upcoming = allRegistrations.stream()
                .filter(dto -> (dto.getRegistration().getStatus() == RegistrationStatus.CONFIRMED || dto.getRegistration().getStatus() == RegistrationStatus.ATTENDED) 
                        && dto.getEvent().getStartAt().isAfter(now)
                        && dto.getEvent().getStatus() != EventStatus.CANCELLED)
                .sorted(Comparator.comparing(dto -> dto.getEvent().getStartAt()))
                .collect(Collectors.toList());
                
        List<RegistrationDetailsDto> pastOrWithdrawn = allRegistrations.stream()
                .filter(dto -> (dto.getRegistration().getStatus() != RegistrationStatus.CONFIRMED && dto.getRegistration().getStatus() != RegistrationStatus.ATTENDED) 
                        || !dto.getEvent().getStartAt().isAfter(now)
                        || dto.getEvent().getStatus() == EventStatus.CANCELLED)
                .collect(Collectors.toList());

        model.addAttribute("upcomingRegistrations", upcoming);
        model.addAttribute("pastRegistrations", pastOrWithdrawn);
        return "registration/my-registrations";
    }

    @GetMapping("/events/{id}/participants")
    public String viewParticipants(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            List<ParticipantDto> participants = registrationService.getParticipantsForEvent(id, userDetails.getUser().getId(), userDetails.getUser().getRole());
            Event event = eventService.getEventById(id).orElseThrow();
            model.addAttribute("event", event);
            model.addAttribute("participants", participants);
            return "registration/participant-list";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/discover";
        }
    }

    // --- NEW: Attendance Endpoints ---
    @GetMapping("/events/{id}/attendance")
    public String viewAttendance(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
            if (event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.AWAITING_PROOF) {
                ra.addFlashAttribute("errorMsg", "Attendance can only be recorded when the event is In Progress or Awaiting Proof.");
                return "redirect:/events/" + id;
            }
            
            // Reusing getParticipantsForEvent because it fetches active (CONFIRMED/ATTENDED) registrants securely
            List<ParticipantDto> activeParticipants = registrationService.getParticipantsForEvent(id, userDetails.getUser().getId(), userDetails.getUser().getRole());
            
            int attendedCount = (int) activeParticipants.stream()
                    .filter(p -> p.getRegistration().getStatus() == RegistrationStatus.ATTENDED).count();

            model.addAttribute("event", event);
            model.addAttribute("participants", activeParticipants);
            model.addAttribute("attendedCount", attendedCount);
            
            return "registration/attendance";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/my-events";
        }
    }

    @PostMapping("/events/{id}/attendance")
    public String saveAttendance(@PathVariable Long id, @RequestParam(required = false) List<Long> attendedUserIds, 
                                 Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            registrationService.saveAttendance(id, userDetails.getUser().getId(), attendedUserIds);
            ra.addFlashAttribute("successMsg", "Attendance saved successfully.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id + "/attendance";
    }
}