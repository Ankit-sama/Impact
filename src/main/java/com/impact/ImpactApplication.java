package com.impact;

import com.impact.config.ImpactProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ImpactProperties.class)
public class ImpactApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImpactApplication.class, args);
    }
}