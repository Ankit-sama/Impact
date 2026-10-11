package com.impact.auth;

import com.impact.auth.dto.RegisterRequest;
import com.impact.user.Role;
import com.impact.user.UserStatus;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    private String getRedirectForAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        if (userDetails.getUser().getStatus() == UserStatus.PENDING_VERIFICATION) {
            return "redirect:/otp";
        }
        Role role = userDetails.getUser().getRole();
        if (role == Role.ROLE_VOLUNTEER) return "redirect:/volunteer/dashboard";
        if (role == Role.ROLE_ORG_MANAGER) return "redirect:/manager/dashboard";
        if (role == Role.ROLE_ADMIN) return "redirect:/admin/dashboard";
        return "redirect:/";
    }

    @GetMapping("/")
    public String rootRedirect(Authentication auth) {
        String redirect = getRedirectForAuthenticatedUser(auth);
        return redirect != null ? redirect : "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm(Authentication auth) {
        String redirect = getRedirectForAuthenticatedUser(auth);
        if (redirect != null) return redirect;
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, Authentication auth) {
        String redirect = getRedirectForAuthenticatedUser(auth);
        if (redirect != null) return redirect;
        
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                               BindingResult bindingResult, Model model,
                               RedirectAttributes redirectAttributes, Authentication auth) {
        String redirect = getRedirectForAuthenticatedUser(auth);
        if (redirect != null) return redirect;
                               
        MultipartFile photo = request.getPhoto();
        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > 2 * 1024 * 1024) {
                bindingResult.rejectValue("photo", "error.photo", "Photo must be smaller than 2MB");
            }
            String contentType = photo.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                bindingResult.rejectValue("photo", "error.photo", "Only JPG and PNG images are allowed");
            }
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        try {
            authService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMsg", "Registration successful. Please log in to complete OTP verification.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "register";
        } catch (Exception e) {
            logger.error("Registration failed with unexpected error", e);
            model.addAttribute("errorMsg", "An unexpected error occurred. Please try again.");
            return "register";
        }
    }

    @GetMapping("/otp")
    public String showOtpForm(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String lastPhone = authService.getLastPhoneForUser(userDetails.getUser().getId());
        if (!model.containsAttribute("phone") && lastPhone != null) {
            model.addAttribute("phone", lastPhone);
        }
        return "otp";
    }

    @PostMapping("/otp/send")
    public String sendOtp(@RequestParam String phone, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (phone == null || !phone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("errorMsg", "Phone number must be exactly 10 digits.");
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/otp";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        try {
            String generatedOtp = authService.generateOtp(userDetails.getUser().getId(), phone);
            redirectAttributes.addFlashAttribute("simulatedOtp", generatedOtp);
            redirectAttributes.addFlashAttribute("successMsg", "OTP generated successfully.");
            redirectAttributes.addFlashAttribute("phone", phone);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            redirectAttributes.addFlashAttribute("phone", phone);
        }
        return "redirect:/otp";
    }

    @PostMapping("/otp/verify")
    public String verifyOtp(@RequestParam String code, Authentication authentication, RedirectAttributes redirectAttributes) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean isValid = authService.verifyOtp(userDetails.getUser().getId(), code);

        if (isValid) {
            SecurityContextHolder.clearContext();
            redirectAttributes.addFlashAttribute("successMsg", "Account verified successfully! Please log in.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid OTP code.");
            return "redirect:/otp";
        }
    }
}