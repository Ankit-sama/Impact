package com.impact.admin;

import com.impact.config.AppClock;
import com.impact.scheduler.LifecycleService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/demo")
@Profile("dev")
public class DemoController {

    private final DemoDataSeeder seeder;
    private final AppClock appClock;
    private final LifecycleService lifecycleService;

    public DemoController(DemoDataSeeder seeder, AppClock appClock, LifecycleService lifecycleService) {
        this.seeder = seeder;
        this.appClock = appClock;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/reset")
    public String resetAndSeed() {
        seeder.resetAndSeed();
        return "redirect:/login?logout=true"; // Force logout to ensure clean state
    }

    @PostMapping("/tick")
    public String runTick() {
        lifecycleService.tick();
        return "redirect:/"; // Will bounce back to appropriate dashboard
    }

    @PostMapping("/skip")
    public String skipTime(@RequestParam long minutes) {
        appClock.addMinutes(minutes);
        return "redirect:/"; // Will bounce back to appropriate dashboard
    }
}