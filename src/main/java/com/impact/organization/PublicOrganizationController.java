package com.impact.organization;

import com.impact.event.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicOrganizationController {

    private final OrganizationService organizationService;
    private final EventService eventService;

    public PublicOrganizationController(OrganizationService organizationService, EventService eventService) {
        this.organizationService = organizationService;
        this.eventService = eventService;
    }

    @GetMapping("/organization/{id}")
    public String showPublicProfile(@PathVariable Long id, Model model) {
        Organization org = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        model.addAttribute("organization", org);
        
        long cancelledCount = eventService.getCreatorCancelledEventsCount(org.getId());
        model.addAttribute("cancelledCount", cancelledCount);
        
        return "public/organization";
    }
}