package com.impact.config;

import com.impact.common.AppSetting;
import com.impact.common.AppSettingRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class AppClock {

    private final AppSettingRepository appSettingRepository;
    private static final String OFFSET_KEY = "CLOCK_OFFSET_MINUTES";
    private long cachedOffset = 0;

    public AppClock(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
        loadOffset();
    }

    public Instant now() {
        return Instant.now().plus(cachedOffset, ChronoUnit.MINUTES);
    }

    public void addMinutes(long minutes) {
        cachedOffset += minutes;
        appSettingRepository.save(new AppSetting(OFFSET_KEY, String.valueOf(cachedOffset)));
    }

    public void reset() {
        cachedOffset = 0;
        appSettingRepository.save(new AppSetting(OFFSET_KEY, "0"));
    }

    private void loadOffset() {
        Optional<AppSetting> setting = appSettingRepository.findById(OFFSET_KEY);
        if (setting.isPresent()) {
            try {
                cachedOffset = Long.parseLong(setting.get().getSettingValue());
            } catch (NumberFormatException e) {
                cachedOffset = 0;
            }
        }
    }
}