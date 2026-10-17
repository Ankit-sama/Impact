package com.impact.organization;

import com.impact.event.CancelledBy;
import com.impact.event.EventRepository;
import com.impact.event.EventStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicOrganizationController {

    private final OrganizationService organizationService;
    private final EventRepository eventRepository;

    public PublicOrganizationController(OrganizationService organizationService, EventRepository eventRepository) {
        this.organizationService = organizationService;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/organization/{id}")
    public String showPublicProfile(@PathVariable Long id, Model model) {
        Organization org = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        long cancelledCount = eventRepository.countByOrganizationIdAndStatusAndCancelledBy(org.getId(), EventStatus.CANCELLED, CancelledBy.CREATOR);
        
        model.addAttribute("organization", org);
        model.addAttribute("cancelledCount", cancelledCount);
        return "public/organization";
    }
}