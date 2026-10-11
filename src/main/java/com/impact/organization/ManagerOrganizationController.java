package com.impact.organization;

import com.impact.auth.CustomUserDetails;
import com.impact.organization.dto.OrganizationDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/organization")
public class ManagerOrganizationController {

    private final OrganizationService organizationService;

    public ManagerOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/edit")
    public String showEditForm(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Organization org = organizationService.getOrganizationByManagerId(userDetails.getUser().getId());
        
        if (org.getStatus() == OrgStatus.VERIFIED || org.getStatus() == OrgStatus.PENDING_VERIFICATION) {
            return "redirect:/manager/dashboard";
        }

        OrganizationDto dto = new OrganizationDto();
        dto.setName(org.getName());
        dto.setCauses(org.getCauses());
        dto.setCity(org.getCity());
        dto.setDescription(org.getDescription());
        dto.setContactEmail(org.getContactEmail());
        dto.setRegistrationNumber(org.getRegistrationNumber());
        
        model.addAttribute("orgDto", dto);
        return "manager/organization-form";
    }

    @PostMapping("/edit")
    public String processEditForm(@Valid @ModelAttribute("orgDto") OrganizationDto dto,
                                  BindingResult result, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "manager/organization-form";
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            organizationService.updateOrganization(userDetails.getUser().getId(), dto);
            redirectAttributes.addFlashAttribute("successMsg", "Organization profile saved successfully.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/dashboard";
    }

    @PostMapping("/submit")
    public String submitForVerification(Authentication authentication, RedirectAttributes redirectAttributes) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            organizationService.submitForVerification(userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("successMsg", "Organization submitted for verification.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/manager/dashboard";
    }
}