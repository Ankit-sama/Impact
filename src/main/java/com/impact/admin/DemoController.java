package com.impact.admin;

import com.impact.config.AppClock;
import com.impact.scheduler.LifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/demo")
@Profile("dev")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private final DemoDataSeeder seeder;
    private final AppClock appClock;
    private final LifecycleService lifecycleService;

    public DemoController(DemoDataSeeder seeder, AppClock appClock, LifecycleService lifecycleService) {
        this.seeder = seeder;
        this.appClock = appClock;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/reset")
    public String resetAndSeed(HttpServletRequest request) {
        try {
            request.getSession().invalidate();
            SecurityContextHolder.clearContext();
            
            seeder.resetAndSeed();
            
            return "redirect:/login?resetSuccess=true"; 
        } catch (Exception e) {
            logger.error("Demo reset failed", e);
            return "redirect:/login?resetError=true";
        }
    }

    @PostMapping("/tick")
    public String runTick(HttpServletRequest request, RedirectAttributes ra) {
        int updatedCount = lifecycleService.tick();
        ra.addFlashAttribute("successMsg", "Tick ran: " + updatedCount + " events updated.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/skip")
    public String skipTime(@RequestParam long minutes, HttpServletRequest request, RedirectAttributes ra) {
        appClock.addMinutes(minutes);
        int updatedCount = lifecycleService.tick();
        ra.addFlashAttribute("successMsg", "Skipped " + minutes + " min. Tick ran: " + updatedCount + " events updated.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/reset-clock")
    public String resetClock(HttpServletRequest request, RedirectAttributes ra) {
        appClock.reset();
        ra.addFlashAttribute("successMsg", "Clock offset reset to zero.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}