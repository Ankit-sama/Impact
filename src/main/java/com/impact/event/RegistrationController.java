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
        
        // Exclude Cancelled events from Upcoming
        List<RegistrationDetailsDto> upcoming = allRegistrations.stream()
                .filter(dto -> dto.getRegistration().getStatus() == RegistrationStatus.CONFIRMED 
                        && dto.getEvent().getStartAt().isAfter(now)
                        && dto.getEvent().getStatus() != EventStatus.CANCELLED)
                .sorted(Comparator.comparing(dto -> dto.getEvent().getStartAt()))
                .collect(Collectors.toList());
                
        List<RegistrationDetailsDto> pastOrWithdrawn = allRegistrations.stream()
                .filter(dto -> dto.getRegistration().getStatus() != RegistrationStatus.CONFIRMED 
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
}