package com.impact.organization;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicOrganizationController {

    private final OrganizationService organizationService;

    public PublicOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organization/{id}")
    public String showPublicProfile(@PathVariable Long id, Model model) {
        Organization org = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        model.addAttribute("organization", org);
        return "public/organization";
    }
}