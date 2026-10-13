package com.impact.user;

import com.impact.auth.CustomUserDetails;
import com.impact.organization.OrganizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final OrganizationService organizationService;

    public DashboardController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/volunteer/dashboard")
    public String volunteerDashboard() {
        return "volunteer/dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("organization", organizationService.getOrganizationByManagerId(userDetails.getUser().getId()));
        return "manager/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("pendingOrgs", organizationService.getPendingOrganizations());
        model.addAttribute("rejectedOrgs", organizationService.getRejectedOrganizations());
        return "admin/dashboard";
    }
}