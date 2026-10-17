package com.impact.event;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.event.dto.EventFormDto;
import com.impact.event.dto.EventPublishedEditDto;
import com.impact.organization.OrganizationService;
import com.impact.scheduler.LifecycleService;
import com.impact.user.User;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class EventController {

    private final EventService eventService;
    private final OrganizationService organizationService;
    private final RegistrationService registrationService;
    private final LifecycleService lifecycleService;
    private final AppClock appClock;

    public EventController(EventService eventService, OrganizationService organizationService, 
                           RegistrationService registrationService, LifecycleService lifecycleService, AppClock appClock) {
        this.eventService = eventService;
        this.organizationService = organizationService;
        this.registrationService = registrationService;
        this.lifecycleService = lifecycleService;
        this.appClock = appClock;
    }

    @GetMapping("/discover")
    public String discoverEvents(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) EventCategory category,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(required = false) String startFrom,
                                 @RequestParam(required = false) String startTo,
                                 Model model) {
        List<Event> events = eventService.searchDiscoverEvents(keyword, category, city, startFrom, startTo);
        Map<Long, Integer> seatsLeftMap = registrationService.getSeatsLeftMap(events);
        
        model.addAttribute("events", events);
        model.addAttribute("seatsLeftMap", seatsLeftMap);
        model.addAttribute("categories", EventCategory.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("city", city);
        model.addAttribute("startFrom", startFrom);
        model.addAttribute("startTo", startTo);
        
        return "event/discover";
    }

    @GetMapping("/events/{id}")
    public String viewEventDetails(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        Long userId = null;
        boolean isConfirmed = false;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            userId = userDetails.getUser().getId();
            isConfirmed = registrationService.isUserActive(id, userId);
        }

        try {
            lifecycleService.lockAndTick(id);
            Event event = eventService.getPublicEventDetails(id, userId);
            User creator = eventService.getEventCreator(event.getCreatorId());
            int seatsLeft = registrationService.getSeatsLeft(id, event.getCapacity());
            
            model.addAttribute("event", event);
            model.addAttribute("creator", creator);
            model.addAttribute("seatsLeft", seatsLeft);
            model.addAttribute("isConfirmed", isConfirmed);
            model.addAttribute("now", appClock.now());
            
            if (event.getOrganizationId() != null) {
                organizationService.getOrganizationById(event.getOrganizationId())
                        .ifPresent(org -> model.addAttribute("organization", org));
            }
            
            return "event/details";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/discover";
        }
    }

    @GetMapping("/my-events")
    public String myEvents(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("events", eventService.getMyEvents(userDetails.getUser().getId()));
        return "event/my-events";
    }

    @GetMapping("/events/create")
    public String showCreateForm(Model model) {
        model.addAttribute("eventDto", new EventFormDto());
        model.addAttribute("categories", EventCategory.values());
        return "event/form";
    }

    @PostMapping("/events/create")
    public String processCreateForm(@Valid @ModelAttribute("eventDto") EventFormDto dto,
                                    BindingResult result, Model model,
                                    Authentication authentication, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", EventCategory.values());
            return "event/form";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.createDraft(userDetails.getUser().getId(), userDetails.getUser().getRole(), dto);
            ra.addFlashAttribute("successMsg", "Draft event created successfully.");
            return "redirect:/my-events";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("categories", EventCategory.values());
            return "event/form";
        }
    }

    @GetMapping("/events/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
            if (event.getStatus() != EventStatus.DRAFT) {
                ra.addFlashAttribute("errorMsg", "Only Draft events can be edited completely.");
                return "redirect:/events/" + id;
            }
            model.addAttribute("eventDto", eventService.convertToFormDto(event));
            model.addAttribute("categories", EventCategory.values());
            return "event/form";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/my-events";
        }
    }

    @PostMapping("/events/{id}/edit")
    public String processEditForm(@PathVariable Long id, @Valid @ModelAttribute("eventDto") EventFormDto dto,
                                  BindingResult result, Model model,
                                  Authentication authentication, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", EventCategory.values());
            return "event/form";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.updateDraft(id, userDetails.getUser().getId(), dto);
            ra.addFlashAttribute("successMsg", "Event updated successfully.");
            return "redirect:/my-events";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("categories", EventCategory.values());
            return "event/form";
        }
    }

    // --- NEW: Edit Published Event ---
    @GetMapping("/events/{id}/edit-published")
    public String showEditPublishedForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
            if (event.getStatus() != EventStatus.PUBLISHED) {
                ra.addFlashAttribute("errorMsg", "This edit form is only for Published events.");
                return "redirect:/events/" + id;
            }
            if (!appClock.now().isBefore(event.getStartAt())) {
                ra.addFlashAttribute("errorMsg", "Cannot edit a published event after it has started.");
                return "redirect:/events/" + id;
            }
            model.addAttribute("editDto", eventService.convertToPublishedEditDto(event));
            model.addAttribute("event", event);
            return "event/edit-published";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/my-events";
        }
    }

    @PostMapping("/events/{id}/edit-published")
    public String processEditPublishedForm(@PathVariable Long id, @Valid @ModelAttribute("editDto") EventPublishedEditDto dto,
                                           BindingResult result, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (result.hasErrors()) {
            try {
                model.addAttribute("event", eventService.getEventForCreator(id, userDetails.getUser().getId()));
            } catch (Exception ignored) {}
            return "event/edit-published";
        }
        try {
            eventService.updatePublishedEvent(id, userDetails.getUser().getId(), dto);
            ra.addFlashAttribute("successMsg", "Event updated successfully.");
            return "redirect:/events/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            try {
                model.addAttribute("event", eventService.getEventForCreator(id, userDetails.getUser().getId()));
            } catch (Exception ignored) {}
            return "event/edit-published";
        }
    }

    @PostMapping("/events/{id}/publish")
    public String tryPublish(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            List<Event> similarEvents = eventService.findSimilarEvents(id, userDetails.getUser().getId());
            if (!similarEvents.isEmpty()) {
                ra.addFlashAttribute("similarEvents", similarEvents);
                return "redirect:/events/" + id + "/publish/confirm";
            }
            eventService.publishEvent(id, userDetails.getUser().getId());
            ra.addFlashAttribute("successMsg", "Event published successfully.");
            return "redirect:/events/" + id;
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/my-events";
        }
    }

    @GetMapping("/events/{id}/publish/confirm")
    public String showSimilarityConfirmation(@PathVariable Long id, Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
        if (!model.containsAttribute("similarEvents")) {
            return "redirect:/my-events";
        }
        model.addAttribute("event", event);
        return "event/similarity-confirm";
    }

    @PostMapping("/events/{id}/publish/force")
    public String forcePublish(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.publishEvent(id, userDetails.getUser().getId());
            ra.addFlashAttribute("successMsg", "Event published successfully.");
            return "redirect:/events/" + id;
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/my-events";
        }
    }

    // --- NEW: Cancel by Creator ---
    @PostMapping("/events/{id}/cancel")
    public String cancelEventCreator(@PathVariable Long id, @RequestParam(required = false) String cancelReason, 
                                     Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.cancelByCreator(id, userDetails.getUser().getId(), cancelReason);
            ra.addFlashAttribute("successMsg", "Event cancelled successfully.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id;
    }
}