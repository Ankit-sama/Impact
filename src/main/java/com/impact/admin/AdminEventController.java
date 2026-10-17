package com.impact.admin;

import com.impact.auth.CustomUserDetails;
import com.impact.event.EventService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/{id}/cancel")
    public String cancelEventByAdmin(@PathVariable Long id, @RequestParam(required = false) String cancelReason, 
                                     Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            eventService.cancelByAdmin(id, userDetails.getUser().getId(), cancelReason);
            ra.addFlashAttribute("successMsg", "Event has been cancelled successfully by admin.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id;
    }
}