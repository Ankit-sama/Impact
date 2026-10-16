package com.impact.user;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.event.EventStatus;
import com.impact.event.RegistrationService;
import com.impact.event.RegistrationStatus;
import com.impact.event.dto.RegistrationDetailsDto;
import com.impact.organization.OrganizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final OrganizationService organizationService;
    private final RegistrationService registrationService;
    private final AppClock appClock;

    public DashboardController(OrganizationService organizationService, RegistrationService registrationService, AppClock appClock) {
        this.organizationService = organizationService;
        this.registrationService = registrationService;
        this.appClock = appClock;
    }

    @GetMapping("/volunteer/dashboard")
    public String volunteerDashboard(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        Instant now = appClock.now();

        List<RegistrationDetailsDto> upcoming = registrationService.getMyRegistrations(userId).stream()
                .filter(dto -> dto.getRegistration().getStatus() == RegistrationStatus.CONFIRMED 
                        && dto.getEvent().getStartAt().isAfter(now)
                        && dto.getEvent().getStatus() != EventStatus.CANCELLED)
                .sorted(Comparator.comparing(dto -> dto.getEvent().getStartAt()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("upcomingRegistrations", upcoming);
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