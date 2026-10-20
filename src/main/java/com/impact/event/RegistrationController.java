package com.impact.event;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.event.dto.ParticipantDto;
import com.impact.event.dto.RegistrationDetailsDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/events/{id}/attendance")
    public String showAttendance(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            List<ParticipantDto> participants = registrationService.getParticipantsForEvent(id, userDetails.getUser().getId(), userDetails.getUser().getRole());
            Event event = eventService.getEventById(id).orElseThrow();
            
            if (!event.getCreatorId().equals(userDetails.getUser().getId())) {
                throw new IllegalStateException("Only the event creator can record attendance.");
            }
            if (event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.AWAITING_PROOF) {
                throw new IllegalStateException("Attendance can only be recorded when the event is In Progress or Awaiting Proof.");
            }
            
            model.addAttribute("event", event);
            model.addAttribute("participants", participants);
            
            long attendedCount = participants.stream().filter(p -> p.getRegistration().getStatus() == RegistrationStatus.ATTENDED).count();
            model.addAttribute("attendedCount", attendedCount);
            
            return "registration/attendance";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/events/" + id;
        }
    }

    @PostMapping("/events/{id}/attendance")
    public String saveAttendance(@PathVariable Long id, 
                                 @RequestParam(value = "attendedIds", required = false, defaultValue = "") List<Long> attendedIds, 
                                 Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            registrationService.saveAttendance(id, userDetails.getUser().getId(), attendedIds);
            
            int present = attendedIds.size();
            int total = registrationService.getParticipantsForEvent(id, userDetails.getUser().getId(), userDetails.getUser().getRole()).size();
            ra.addFlashAttribute("successMsg", "Attendance saved: " + present + " of " + total + " present.");
            return "redirect:/events/" + id;
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/events/" + id + "/attendance";
        }
    }
}