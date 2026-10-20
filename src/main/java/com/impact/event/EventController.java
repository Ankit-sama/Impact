package com.impact.event;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.event.dto.EventFormDto;
import com.impact.event.dto.PublishedEventEditDto;
import com.impact.organization.OrganizationService;
import com.impact.scheduler.LifecycleService;
import com.impact.user.Role;
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
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
        
        if (event.getStatus() != EventStatus.DRAFT) {
            return "redirect:/events/" + id;
        }

        model.addAttribute("eventDto", eventService.convertToFormDto(event));
        model.addAttribute("categories", EventCategory.values());
        return "event/form";
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

    @GetMapping("/events/{id}/edit-published")
    public String showEditPublishedForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            Event event = eventService.getEventForCreator(id, userDetails.getUser().getId());
            model.addAttribute("event", event);
            model.addAttribute("editDto", eventService.getPublishedEventForEdit(id, userDetails.getUser().getId()));
            return "event/edit-published";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/events/" + id;
        }
    }

    @PostMapping("/events/{id}/edit-published")
    public String processEditPublishedForm(@PathVariable Long id, @Valid @ModelAttribute("editDto") PublishedEventEditDto dto,
                                          BindingResult result, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (result.hasErrors()) {
            model.addAttribute("event", eventService.getEventForCreator(id, userDetails.getUser().getId()));
            return "event/edit-published";
        }
        try {
            eventService.updatePublishedEvent(id, userDetails.getUser().getId(), dto);
            ra.addFlashAttribute("successMsg", "Event updated successfully.");
            return "redirect:/events/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("event", eventService.getEventForCreator(id, userDetails.getUser().getId()));
            return "event/edit-published";
        }
    }

    @PostMapping("/events/{id}/publish")
    public String tryPublish(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        try {
            List<Event> similarEvents = eventService.findSimilarEvents(id, userId);
            if (!similarEvents.isEmpty()) {
                ra.addFlashAttribute("similarEvents", similarEvents);
                return "redirect:/events/" + id + "/publish/confirm";
            }
            
            eventService.publishEvent(id, userId);
            ra.addFlashAttribute("successMsg", "Event published successfully.");
            return "redirect:/events/" + id;
        } catch (IllegalStateException e) {
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

    @GetMapping("/events/{id}/cancel")
    public String showCancelForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        Role role = userDetails.getUser().getRole();
        
        try {
            Event event = eventService.getEventById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));
            boolean isCreator = event.getCreatorId().equals(userId);
            boolean isAdmin = role == Role.ROLE_ADMIN;

            if (!isCreator && !isAdmin) {
                throw new IllegalStateException("You do not have permission to cancel this event.");
            }
            if (event.getStatus() == EventStatus.CANCELLED) {
                 throw new IllegalStateException("Event is already cancelled.");
            }
            
            if (isCreator && !isAdmin) {
                if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Creators can only cancel Published or In Progress events.");
                }
                if (!event.getEndAt().isAfter(appClock.now())) {
                    throw new IllegalStateException("Creators cannot cancel an event after its end time.");
                }
            } else if (isAdmin) {
                if (event.getStatus() != EventStatus.PUBLISHED && event.getStatus() != EventStatus.IN_PROGRESS && event.getStatus() != EventStatus.AWAITING_PROOF) {
                    throw new IllegalStateException("Administrators cannot cancel this event at its current stage.");
                }
            }

            model.addAttribute("event", event);
            model.addAttribute("isAdmin", isAdmin);
            return "event/cancel";
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/events/" + id;
        }
    }

    @PostMapping("/events/{id}/cancel")
    public String processCancel(@PathVariable Long id, @RequestParam(required = false) String reason, Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.processUserCancellation(id, userDetails.getUser().getId(), userDetails.getUser().getRole(), reason);
            ra.addFlashAttribute("successMsg", "Event successfully cancelled.");
            return "redirect:/events/" + id;
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/events/" + id + "/cancel";
        }
    }
}