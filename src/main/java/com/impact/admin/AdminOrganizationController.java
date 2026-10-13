package com.impact.admin;

import com.impact.auth.CustomUserDetails;
import com.impact.config.ImpactProperties;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/organization")
public class AdminOrganizationController {

    private final OrganizationService organizationService;
    private final ImpactProperties impactProperties;

    public AdminOrganizationController(OrganizationService organizationService, ImpactProperties impactProperties) {
        this.organizationService = organizationService;
        this.impactProperties = impactProperties;
    }

    @GetMapping("/{id}/review")
    public String showReviewPage(@PathVariable Long id, Model model) {
        Organization org = organizationService.getOrganizationById(id).orElseThrow();
        model.addAttribute("organization", org);
        model.addAttribute("maxRounds", impactProperties.getOrganization().getMaxCorrectionRounds());
        return "admin/organization-review";
    }

    @PostMapping("/{id}/review")
    public String processReview(@PathVariable Long id, @RequestParam OrgStatus decision, 
                                @RequestParam(required = false) String reason, 
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            organizationService.reviewOrganization(userDetails.getUser().getId(), id, decision, reason);
            redirectAttributes.addFlashAttribute("successMsg", "Organization review saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/organization/" + id + "/review";
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/{id}/reopen")
    public String processReopen(@PathVariable Long id, @RequestParam String reopenReason, 
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            organizationService.reopenOrganization(userDetails.getUser().getId(), id, reopenReason);
            redirectAttributes.addFlashAttribute("successMsg", "Organization reopened for correction successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}